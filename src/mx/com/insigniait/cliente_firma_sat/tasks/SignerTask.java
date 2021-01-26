package mx.com.insigniait.cliente_firma_sat.tasks;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javafx.concurrent.Task;
import mx.com.insigniait.cliente_firma_sat.dto.SatSigner;
import mx.com.insigniait.cliente_firma_sat.services.SigningService;
import mx.com.insigniait.cliente_firma_sat.services.TimestampAuthorityClient;
import mx.com.insigniait.cliente_firma_sat.util.AppProperties;
import mx.com.insigniait.cliente_firma_sat.util.CredentialStorage;
import mx.com.insigniait.cliente_firma_sat.util.Debug;
import mx.com.insigniait.cliente_firma_sat.util.GuiUtils;

@SuppressWarnings("restriction")
public class SignerTask extends Task<Void> {
	
	private Map<String, String> parametros = new HashMap<String, String>();
	
	public SignerTask(Map<String, String> parametros) {
		this.parametros = parametros;
		Debug.info(parametros.toString());
	}

	@Override
	protected Void call() throws Exception {
		
		String certificateAlias = parametros.get("alias");
		
		//Credenciales
		X509Certificate cert;
		PrivateKey pk;
		Certificate[] chain;

		try {
			cert 	= 	CredentialStorage.getCertificate(certificateAlias);
			Debug.info("Certificado recuperado");
			
			pk 		= 	CredentialStorage.getPrivateKey(certificateAlias);
			Debug.info("Llave privada recuperada");

			chain	=	CredentialStorage.getCertificateChain(certificateAlias);
			Debug.info("Cadena de certificados recuperada");

			//Documento a firmar
			Debug.info("Recuperando documento...");
			
			String 	docUrl 	= 	parametros.get("doc");
			File 	pdf 	= 	File.createTempFile("cfd_", ".pdf");
			Long	start	=	System.currentTimeMillis();

			FileUtils.copyInputStreamToFile(new URL(docUrl).openStream(), pdf);
			Long	end		=	System.currentTimeMillis();
			
			Debug.info("Documento recuperado de " + docUrl + " en " + (end - start) + "ms");

			//Firma
			SatSigner 	signature 		= 	new SatSigner(cert, pk, chain);
			signature.setTsaClient(new TimestampAuthorityClient(AppProperties.get("tsa_client")));

			File 		signedPdf 		=	new SigningService().signPdf(pdf, signature);

			//Autenticación de guardado
			String auth 		= 	parametros.get("username") + ":" + parametros.get("password");
			byte[] encodedAuth 	= 	Base64.getEncoder().encode(auth.getBytes("UTF-8"));

			//URL de guardado
			String  url = parametros.get("referer");
			url += url.endsWith("/") ? "" : "/";
			url += AppProperties.get("protocol") + "/save";

			//Creando solicitud
			HttpEntity data = MultipartEntityBuilder
					.create()
					.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
					.addBinaryBody("file", signedPdf, ContentType.DEFAULT_BINARY, signedPdf.getName())
					.addTextBody("docId", parametros.get("docId"))
					.addTextBody("signer", certificateAlias)
					.build();

			HttpUriRequest request = RequestBuilder.post()
					.setUri(url)
					.setEntity(data)
					.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth))
					.build();

			//Llamada ciclada en caso de error
			AtomicBoolean exito = new AtomicBoolean(false);

			do {
				//Solicitud
				int 			responseCode 	= 	404;
				HttpResponse 	response 		= 	null;

				if(!AppProperties.getBoolean("debug_pdf")) {
					Debug.info("Iniciado subida de documento firmado...");
					Long	start2	=	System.currentTimeMillis();
					
					response 		= 	HttpClients.custom().build().execute(request);
					responseCode 	= 	response.getStatusLine().getStatusCode();
					
					Long	end2	=	System.currentTimeMillis();
					
					Debug.info("Solicitud completada en " + (end2 - start2) + "ms\n" + 
							">>> " + responseCode + " " + response.getStatusLine().getReasonPhrase());
				}
				
				//En caso de exito guardar la respuesta en los argumentos de la aplicación
				if (responseCode >= 200 && responseCode < 300) {
					Debug.info("Documento subido\nRespuesta: " + EntityUtils.toString(response.getEntity()));
					exito.set(true);
				} 
				//En caso de fallo mostrar un dialogo de decisión: Reintentar o guardar localmente
				else {
					Debug.error("Error al subir. Esto puede ser causado por una configuración de prueba activa. Esperando decisión.");
					
					List<String> decisiones = Arrays.asList("Reintentar", "Guardar en mi escritorio");
					
					Optional<String> optionSelected = GuiUtils.decision("Error de conexión", "Ocurrio un error al subir el documento firmado: " 
							+ (response != null ? response.getStatusLine().getReasonPhrase() : "") + "\n", decisiones);
					
					switch (optionSelected.orElse("exit")) {
						case "Guardar en mi escritorio":
							String filePath = System.getProperty("user.home") + "/Desktop/" + signedPdf.getName() + ".pdf";
							
							Debug.info("Guardando localmente en " + filePath);
							
							try {
								FileUtils.moveFile(signedPdf, new File(filePath));
								exito.set(true);
							} 
							catch (IOException e) {
								GuiUtils.throwError("Error al guardar archivo", e);
							}
							
							break;
						case "exit":
							Debug.info("Operación cancelada...");
							System.exit(0);
							break;
						default:
							Debug.info("Reintentando...");
					}
				}
			}
			while(!exito.get());

			//Éxito
			GuiUtils.finalMessage("Firma exitosa");
		} 
		catch (Exception e1) {
			GuiUtils.throwError(e1.getMessage(), e1);
		}
		
		return null;
	}
	
}
