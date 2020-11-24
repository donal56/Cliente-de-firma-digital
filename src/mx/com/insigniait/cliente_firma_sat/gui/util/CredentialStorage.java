package mx.com.insigniait.cliente_firma_sat.gui.util;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

public class CredentialStorage {

    private static KeyStore ks = null;
    
    private static void init() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    	if(ks == null) {
    		
        	if(!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
        		throw new IllegalStateException("La aplicación solo es ejecutable en Windows.");
        	}
    		
    		ks = KeyStore.getInstance("Windows-MY");
    		ks.load(null, null); 
    	}
    }
    
    public static Enumeration<String> getAliases() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    	init();
    	return ks.aliases();
    }
}
