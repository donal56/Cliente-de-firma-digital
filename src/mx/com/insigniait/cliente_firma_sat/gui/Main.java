package mx.com.insigniait.cliente_firma_sat.gui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Map;

import org.springframework.util.StringUtils;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import mx.com.insigniait.cliente_firma_sat.tasks.SignerTask;
import mx.com.insigniait.cliente_firma_sat.util.AppProperties;
import mx.com.insigniait.cliente_firma_sat.util.CredentialStorage;
import mx.com.insigniait.cliente_firma_sat.util.Debug;
import mx.com.insigniait.cliente_firma_sat.util.GuiUtils;
import mx.com.insigniait.cliente_firma_sat.util.Utils;

/*
 * https://jvmfy.com/2018/11/17/how-to-digitally-sign-pdf-files/
 * https://jvmfy.com/2018/12/06/timestamp-in-digital-signatures/
 * https://svn.apache.org/repos/asf/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/signature/ 
 */

@SuppressWarnings("restriction")
public class Main extends Application {

	/*
	 * Argumentos de la aplicación
	 */
	private static Map<String, String> argumentos = null;

	/*
	 * Punto de entrada de la aplicación
	 */
	public static void main(String[] args) throws IllegalStateException, FileNotFoundException, IOException {

		// Limpiando parametros de la aplicación
		if (args.length > 0) {
			argumentos 	= 	Utils.getProtocolQueryParams(args[0]);
		}

		Application.launch(args);
	}

	/*
	 * Comprobar argumentos de la aplicación y ejecutar GUI
	 */
	@Override
	public void start(Stage stage) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		
		//Comprobando parametros
		if(	argumentos == null || 
			argumentos.size() == 0 || 
			!argumentos.containsKey("req") || 
			!argumentos.containsKey("ss")) {
			
			GuiUtils.throwError("Parámetros faltantes.", "Solicitud o secret-salt faltantes: " + argumentos.toString());
		}

		//Desencriptando parametros
		String 				request 	= 	Utils.decrypt(argumentos.get("req"), AppProperties.get("secret"), argumentos.get("ss"));
		Map<String, String> parametros 	= 	Utils.createMapFromCommaSeparatedString(request);

		if(	StringUtils.isEmpty(parametros.get("doc")) || 
			StringUtils.isEmpty(parametros.get("username")) || 
			StringUtils.isEmpty(parametros.get("password")) || 
			StringUtils.isEmpty(parametros.get("referer")) || 
			StringUtils.isEmpty(parametros.get("docId"))) {
			
			GuiUtils.throwError("Solicitud no válida.", "URL del documento, ID del documento o datos de conexión(URL, usuario, contraseña) "
					+ "faltantes: " + parametros.toString());
		}

		// Etiquetas
		Label labelCrt = new Label("Seleccionar certificado");
		labelCrt.setPrefWidth(380);

		// Combo de certificados instalados
		ComboBox<String> certCombo = new ComboBox<String>();
		certCombo.setPrefWidth(380);
		certCombo.setPromptText("--Seleccione uno--");

		Enumeration<String> aliases = CredentialStorage.getAliases();

		while (aliases.hasMoreElements()) {
			certCombo.getItems().add(aliases.nextElement());
		}

		// Icono de botón de formulario
		ImageView checkIcon = new ImageView(new Image("check.png"));
		checkIcon.setFitHeight(22);
		checkIcon.setFitWidth(22);

		// Botón de formulario
		Button btnElegir = new Button("Elegir");
		btnElegir.setGraphic(checkIcon);
		btnElegir.setPrefWidth(160);
		GuiUtils.setEnterAsClick(btnElegir);

		// Panel
		FlowPane flowPane = new FlowPane();
		flowPane.getChildren().add(labelCrt);
		flowPane.getChildren().add(certCombo);
		flowPane.getChildren().add(btnElegir);
		flowPane.setPadding(new Insets(20, 20, 20, 20));
		flowPane.setColumnHalignment(HPos.CENTER);
		flowPane.setOrientation(Orientation.VERTICAL);
		flowPane.setHgap(20);
		flowPane.setVgap(10);

		// Contenedor principal
		Scene scene = new Scene(flowPane, 420, 150);

		// Configuración de ventana
		stage.setTitle("Cliente de firma digital SAT");
		stage.getIcons().add(new Image("icono.png"));
		stage.setResizable(false);
		stage.setScene(scene);
		stage.show();

		// Eventos
		btnElegir.setOnAction(e -> {
			if (certCombo.getValue() != null) {
				
				//Cargando
				btnElegir.setText("Cargando...");
				btnElegir.setDisable(true);
				certCombo.setDisable(true);
				
				parametros.put("alias", certCombo.getValue());
				Debug.info("Certificado seleccionado: " + parametros.get("alias"));
				Debug.info("Iniciando firma...");
				
				new Thread(new SignerTask(parametros)).start();
			}
		});
	}
}
