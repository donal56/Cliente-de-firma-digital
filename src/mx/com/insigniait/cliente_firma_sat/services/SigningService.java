package mx.com.insigniait.cliente_firma_sat.services;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Matrix;

import mx.com.insigniait.cliente_firma_sat.dto.SatSigner;
import mx.com.insigniait.cliente_firma_sat.util.Debug;
import mx.com.insigniait.cliente_firma_sat.util.Utils;

public class SigningService {

	/*
	 * Firma un PDF
	 */
	public File signPdf(File pdf, SatSigner signer) throws Exception {

		if (pdf == null || !FilenameUtils.getExtension(pdf.getPath()).equals("pdf")) {
			throw new Exception("Documento inválido. Solo PDFs aceptados.");
		}

		// Archivo final
		File signedPdf 	= 	File.createTempFile("cfd_signed_", "");

		// IO sobre el que se trabajara
		FileOutputStream 	fos 	= 	new FileOutputStream(signedPdf);
		PDDocument 			doc 	= 	PDDocument.load(pdf);

		// Atributos del certificado
		Map<String, String> signerAttributes = Utils.createMapFromCommaSeparatedString(signer.getCertificate().getSubjectDN().getName());
		Debug.info("Atributos del certificado cargados: " + signerAttributes.toString());

		String commonName 		= 	signerAttributes.get("CN");
		String email 			=	signerAttributes.get("EMAILADDRESS");
		String country 			= 	signerAttributes.get("C");

		// Elemento de firma del PDF
		PDSignature pdSignature = new PDSignature();
		pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
		pdSignature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
		pdSignature.setReason("Firma desde cliente de firma electrónica");
		pdSignature.setName((commonName != null ? commonName : "").toUpperCase());
		pdSignature.setContactInfo(email);
		pdSignature.setLocation((country != null ? country : "").toUpperCase());

		// Usado para atributo de firma válida
		pdSignature.setSignDate(Calendar.getInstance());

		// PDFBOX-3738 NeedAppearances == true results in visible signature becoming invisible with Adobe Reader
		String 		signatureFieldName 	= 	"FIRMA-" + pdSignature.getName();
		PDAcroForm 	acroForm 			= 	doc.getDocumentCatalog().getAcroForm();

		if (acroForm != null) {
			PDSignatureField existingSignature = findExistingSignature(acroForm, pdSignature.getName());
			
			if(existingSignature != null) {
				pdSignature = existingSignature.getSignature();
				Debug.error("Se encontro una firma existente para el mismo certificado.\n" 
						+ "La firma anterior anterior data de " + pdSignature.getSignDate().getTime().toString() + "\n"
						+ "Se procederan a actualizar la razón y fecha de firma.");

				pdSignature.setReason("Firma desde cliente de firma electrónica");
				pdSignature.setSignDate(Calendar.getInstance());
			}
			
			if(acroForm.getNeedAppearances()) {
				if (acroForm.getFields().isEmpty()) {
					acroForm.getCOSObject().removeItem(COSName.NEED_APPEARANCES);
					Debug.info("Se autoconfiguro el formulario de firma para ser visible en visores como Adobe Reader (Atributo NeedsAppearances)");
				} 
				else {
					Debug.error("La firma posiblemente resulte no sea visible en programas como Adobe Reader.");
				}
			}
		}
		
		//Calcular hash
		//TODO: hash usando la llave pública
		FileInputStream fis 	= 	new FileInputStream(pdf);
		String 			md5 	= 	DigestUtils.md5Hex(fis);
		Debug.info("Calculado MD5 de documento original: " + md5);
		
		// Opciones de firma visual
		SignatureOptions signatureOptions = new SignatureOptions();
		signatureOptions.setVisualSignature(createVisualSignatureTemplate(doc, pdSignature, signatureFieldName, md5));
		signatureOptions.setPage(doc.getNumberOfPages() - 1);

		Debug.info("Agregando firma...");
		doc.addSignature(pdSignature, signer, signatureOptions);

		// Si se guarda por el método 'save' común se podría romper el pdf
		Debug.info("Compilado PDF firmado...");
		doc.saveIncremental(fos);
		doc.close();

		return signedPdf;
	}

