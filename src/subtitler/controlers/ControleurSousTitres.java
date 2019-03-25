package subtitler.controlers;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import subtitler.subtitles.Subtitle;
import subtitler.utils.ConversionStringMilli;
import subtitler.utils.modifSubtitleUtils;

public class ControleurSousTitres implements Initializable{

    @FXML
    private TableView<Subtitle> subtitlesView;

    @FXML
    private TableColumn<Subtitle, String> auteurView;
    
    @FXML
    private TableColumn<Subtitle, String> speechView;
    
    @FXML
    private TableColumn<Subtitle, String>  timeStart;

    @FXML
    private TableColumn<Subtitle, String> timeStop; 
    
    public static ControleurSousTitres controleur;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		controleur = this;
		auteurView.setCellValueFactory(cellData -> cellData.getValue().getContenu().get(0).getAuthorProperty());
		//subtitlesView.setItems(MainControler.subtitles.getObservableSubtitles());
		
		speechView.setCellValueFactory(cellData -> cellData.getValue().getContenu().get(0).getTextProperty());
		//subtitlesView.setItems(MainControler.subtitles.getObservableSubtitles());
		
		timeStart.setCellValueFactory(cellData -> cellData.getValue().getTimeStartProperty());
		
		timeStop.setCellValueFactory(cellData -> cellData.getValue().getTimeStopProperty());
		//subtitlesView.setItems(MainControler.subtitles.getObservableSubtitles());
		
		subtitlesView.setItems(MainControler.subtitles.getObservableSubtitles());
	}
	

	@FXML
    void modifierOnClick(ActionEvent event) {
		AnchorPane root;
		try {
			modifSubtitleUtils.selectedSubtitle = subtitlesView.getSelectionModel().getSelectedItem();
			root = (AnchorPane) FXMLLoader.load(new File("modifOneSubtitle.fxml").toURI().toURL());
			modifSubtitleUtils.modifOneSubtitleStage = new Stage(); 
			Scene scene = new Scene(root, 640, 380); 
			modifSubtitleUtils.modifOneSubtitleStage.setTitle("Modifier Un Sous-Titre");
			modifSubtitleUtils.modifOneSubtitleStage.setScene(scene); 
			modifSubtitleUtils.modifOneSubtitleStage.show();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
		
}
