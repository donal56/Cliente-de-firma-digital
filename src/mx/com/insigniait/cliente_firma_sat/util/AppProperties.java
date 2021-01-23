package mx.com.insigniait.cliente_firma_sat.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {
	
	private static Boolean debug = null;
	
	public static String get(String code) {
		return getApplicationProperty(code);
	}
	
	public static Boolean getBoolean(String code) {
		return Boolean.valueOf(getApplicationProperty(code));
	}
	
	public static Integer getInteger(String code) {
		return Integer.parseInt(getApplicationProperty(code));
	}
	
	public static Boolean debug() {
		
		if(debug == null) {
			debug = getBoolean("debug");
		}
		
		return debug;
	}
	
	/*
	 * Recuperar propiedades de la aplicación
	 * Retorna @null si el código o el archivo de propiedades no existe
	 */
	private static String getApplicationProperty(String code) {
		InputStream is 		= 	Utils.class.getClassLoader().getResourceAsStream("app.properties");
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
	
}
