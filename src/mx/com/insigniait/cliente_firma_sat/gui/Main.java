package mx.com.insigniait.cliente_firma_sat.gui;

import java.util.Arrays;
import java.util.Enumeration;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import mx.com.insigniait.cliente_firma_sat.gui.util.CredentialStorage;

public class Main extends Application {
	
	private static String[] options;

	public static void main(String[] args) {
		options = args;
		
		Application.launch(args);
	}
	
    @Override
    public void start(Stage stage) throws Exception {
        
    	//Etiquetas
        Label 	labelCrt = new Label("Seleccionar certificado");
        labelCrt.setPrefWidth(350);
        
        //Combo de certificados instalados
        ComboBox<String>  	certCombo 	= 	new ComboBox<String>();
        certCombo.setPrefWidth(350);
        certCombo.setPromptText("--Seleccione uno--");

        Enumeration<String> aliases = CredentialStorage.getAliases();
        
    	while(aliases.hasMoreElements()) {
    		certCombo.getItems().add(aliases.nextElement());
    	}
        
        //Icono de botón de formulario
        ImageView checkIcon = new ImageView(new Image("check.png"));
        checkIcon.setFitHeight(22);
        checkIcon.setFitWidth(22);
        
        //Botón de formulario
        Button btnInstalar = new Button("Firmar");
        btnInstalar.setGraphic(checkIcon);
        btnInstalar.setPrefWidth(160);
        
        //Panel
        FlowPane flowPane = new FlowPane();
        flowPane.getChildren().add(labelCrt);
        flowPane.getChildren().add(certCombo);
        flowPane.getChildren().add(btnInstalar);
        flowPane.setPadding(new Insets(20, 20, 20, 20));
        flowPane.setColumnHalignment(HPos.CENTER);
        flowPane.setOrientation(Orientation.VERTICAL);
        flowPane.setHgap(20);
        flowPane.setVgap(10);
        
        //Contenedor principal
        Scene scene = new Scene(flowPane, 400, 150);
        
        //Configuración de ventana
        stage.setTitle("Cliente de firma digital SAT");
        stage.getIcons().add(new Image("icono.png"));
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
        
        //Eventos
        btnInstalar.setOnAction(e -> {
        	if(certCombo.getValue() != null) {
        		
        		Alert alert = new Alert (AlertType.INFORMATION, Arrays.asList(options).toString(), ButtonType.OK); 
        		alert.setHeaderText("Firma exitosa");
        		alert.setTitle("Firma exitosa");
        		alert.setResizable(false);
        		alert.show();

        	}
        });
    }
}

