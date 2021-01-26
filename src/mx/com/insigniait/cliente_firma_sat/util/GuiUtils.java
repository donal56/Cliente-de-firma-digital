package mx.com.insigniait.cliente_firma_sat.util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.input.KeyCode;
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
		
		Debug.fatal(debugMsg);
		
		if(e != null) {
			e.printStackTrace();
		}

		Alert alert = new Alert(AlertType.ERROR, errorMsg, ButtonType.OK);
		alert.setHeaderText("Error");
		alert.setTitle("Error");
		alert.setResizable(false);
		alert.initModality(Modality.APPLICATION_MODAL);
		alert.setOnCloseRequest(ev -> System.exit(0));
		alert.showAndWait();
	}
	
	public static void finalMessage(String mensaje) {
		
		 Task<Void> task = new Task<Void>() {

				@Override
				protected Void call() throws Exception {
					Alert alert = new Alert(AlertType.INFORMATION, mensaje, ButtonType.OK);
					alert.setHeaderText(mensaje);
					alert.setTitle(mensaje);
					alert.setResizable(false);
					alert.setOnCloseRequest(e -> System.exit(0));
					alert.showAndWait();
					
					return null;
				}
			 };
			 
		Platform.runLater(task);
	}
	
	public static Optional<String> decision(String titulo, String mensaje, List<String> decisiones) throws InterruptedException, ExecutionException {
		
		 Task<Optional<String>> task = new Task<Optional<String>>() {

			@Override
			protected Optional<String> call() throws Exception {
				ChoiceDialog<String> 	choice 		= 	new ChoiceDialog<String>(decisiones.get(0), decisiones);
				
				choice.setTitle(titulo);
				choice.setContentText(mensaje);
				
				return choice.showAndWait();
			}
		 };
		 
		 Platform.runLater(task);
		 
		 return task.get();
	}
}
