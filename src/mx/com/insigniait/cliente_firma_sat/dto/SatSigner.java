package mx.com.insigniait.cliente_firma_sat.dto;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPException;

import mx.com.insigniait.cliente_firma_sat.services.TimestampAuthorityClient;
import mx.com.insigniait.cliente_firma_sat.util.Debug;

/*
 * Implementación de firma para PDFBox
 * Además de algunos métodos utiles relacionados a las firmas y al SAT
 */
public class SatSigner implements SignatureInterface  {

	private X509Certificate 	certificate 		= 	null; 
	private PrivateKey 			privateKey 			= 	null;
	private Certificate[] 		certificateChain 	= 	null;
	private String 				algorithm 			= 	null;
	
	private TimestampAuthorityClient tsaClient 		=	null;
	
	public SatSigner(X509Certificate certificateParam, PrivateKey privateKeyParam, Certificate[] certificateChainParam) {
		certificate			= 	certificateParam;
		privateKey 			= 	privateKeyParam;
		certificateChain	= 	certificateChainParam;
		algorithm 			= 	"SHA256WithRSA";
		// "SHA1withRSA"
	}
	
	public void setCertificate(X509Certificate certificateParam) {
		certificate	= 	certificateParam;
	}
	
	public void setPrivateKey(PrivateKey privateKeyParam) {
		privateKey 	= 	privateKeyParam;
	}
	
	public void setCertificateChain(Certificate[] certificateChainParam) {
		certificateChain	= 	certificateChainParam;
	}
	
	public void setAlgorithm(String algorithmParam) {
		algorithm 	=	algorithmParam;
	}
	
	public X509Certificate getCertificate() {
		return certificate;
	}
	
	public PrivateKey getPrivateKey() {
		return privateKey;
	}
	
	public Certificate[] getCertificateChain() {
		return certificateChain;
	}
	
	public String setAlgorithm() {
		return algorithm;
	}
	
	public TimestampAuthorityClient getTsaClient() {
		return tsaClient;
	}

	public void setTsaClient(TimestampAuthorityClient tsaClient) {
		this.tsaClient = tsaClient;
	}
	
	/*
	 * Verifica firma de cadena de texto
	 */
	public void verifySignatureSimple(String plainMsg, String signatureBase64) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
        
		Signature signer = Signature.getInstance(algorithm);
        signer.initVerify(certificate);
        signer.update(plainMsg.getBytes("UTF8"));
        
        Boolean verified = signer.verify(Base64.getDecoder().decode(signatureBase64));
        
        if(verified) {
        	Debug.info("Verificación simple exitosa");
        }
        else {
        	Debug.error("La llave privada y el certificado no corresponden");
        	throw new IOException("Verificación simple fallada.");
        }
	}
	
	/*
	 * Verifica que la llave pública y privada coincidan
	 * Verifica que el certificado sea emitido por el SAT
	 * TODO: comprobar (Issuer)
	 */
	public void verifySatCertificate() throws Exception {
		
		verifyCertificateVigency();
		
		//Encriptamos un texto y lo desencriptamos para validar que las llaves sean correspondientes
	    String TEST_STRING 		= 	"Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
  		String signatureBase64 	= 	simpleSign(TEST_STRING);

  		verifySignatureSimple(TEST_STRING, signatureBase64);
		
		//Los certificados CSD solo permiten la firma digital
		int pathLen = certificate.getBasicConstraints();
		
		if(pathLen != -1) {
        	Debug.error("El certificado no es un CSD, posee el atributo de Autoridad Certificadora(CA): " + pathLen);
		    throw new Exception("El certificado no es una e-firma");
		}
			
		if(getSerial().length() != 20) {
			Debug.error("El numero de serie del certificado debe contener 20 caracteres: " + getSerial());
			throw new Exception("El certificado no es una e-firma");
		}

		Debug.info("El certificado es una e-firma");
	}
	
	/*
	 * Verifica la vigencia del certificado
	 */
	public void verifyCertificateVigency() throws Exception {
		try {
			certificate.checkValidity();
		}
		catch (CertificateExpiredException e) {
			Debug.error("El certificado expiró");
			throw new Exception(e);
		}
		catch (CertificateNotYetValidException e) {
			Debug.error("El certificado aun no entra en su periodo de validez");
			throw new Exception(e);
		}
	}
	
	/*
	 * Firma o sella digitalmente datos usando el algoritmo indicado con la llave privada proporcionada, retorna el resultado en Base64 
	 */
	public String simpleSign(byte[] bs) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException, SignatureException {
		
        Signature signer = Signature.getInstance(algorithm);
        signer.initSign(privateKey);
        signer.update(bs);
        
        byte[] sign = Base64.getEncoder().encode(signer.sign());
        
        return new String(sign);
	}
	
	/*
	 * Firma una cadena de texto
	 */
	public String simpleSign(String cadena) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException, SignatureException {
		return simpleSign(cadena.getBytes("UTF8"));
	}
	
	/*
	 * Implementación de firma usada por PDFBox
	 */
	@Override
	public byte[] sign(InputStream content) throws IOException {
		
		try {
			verifySatCertificate();
		} 
		catch (Exception e1) {
			e1.printStackTrace();
			throw new IOException("Error al firmar PDF: " + e1.getMessage());
		}
		
		CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

		if(certificate == null) {
			throw new IOException("Certificado no configurado.");
		}
		
		if(certificateChain == null) {
			throw new IOException("Cadena de certificados no configurado.");
		}
		
		if(privateKey == null) {
			throw new IOException("Llave privada no configurada.");
		}
		
		ContentSigner sha1Signer;
		
		try {
			sha1Signer = new JcaContentSignerBuilder(algorithm).build(privateKey);
			DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().build();
			SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider).build(sha1Signer, certificate);
			
			generator.addSignerInfoGenerator(signerInfoGenerator);
			generator.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
			
			CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
			CMSSignedData signedData = generator.generate(msg, false);

			if(tsaClient != null) {
				signedData = tsaClient.addSignedTimeStamp(signedData);
			}
			
			return signedData.getEncoded();
		} 
		catch (OperatorCreationException | CertificateEncodingException | CMSException | NoSuchAlgorithmException | IOException | TSPException e) {
			Debug.error("Error al firmar PDF: " + e.getMessage());
			
			e.printStackTrace();
			throw new IOException(e.getMessage(), e.getCause());
		}
	}
	
	/*
	 * 
	 */
	private String getSerial() {
	    String hex = certificate.getSerialNumber().toString(16);
	   
	    if(hex.length() % 2 != 0) {
	        return "";
	    }
	   
	    StringBuilder serial = new StringBuilder();
	    int pares = hex.length() / 2;
	    
	    for(int i = 0; i < pares; i++) {
	        serial.append((char) Integer.parseInt(hex.substring(2 * i, (2 * i) + 2), 16));
	    }
	   
	    return serial.toString();
	}
}
