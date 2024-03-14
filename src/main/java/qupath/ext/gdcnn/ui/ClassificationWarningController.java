package qupath.ext.gdcnn.ui;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

/**
 * Controller for the classification warning pane
 */
public class ClassificationWarningController {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationWarningController.class);

    @FXML
    private ListView<String> imgsListView;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button okBtn;

    private boolean continueClassification = false;

    public boolean continueClassification() {
        return continueClassification;
    }

    @FXML
    /**
     * Initializes the controller
     */
    private void initialize() {
        logger.info("Initializing...");
    }

    /**
     * Sets the list of images which will not be classified
     * 
     * @param imgs List of images which will not be classified
     */
    public void setImages(List<String> imgs) {
        imgsListView.setItems(FXCollections.observableArrayList(imgs));
    }

    @FXML
    private void okBtnClicked() {
        logger.info("Ok button clicked");
        continueClassification = true;
        // Get the stage and close it
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void cancelBtnClicked() {
        logger.info("Cancel button clicked");
        continueClassification = false;
        // Get the stage and close it
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

}
