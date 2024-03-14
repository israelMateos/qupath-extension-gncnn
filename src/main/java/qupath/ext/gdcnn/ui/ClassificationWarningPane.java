package qupath.ext.gdcnn.ui;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Pane to display a warning message when the classification is not possible.
 * 
 * @author Israel Mateos Aparicio
 */
public class ClassificationWarningPane {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationWarningPane.class);

    private final Stage ownerStage;

    private Stage stage;
    private ClassificationWarningController controller;

    public ClassificationWarningPane(Stage ownerStage) {
        this.ownerStage = ownerStage;
    }

    public boolean show(List<String> images) {
        if (stage == null) {
            try {
                stage = createStage(images);
                stage.showAndWait();
                return controller.continueClassification();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        } else {
            stage.showAndWait();
            return controller.continueClassification();
        }
    }

    private Stage createStage(List<String> images) throws IOException {
        URL url = getClass().getResource("ClassificationWarningPane.fxml");
        if (url == null) {
            throw new IOException("Cannot find URL for MainPane FXML");
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
        stage.setResizable(false);
        stage.setScene(new Scene(root));
        stage.setTitle("Warning");

        controller.setImages(images);

        return stage;
    }
}