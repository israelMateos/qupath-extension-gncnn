package qupath.ext.gdcnn.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import qupath.ext.gdcnn.entities.ImageResult;
import qupath.ext.gdcnn.tasks.TaskManager;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;

/**
 * Controller for the GDCnn extension main UI
 */
public class GDCnnController {

    private static final Logger logger = LoggerFactory.getLogger(GDCnnController.class);

    private QuPathGUI qupath;

    private Stage stage;

    @FXML
    private Button selectAllImgsBtn;
    @FXML
    private Button runAllBtn;
    @FXML
    private Button runDetectionBtn;
    @FXML
    private Button runClassificationBtn;
    @FXML
    private ProgressIndicator progressInd;
    @FXML
    private Label progressLabel;
    @FXML
    private ImageView tickIconImg;
    @FXML
    private Label doneLabel;
    @FXML
    private Button viewResultsBtn;
    @FXML
    private CheckListView<String> imgsCheckList;
    @FXML
    private Button cancelBtn;

    private TaskManager taskManager;

    @FXML
    /**
     * Initializes the controller
     */
    private void initialize() {
        logger.info("Initializing...");

        qupath = QuPathGUI.getInstance();
        taskManager = new TaskManager(qupath);

        setUpInterfaceElements();
        cancelBtn.disableProperty().bind(taskManager.runningProperty().not());
        bindButtonsToSelectedImages();
        bindProgress();
    }

    /**
     * Sets the GDCnn window stage
     * 
     * @param stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isRunning() {
        return taskManager.isRunning();
    }

    /**
     * Cancels all the tasks
     */
    public void cancelAllTasks() {
        ObservableList<String> selectedImages = imgsCheckList.getCheckModel().getCheckedItems();
        try {
            taskManager.cancelAllTasks(selectedImages);
        } catch (IOException e) {
            logger.error("Error cancelling all tasks", e);
            Dialogs.showErrorMessage("Error cancelling all tasks", e);
        }
    }

