package mx.com.insigniait.cliente_firma_sat.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.application.Platform;
import javafx.stage.Modality;

@SuppressWarnings("restriction")
public class GuiUtils {

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
	 * Modal de error
	 */
	public static void throwError(String errorMsg) {
		throwError(errorMsg, null, null);
	}
	
	public static void throwError(String errorMsg, String debugMsg) {
		throwError(errorMsg, debugMsg, null);
	}
	
	public static void throwError(String errorMsg, Exception e) {
		throwError(errorMsg, errorMsg, e);
	}
	
	public static void throwError(String errorMsg, String debugMsg, Exception e) {
		
		Platform.runLater(new Runnable() {

			@Override
			public void run() {

				Alert alert = new Alert(AlertType.ERROR, errorMsg, ButtonType.OK);
				alert.setHeaderText("Error");
				alert.setTitle("Error");
				alert.setResizable(false);
				alert.initModality(Modality.APPLICATION_MODAL);
				alert.setOnCloseRequest(ev -> System.exit(0));
				alert.show();
				
				Debug.fatal(debugMsg);
				
				if(e != null) {
					e.printStackTrace();
				}
			}
		});
	}
}