	/*
	 * Crea un documento plantilla con una firma incrustada y lo devuelve como stream
	 */
	private InputStream createVisualSignatureTemplate(PDDocument srcDoc, PDSignature pdSignature, String signatureFieldName, String digitalSignature) throws IOException {

		//Última página
		int 	pageNum 	=	srcDoc.getNumberOfPages() - 1;
		PDPage 	lastPage 	=	srcDoc.getPage(pageNum);
		
		Debug.info("Se colocara la firma en la página " + (pageNum + 1));
		
		//Especifica el espacio en donde se creara la firma visual, tomando en cuenta si ya existia una previa
		PDAcroForm 	acroFormSrc = 	srcDoc.getDocumentCatalog().getAcroForm();
		PDRectangle rect 		= 	null;
		
		//Calculando la posición de la firma visual
		List<PDSignatureField> signatureFields = findExistingSignatures(acroFormSrc);
		Float highestSignatureYAxis = 80f;
		
		if(acroFormSrc == null || signatureFields.isEmpty()) {
			Rectangle2D	humanRect =	new Rectangle2D.Float(40, lastPage.getMediaBox().getHeight() - highestSignatureYAxis, lastPage.getMediaBox().getWidth() - 80, 25);
			rect 	=	 createSignatureRectangle(lastPage, humanRect);
		}
		else {
			String signer = signatureFieldName.substring(6, signatureFieldName.length());
			
			PDSignatureField existingSignature = findExistingSignature(acroFormSrc, signer);
			
			if(existingSignature != null) {
				rect 	= 	existingSignature.getWidgets().get(0).getRectangle();
			}
			else {
				Debug.info("Se encontraron " + signatureFields.size() + " firma(s) anteriores en el documento");
				
				for (PDSignatureField signatureField : signatureFields) {
					if(!signatureField.getWidgets().isEmpty()) {
						PDRectangle rectWidget = signatureField.getWidgets().get(0).getRectangle();
						
						if(rectWidget.getUpperRightY() > highestSignatureYAxis) {
							highestSignatureYAxis = rectWidget.getUpperRightY();
						}
						
						Debug.info(">> " + signatureField.getSignature().getName());
					}
				}
				
				Rectangle2D	humanRect =	new Rectangle2D.Float(40, lastPage.getMediaBox().getHeight() - highestSignatureYAxis - 25, lastPage.getMediaBox().getWidth() - 80, 25);
				rect 	=	 createSignatureRectangle(lastPage, humanRect);
			}
		}
		
		Debug.info("Posición de la firma visual: " + rect);
		
		//Nuevo documento
		try (PDDocument doc = new PDDocument()) {
			
			PDPage page = new PDPage(lastPage.getMediaBox());
			doc.addPage(page);

			//Nuevo formulario
			PDAcroForm acroForm = new PDAcroForm(doc);
			doc.getDocumentCatalog().setAcroForm(acroForm);

			//Nuevo campo de firma
			PDSignatureField signatureField = new PDSignatureField(acroForm);
			signatureField.setReadOnly(true);
			signatureField.setPartialName(signatureFieldName);
			signatureField.setAlternateFieldName(signatureFieldName);
			
			//Agregando anotación
			PDAnnotationWidget 	widget = signatureField.getWidgets().get(0);
			widget.setRectangle(rect);
			widget.setAnnotationName(signatureFieldName);
			
			//Configurando formulario
			acroForm.setSignaturesExist(true);
			acroForm.setAppendOnly(true);
			acroForm.getCOSObject().setDirect(true);

			//Agregando campo
			List<PDField> acroFormFields = acroForm.getFields();
			acroFormFields.add(signatureField);

			//Preparativos para la configuración de estilo
			PDStream 		stream 	= 	new PDStream(doc);
			PDFormXObject 	form 	=	new PDFormXObject(stream);
			PDResources 	res 	= 	new PDResources();

			form.setResources(res);
			form.setFormType(1);
			
			PDAppearanceDictionary appearance = new PDAppearanceDictionary();
			appearance.getCOSObject().setDirect(true);

			PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
			appearance.setNormalAppearance(appearanceStream);

			widget.setAppearance(appearance);

			//Preparativos para el ajuste de posicion de la firma
			PDRectangle bbox 			= 	new PDRectangle(rect.getWidth(), rect.getHeight());
			Matrix 		initialScale 	= 	null;

			switch (srcDoc.getPage(pageNum).getRotation()) {
				case 90:
					form.setMatrix(AffineTransform.getQuadrantRotateInstance(1));
					initialScale = Matrix.getScaleInstance(bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
					break;
				case 180:
					form.setMatrix(AffineTransform.getQuadrantRotateInstance(2));
					break;
				case 270:
					form.setMatrix(AffineTransform.getQuadrantRotateInstance(3));
					initialScale = Matrix.getScaleInstance(bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
					break;
				case 0:
				default:
					break;
			}
			form.setBBox(bbox);

			// Firma en texto
			try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream)) {

				Debug.info("Generando firma visual...");
				
				if (initialScale != null) {
					cs.transform(initialScale);
				}
				
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				
				String text = 	"Firmado por " + pdSignature.getName() + " el " + sdf.format(pdSignature.getSignDate().getTime())
								+ " [" + digitalSignature + "]";

				cs.beginText();
				cs.setLeading(13.5f);
				cs.setFont(PDType1Font.COURIER_OBLIQUE, 8);
				cs.setNonStrokingColor(Color.black);
				cs.newLineAtOffset(0, 15);
				cs.showText(text);
				cs.endText();
				cs.close();
			}

			//Transformar a stream
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Debug.info("Compilando firma visual...");
			
			doc.save(baos);
			doc.close();
			
			return new ByteArrayInputStream(baos.toByteArray());
		}
	}