    @FXML
    /**
     * Shows a confirmation alert and cancels all the tasks if the user confirms
     */
    private void showCancelConfirmation() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("GDCnn");
        alert.setHeaderText("Are you sure you want to close GDCnn?");
        alert.setContentText("There are tasks running. Closing GDCnn will cancel all tasks.");
        // If closing, cancel all tasks; if not, close the alert
        alert.showAndWait().filter(r -> r != null && r.getButtonData().equals(ButtonData.OK_DONE))
                .ifPresent(r -> {
                    logger.info("Cancelling all tasks");
                    cancelAllTasks();
                });
        alert.close();
    }

    /**
     * Binds the progress indicator percentage to the task progress, as well as
     * the progress label to the task name
     */
    private void bindProgress() {
        progressInd.progressProperty().bind(taskManager.progressProperty());
        progressLabel.textProperty().bind(taskManager.messageProperty());

        progressInd.visibleProperty().bind(taskManager.runningProperty());
        progressLabel.visibleProperty().bind(taskManager.runningProperty());

        tickIconImg.visibleProperty().bind(taskManager.doneProperty());
        doneLabel.visibleProperty().bind(taskManager.doneProperty());
    }

    /**
     * Binds the buttons to the selected images in the check list
     */
    private void bindButtonsToSelectedImages() {
        BooleanBinding selectedImagesBinding = Bindings.isEmpty(imgsCheckList.getCheckModel().getCheckedItems());
        runAllBtn.disableProperty().bind(selectedImagesBinding);
        runDetectionBtn.disableProperty().bind(selectedImagesBinding);
        runClassificationBtn.disableProperty().bind(selectedImagesBinding);
    }

    @FXML
    /**
     * Runs the detection and classification of the glomeruli
     */
    private void runAll() {
        if (!isImageOrProjectOpen()) {
            Dialogs.showErrorMessage("No image or project open", "Please open an image or project to run the tasks");
            return;
        } else {
            ObservableList<String> selectedImages = imgsCheckList.getCheckModel().getCheckedItems();
            logger.info("Running all tasks");
            try {
                refreshViewer(selectedImages);
                taskManager.runAll(selectedImages);
            } catch (IOException e) {
                logger.error("Error running all tasks", e);
                Dialogs.showErrorMessage("Error running all tasks", e);
            }
        }
    }

    @FXML
    /**
     * Runs the detection of the glomeruli
     */
    private void runDetection() {
        if (!isImageOrProjectOpen()) {
            Dialogs.showErrorMessage("No image or project open", "Please open an image or project to run the tasks");
            return;
        } else {
            logger.info("Running detection pipeline");
            ObservableList<String> selectedImages = imgsCheckList.getCheckModel().getCheckedItems();
            try {
                refreshViewer(selectedImages);
                taskManager.runDetection(selectedImages);
            } catch (IOException e) {
                logger.error("Error running detection", e);
                Dialogs.showErrorMessage("Error running detection", e);
            }
        }
    }

    @FXML
    /**
     * Runs the classification of the glomeruli
     */
    private void runClassification() {
        if (!isImageOrProjectOpen()) {
            Dialogs.showErrorMessage("No image or project open", "Please open an image or project to run the tasks");
            return;
        } else {
            ObservableList<String> selectedImages = imgsCheckList.getCheckModel().getCheckedItems();
            List<String> imgsWithGlomeruli;
            boolean continueClassification = true;

            try {
                imgsWithGlomeruli = taskManager.getImgsWithGlomeruli(selectedImages);
            } catch (IOException e) {
                logger.error("Error checking \"Glomerulus\" annotations", e);
                Dialogs.showErrorMessage("Error checking \"Glomerulus\" annotations", e);
                return;
            }

            if (imgsWithGlomeruli.isEmpty()) {
                // If all the selected images don't have "Glomerulus" annotations, show
                // an error message
                Dialogs.showErrorMessage("No \"Glomerulus\" annotations",
                        "There are no \"Glomerulus\" annotations in the selected images.\nPlease run the detection pipeline first or annotate them manually.");
                return;
            } else if (imgsWithGlomeruli.size() < selectedImages.size()) {
                // If there are less images with "Glomerulus" annotations than the
                // selected images, show a warning message
                List<String> imgsWithoutGlomeruli = selectedImages.stream()
                        .filter(img -> !imgsWithGlomeruli.contains(img))
                        .collect(Collectors.toList());
                ClassificationWarningPane warningPane = new ClassificationWarningPane(stage);
                continueClassification = warningPane.show(imgsWithGlomeruli, imgsWithoutGlomeruli);
            }

            if (continueClassification) {
                logger.info("Running classification pipeline");
                try {
                    refreshViewer(imgsWithGlomeruli);
                    taskManager.runClassification(imgsWithGlomeruli);
                } catch (IOException e) {
                    logger.error("Error running classification", e);
                    Dialogs.showErrorMessage("Error running classification", e);
                }
            } else {
                logger.info("Classification cancelled");
            }
        }
    }

    @FXML
    /**
     * Shows the results of the detection and classification of the glomeruli
     */
    private void viewResults() {
        if (!isImageOrProjectOpen()) {
            Dialogs.showErrorMessage("No image or project open", "Please open an image or project to view the results");
            return;
        } else {
            logger.info("Showing results");
            try {
                ObservableList<ImageResult> results = taskManager.getResults();
                logger.info("Results: " + results);
                ResultsPane resultsPane = new ResultsPane(stage);
                resultsPane.show(results);
            } catch (IOException e) {
                logger.error("Error showing results", e);
                Dialogs.showErrorMessage("Error showing results", e);
            }
        }
    }

    @FXML
    /**
     * Selects all the images in the check list
     */
    private void selectAllImgs() {
        imgsCheckList.getCheckModel().checkAll();
    }

    /**
     * Returns true if an image or project is open, false otherwise
     * 
     * @return True if an image or project is open, false otherwise
     */
    private boolean isImageOrProjectOpen() {
        return qupath.getProject() != null || qupath.getImageData() != null;
    }

    /**
     * Puts the project images in the check list
     */
    private void setImgsCheckListElements() {
        ObservableList<String> imgsCheckListItems = FXCollections.observableArrayList();
        Project<BufferedImage> project = qupath.getProject();
        if (project != null) {
            // Add all images in the project to the list
            List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();

            for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
                String imageName = GeneralTools.stripExtension(imageEntry.getImageName());
                imgsCheckListItems.add(imageName);
            }
        } else {
            // Add the current image to the list
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                String imageName = GeneralTools.stripExtension(imageData.getServer().getMetadata().getName());
                imgsCheckListItems.add(imageName);
            } else {
                logger.error("No project or image is open");
            }
        }

        imgsCheckList.setItems(imgsCheckListItems);
    }

    /**
     * Sets up the interface elements
     */
    private void setUpInterfaceElements() {
        boolean disable = !isImageOrProjectOpen();
        imgsCheckList.setDisable(disable);
        selectAllImgsBtn.setDisable(disable);
        runAllBtn.setDisable(disable);
        runDetectionBtn.setDisable(disable);
        runClassificationBtn.setDisable(disable);
        viewResultsBtn.setDisable(disable);
        if (!disable) {
            setImgsCheckListElements();
        }
    }

    /**
     * If dealing with a project and one of the selected images is selected in
     * the viewer, removes the image data from the viewer to fix a bug where the
     * annotations are not updated in the viewer although they are updated in
     * the hierarchy
     * 
     * @param selectedImages
     * @throws IOException
     */
    private void refreshViewer(List<String> selectedImages) throws IOException {
        Project<BufferedImage> project = qupath.getProject();
        ImageData<BufferedImage> currentImageData = qupath.getViewer().getImageData();
        if (project != null && currentImageData != null) {
            String currentImageName = GeneralTools.stripExtension(currentImageData.getServer().getMetadata().getName());
            if (selectedImages.contains(currentImageName)) {
                qupath.getViewer().setImageData(null);
            }
        }
    }
}
