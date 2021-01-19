package mx.com.insigniait.cliente_firma_sat.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;

@SuppressWarnings("restriction")
public class Util {

	/*
	 * Recupera el stream de datos de una URL
	 */
	public static InputStream getOnlineFileStream(String url) throws IOException {
		return new URL(url).openStream();
	}
	
	/*
	 * Recupera y limpia los datos recuperados al protocolo de aplicación configurado
	 */
	public static Map<String, String> getProtocolQueryParams(String url) {
		
		Map<String, String> parametros 	= 	new HashMap<String, String>();
		String 				protocol 	= 	getApplicationProperty("protocol");
		
		if(protocol == null || !url.startsWith(protocol + ":?")) {
			return null;
		}
		
		String[] parts = url.replaceFirst(protocol + "\\:\\?", "").split("&");
		
		for (String part : parts) {
			String[]	pair 	= 	part.split("=");
			String		key		=	pair[0];
			String		value	=	pair[1];
			
			try {
				value = URLDecoder.decode(value, "UTF-8");
			} 
			catch (UnsupportedEncodingException e) {
				return null;
			}
			
			parametros.put(key, value);
		}

		return parametros;
	}
	
	/*
	 * Recupera los datos de una cadena con la forma: parametro=valor,parametro2=valor2,parametro3="valor3"
	 */
	public static Map<String, String> createMapFromCommaSeparatedString(String list) {
		
    	Map<String, String>	mapa = new HashMap<String, String>();
		
		if(list != null) {
			String[] partes = list.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
			
			for (String parte : partes) {
				Integer breakPoint = parte.indexOf("=");
				
				if(breakPoint != - 1) {
					String identificador 	= 	parte.substring(0, breakPoint);
					String valor 			= 	parte.substring(identificador.length() + 1, parte.length());
					
					identificador = identificador.trim();
					valor = valor.trim();
					
					if(valor.startsWith("\"") && valor.endsWith("\"")) {
						valor = valor.substring(1, valor.length() - 1);
					}
					
					mapa.put(identificador, valor);
				}
			}
		}
		
		return mapa;
	}
	
	/*
	 * Permite utilizar la tecla Enter como Click en un botón
	 */
	public static void setEnterAsClick(ButtonBase btn) {
		btn.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
            	btn.fire();
            }
        });
	}
	
	/*
	 * Recuperar propiedades de la aplicación
	 * Retorna @null si el código o el archivo de propiedades no existe
	 */
	public static String getApplicationProperty(String code) {
		InputStream is 		= 	Util.class.getClassLoader().getResourceAsStream("app.properties");
		Properties 	prop 	= 	new Properties();

		if (is != null) {
			try {
				prop.load(is);
				return prop.getProperty(code);
			} 
			catch (IOException e) {	}
		} 
		
		return null;
	}
	
	/*
	 * Encrypta una cadena
	 */
	public static String encrypt(String originalText, String passphrase, String salt) {
		TextEncryptor encryptor = Encryptors.text(passphrase, salt);
		return encryptor.encrypt(originalText);
	}
	
	/*
	 * Desencripta una cadena
	 */
	public static String decrypt(String encryptedText, String passphrase, String salt) {
		TextEncryptor decryptor = Encryptors.text(passphrase, salt);
		return decryptor.decrypt(encryptedText);
	}
}