	/*
	 * Traduce coordenadas base arriba-derecha a abajo-derecha, tomando en cuenta la orientación de la página
	 */
	private PDRectangle createSignatureRectangle(PDPage page, Rectangle2D humanRect) {

		float x 		= 	(float) humanRect.getX();
		float y 		= 	(float) humanRect.getY();
		float width 	= 	(float) humanRect.getWidth();
		float height 	= 	(float) humanRect.getHeight();
		
		PDRectangle pageRect 	= 	page.getCropBox();
		PDRectangle rect 		= 	new PDRectangle();
		
		switch (page.getRotation()) {
			case 90:
				rect.setLowerLeftY(x);
				rect.setUpperRightY(x + width);
				rect.setLowerLeftX(y);
				rect.setUpperRightX(y + height);
				break;
			case 180:
				rect.setUpperRightX(pageRect.getWidth() - x);
				rect.setLowerLeftX(pageRect.getWidth() - x - width);
				rect.setLowerLeftY(y);
				rect.setUpperRightY(y + height);
				break;
			case 270:
				rect.setLowerLeftY(pageRect.getHeight() - x - width);
				rect.setUpperRightY(pageRect.getHeight() - x);
				rect.setLowerLeftX(pageRect.getWidth() - y - height);
				rect.setUpperRightX(pageRect.getWidth() - y);
				break;
			case 0:
			default:
				rect.setLowerLeftX(x);
				rect.setUpperRightX(x + width);
				rect.setLowerLeftY(pageRect.getHeight() - y - height);
				rect.setUpperRightY(pageRect.getHeight() - y);
				break;
		}
		
		return rect;
	}

	/*
	 * Busca cierta firmas existente
	 */
	private PDSignatureField findExistingSignature(PDAcroForm acroForm, String sigFieldName) throws IOException {
		List<PDSignatureField> 	signatureFields = findExistingSignatures(acroForm);
		
		PDSignatureField found = null;
		
		for (PDSignatureField signatureField : signatureFields) {
			if(signatureField.getSignature().getName().equals(sigFieldName)) {
				found = signatureField;
				break;
			}
		}
		
		return found;
	}
	
	/*
	 * Busca campos de firma existentes
	 */
	private List<PDSignatureField> findExistingSignatures(PDAcroForm acroForm) throws IOException {
		List<PDSignatureField> 	signatureFields = 	new ArrayList<PDSignatureField>();
		
		if (acroForm != null) {
			for (PDField field : acroForm.getFields()) {
				if(field instanceof PDSignatureField) {
					signatureFields.add((PDSignatureField) field);
				}
			}
		}
		return signatureFields;
	}

}
