package mx.com.insigniait.cliente_firma_sat.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/*
 * Acceso a credenciales instaladas el almacén de certificados personales de Windows
 */
public class CredentialStorage {

    private static KeyStore ks = null;
    
    /*
     * Inicializa el almacen de credenciales
     * Comprueba datos y el entorno de la aplicación
     */
    private static void init() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    	if(ks == null) {
    		
        	if(!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
        		GuiUtils.throwError("La aplicación solo es ejecutable en Windows.", "OS no soportado: " + System.getProperty("os.name"));
        	}
        	
    		ks = KeyStore.getInstance("Windows-MY");
    		ks.load(null, null); 
    	}
    }
    
    public static Enumeration<String> getAliases() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    	init();
    	return ks.aliases();
    }
    
	public static X509Certificate getCertificate(String alias) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		init();
		
		Certificate cert = ks.getCertificate(alias);
		
		CertificateFactory cf = CertificateFactory.getInstance("X509");
		X509Certificate certX509 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
		
		return certX509;
	}
	
	public static PrivateKey getPrivateKey(String alias) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException{
		init();
		return (PrivateKey) ks.getKey(alias, null);
	}
	
	public static Certificate[] getCertificateChain(String alias) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		init();
		return ks.getCertificateChain(alias);
	}
}
