package qupath.ext.gdcnn.ui;

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import qupath.ext.gdcnn.entities.ImageResult;

/**
 * Pane to display the results of the detection and classification of glomeruli.
 * 
 * @author Israel Mateos Aparicio
 */
public class ResultsPane {

    private static final Logger logger = LoggerFactory.getLogger(ResultsPane.class);

    private final Stage ownerStage;

    private Stage stage;
    private ResultsController controller;

    public ResultsPane(Stage ownerStage) {
        this.ownerStage = ownerStage;
    }

    public void show(ObservableList<ImageResult> results) {
        if (stage == null) {
            try {
                stage = createStage(results);
                stage.showAndWait();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            stage.showAndWait();
        }
    }

    private Stage createStage(ObservableList<ImageResult> results) throws IOException {
        URL url = getClass().getResource("ResultsPane.fxml");
        if (url == null) {
            throw new IOException("Cannot find URL for ResultsPane FXML");
        }

        // We need to use the ExtensionClassLoader to load the FXML, since it's in a
        // different module
        var loader = new FXMLLoader(url);
        loader.setClassLoader(this.getClass().getClassLoader());
        GridPane root = (GridPane) loader.load();
        controller = loader.getController();

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(ownerStage);
        stage.setResizable(true);
        stage.setScene(new Scene(root));
        stage.setTitle("Results");

        controller.fillTable(results);

        return stage;
    }
}