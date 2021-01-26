package mx.com.insigniait.cliente_firma_sat.util;

public class Debug {

	/*
	 * Mensaje de error fatal
	 */
	public static void fatal(String debugMsg) {
		if(debugMsg != null && AppProperties.debug()) {
			System.err.println("[FATAL] " + debugMsg);
		}
	}

	/*
	 * Mensaje de error
	 */
	public static void error(String debugMsg) {
		if(debugMsg != null && AppProperties.debug()) {
			System.err.println("[ERROR] " + debugMsg);
		}
	}
	
	/*
	 * Mensaje de información
	 */
	public static void info(String debugMsg) {
		if(debugMsg != null && AppProperties.debug()) {
			System.out.println("[INFO] " + debugMsg);
		}
	}
}
