/**
 * Copyright (C) 2024 Israel Mateos-Aparicio-Ruiz
 */
package qupath.ext.gdcnn.ui;

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import qupath.fx.dialogs.Dialogs;
import qupath.fx.utils.FXUtils;
import qupath.lib.gui.QuPathGUI;

/**
 * Command to open the extension.
 */
public class GDCnnCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GDCnnCommand.class);

    private final QuPathGUI qupath;

    private Stage stage;
    private GDCnnController controller;

    public GDCnnCommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    @Override
    public void run() {
        if (stage == null) {
            try {
                stage = createStage();
                stage.show();
                FXUtils.retainWindowPosition(stage);
            } catch (IOException e) {
                Dialogs.showErrorMessage("Error", "Error opening GDCnn");
                logger.error(e.getMessage(), e);
                return;
            }
        } else {
            stage.show();
        }
    }

    private Stage createStage() throws IOException {

        URL url = getClass().getResource("MainPane.fxml");
        if (url == null) {
            throw new IOException("Cannot find URL for MainPane FXML");
        }

        // We need to use the ExtensionClassLoader to load the FXML, since it's in a
        // different module
        var loader = new FXMLLoader(url);
        loader.setClassLoader(this.getClass().getClassLoader());
        GridPane root = (GridPane) loader.load();
        Scene scene = new Scene(root);
        controller = loader.getController();

        Stage stage = new Stage();
        stage.initOwner(qupath.getStage());
        stage.setTitle("GDCnn");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.initModality(Modality.WINDOW_MODAL);

        stage.setOnCloseRequest(e -> {
            // If a task is running, show an 'Are you sure?' dialog with
            // a warning message and option to cancel all tasks
            if (controller.isRunning()) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("GDCnn");
                alert.setHeaderText("Are you sure you want to close GDCnn?");
                alert.setContentText("There are tasks running. Closing GDCnn will cancel all tasks.");
                // If closing, cancel all tasks; if not, consume the event
                alert.showAndWait().filter(r -> r != null && r.getButtonData().equals(ButtonData.OK_DONE))
                        .ifPresent(r -> {
                            logger.info("Cancelling all tasks");
                            controller.cancelAllTasks();
                            stage.close();
                        });
                e.consume();
            }
        });

        controller.setStage(stage);

        root.heightProperty().addListener((v, o, n) -> handleStageHeightChange());

        return stage;
    }

    private void handleStageHeightChange() {
        stage.sizeToScene();
        // This fixes a bug where the stage would migrate to the corner of a screen if
        // it is resized, hidden, then shown again
        if (stage.isShowing() && Double.isFinite(stage.getX()) && Double.isFinite(stage.getY()))
            FXUtils.retainWindowPosition(stage);
    }

}