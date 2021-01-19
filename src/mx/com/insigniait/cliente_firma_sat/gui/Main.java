package mx.com.insigniait.cliente_firma_sat.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
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
import org.springframework.util.StringUtils;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import mx.com.insigniait.cliente_firma_sat.dto.SatSigner;
import mx.com.insigniait.cliente_firma_sat.services.SigningService;
import mx.com.insigniait.cliente_firma_sat.services.TimestampAuthorityClient;
import mx.com.insigniait.cliente_firma_sat.util.CredentialStorage;
import mx.com.insigniait.cliente_firma_sat.util.Util;

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
		
		//Limpiando parametros de la aplicación
		if(args.length > 0) {
			argumentos = Util.getProtocolQueryParams(args[0]);
		}

		Application.launch(args);
	}

	/*
	 * Comprobar argumentos de la aplicación y ejecutar GUI
	 */
	@Override
	public void start(Stage stage) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		
		//Comprobando parametros
		if(argumentos == null || argumentos.size() == 0 || !argumentos.containsKey("req") || !argumentos.containsKey("ss")) {
			throwError("Parámetros faltantes.");
			return;
		}
	
		String 				request 	= 	Util.decrypt(argumentos.get("req"), Util.getApplicationProperty("secret"), argumentos.get("ss"));
		Map<String, String> parametros 	= 	Util.createMapFromCommaSeparatedString(request);
		
		if(StringUtils.isEmpty(parametros.get("doc")) || StringUtils.isEmpty(parametros.get("username")) 
				|| StringUtils.isEmpty(parametros.get("password")) || StringUtils.isEmpty(parametros.get("referer")) 
						|| StringUtils.isEmpty(parametros.get("docId"))) {
			throwError("Solicitud no válida.");
			return;
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
		Util.setEnterAsClick(btnElegir);
		
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
				runSigner(parametros, stage, certCombo.getValue());
			}
		});
	}

	/*
	 * Proceso principal
	 */
	public void runSigner(Map<String, String> parametros, Stage stage, String certificateAlias) {

		//Credenciales
		X509Certificate cert;
		PrivateKey pk;
		Certificate[] chain;
		
		try {
			cert 	= 	CredentialStorage.getCertificate(certificateAlias);
			pk 		= 	CredentialStorage.getPrivateKey(certificateAlias);
			chain	=	CredentialStorage.getCertificateChain(certificateAlias);
			
			//Documento a firmar
			String 	docUrl 	= 	parametros.get("doc");
			File 	pdf 	= 	File.createTempFile("cfd_", ".pdf");
					
			FileUtils.copyInputStreamToFile(new URL(docUrl).openStream(), pdf);
			
			//Firma
			SatSigner 	signature 		= 	new SatSigner(cert, pk, chain);
			String		tsaClientUrl	=	Util.getApplicationProperty("tsa_client");
			signature.setTsaClient(new TimestampAuthorityClient(tsaClientUrl, null, null));
			
			File 		signedPdf 		=	new SigningService().signPdf(pdf, signature);
			
			//Autenticación de guardado
			String auth 		= 	parametros.get("username") + ":" + parametros.get("password");
			byte[] encodedAuth 	= 	Base64.getEncoder().encode(auth.getBytes("UTF-8"));
			
			//URL de guardado
			String  url = parametros.get("referer");
			url += url.endsWith("/") ? "" : "/";
			url += Util.getApplicationProperty("protocol") + "/save";
			
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
				//Código de respuesta
				HttpResponse 	response 		= 	HttpClients.custom().build().execute(request);
				int 			responseCode 	= 	response.getStatusLine().getStatusCode();
				
				//En caso de exito guardar la respuesta en los argumentos de la aplicación
				if (responseCode >= 200 && responseCode < 300) {
					argumentos.put("response", EntityUtils.toString(response.getEntity()));
					exito.set(true);
				} 
				//En caso de fallo mostrar un dialogo de decisión: Reintentar o guardar localmente
				else {
					ChoiceDialog<String> 	decision 	= 	new ChoiceDialog<String>();
					ObservableList<String> 	options 	= 	decision.getItems();
					
					decision.setTitle("Error de conexión");
					decision.setContentText("Ocurrio un error al subir el documento firmado: " + response.getStatusLine().getReasonPhrase());
					
					options.add("Reintentar");
					options.add("Guardar en mi PC");
					
					Optional<String> optionSelected 	= 	decision.showAndWait();
					
					optionSelected.ifPresent(option -> {
						switch (option) {
							case "Guardar localmente":
								try {
									String filePath = System.getProperty("user.home") + "/Desktop/" + signedPdf.getName() + ".pdf";
									FileUtils.moveFile(signedPdf, new File(filePath));
									exito.set(true);
								} 
								catch (IOException e) { }
						}
					});
				}
			}
			while(!exito.get());
			
			//Éxito
			Alert alert = new Alert(AlertType.INFORMATION, Arrays.asList(argumentos.values().toArray()).toString(), ButtonType.OK);
			alert.setHeaderText("Firma exitosa");
			alert.setTitle("Firma exitosa");
			alert.setResizable(false);
			alert.setOnCloseRequest(e2 -> System.exit(0));
			alert.show();
		} 
		catch (Exception e1) {
			e1.printStackTrace();
			throwError(e1.getMessage());
		}
	}
	
	/*
	 * Modal de error
	 */
	public void throwError(String errorMsg) {
		Alert alert = new Alert(AlertType.ERROR, errorMsg, ButtonType.OK);
		alert.setHeaderText("Error");
		alert.setTitle("Error");
		alert.setResizable(false);
		alert.show();
		alert.setOnCloseRequest(e -> System.exit(0));
	}
}
