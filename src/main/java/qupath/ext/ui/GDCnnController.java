package qupath.ext.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.controlsfx.control.CheckListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import qupath.ext.tasks.AnnotationExportTask;
import qupath.ext.tasks.ClassificationTask;
import qupath.ext.tasks.GlomerulusDetectionTask;
import qupath.ext.tasks.TilerTask;
import qupath.ext.tasks.TissueDetectionTask;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.common.GeneralTools;
import qupath.lib.common.ThreadTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.hierarchy.events.PathObjectSelectionModel;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;

public class GDCnnController {

    private static final Logger logger = LoggerFactory.getLogger(GDCnnController.class);

    private QuPathGUI qupath;

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

    private final ExecutorService pool = Executors
            .newSingleThreadExecutor(ThreadTools.createThreadFactory("GDCnn", true));

    private static final LinkedHashMap<String, String> PROGRESS_MESSAGES = new LinkedHashMap<String, String>() {
        {
            put("ThresholdTask", "Detecting tissue...");
            put("TilerTask", "Tiling images...");
            put("DetectionTask", "Detecting glomeruli... (this may take a while)");
            put("AnnotationExportTask", "Exporting glomerular annotations...");
            put("ClassificationTask", "Classifying glomeruli...");
        }
    };

    @FXML
    /**
     * Initializes the controller
     */
    private void initialize() {
        logger.info("Initializing...");

        qupath = QuPathGUI.getInstance();

        setUpInterfaceElements();

        // runAllBtn.disableProperty().bind(
        // Bindings.isNull(imgsCheckList.checkModelProperty())
        // );
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
            logger.info("Running all tasks");
            ObservableList<String> selectedImages = imgsCheckList.getCheckModel().getCheckedItems();
            try {
                thresholdForeground(selectedImages);
                tileWSIs(selectedImages);
                detectGlomeruli(selectedImages);
                exportAnnotations(selectedImages);
                classifyGlomeruli(selectedImages);
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
                thresholdForeground(selectedImages);
                tileWSIs(selectedImages);
                detectGlomeruli(selectedImages);
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
            logger.info("Running classification pipeline");
            ObservableList<String> selectedImages = imgsCheckList.getCheckModel().getCheckedItems();
            try {
                exportAnnotations(selectedImages);
                classifyGlomeruli(selectedImages);
            } catch (IOException e) {
                logger.error("Error running classification", e);
                Dialogs.showErrorMessage("Error running classification", e);
            }
        }
    }

    @FXML
    // TODO
    private void viewResults() {

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
     * Sets the progress indicator and label to show that the task is running
     */
    private void setProgressRunning(String taskName) {
        tickIconImg.setVisible(false);
        doneLabel.setVisible(false);
        progressInd.setVisible(true);
        progressLabel.setVisible(true);
        progressLabel.setText(PROGRESS_MESSAGES.get(taskName));
    }

    /**
     * Sets the progress indicator and label to show that the task is done
     */
    private void setProgressDone() {
        progressInd.setVisible(false);
        progressLabel.setVisible(false);
        tickIconImg.setVisible(true);
        doneLabel.setVisible(true);
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
    public void setUpInterfaceElements() {
        boolean disable = !isImageOrProjectOpen();
        imgsCheckList.setDisable(disable);
        selectAllImgsBtn.setDisable(disable);
        runAllBtn.setDisable(disable);
        runDetectionBtn.setDisable(disable);
        runClassificationBtn.setDisable(disable);
        viewResultsBtn.setDisable(disable);
        if (disable) {
            Dialogs.showErrorMessage("No image or project open", "Please open an image or project to run the tasks");
        } else {
            setImgsCheckListElements();
        }
    }

    /**
     * Submits a task to the thread pool to run in the background
     * 
     * @param task
     */
    private void submitTask(Task<?> task) {
        task.setOnRunning(e -> {
            logger.info("Task running");
            setProgressRunning(task.getClass().getSimpleName());
        });
        task.setOnSucceeded(e -> {
            if (task instanceof GlomerulusDetectionTask || task instanceof ClassificationTask) {
                // If there is an image selected, select an object from the
                // hierarchy and deselect it to refresh the viewer
                ImageData<BufferedImage> currentImageData = qupath.getViewer().getImageData();
                if (currentImageData != null) {
                    PathObjectHierarchy hierarchy = currentImageData.getHierarchy();
                    Collection<PathObject> objects = hierarchy.getAnnotationObjects();
                    if (!objects.isEmpty()) {
                        PathObject object = objects.iterator().next();
                        PathObjectSelectionModel selectionModel = hierarchy.getSelectionModel();
                        selectionModel.setSelectedObject(object);
                        selectionModel.clearSelection();
                    }
                }
                logger.info("Task succeeded");
                Dialogs.showInfoNotification("Task succeeded", task.getClass().getSimpleName() + " succeeded");
                setProgressDone();
            }
        });
        task.setOnFailed(e -> {
            logger.error("Task failed", e.getSource().getException());
            Dialogs.showErrorMessage("Task failed", e.getSource().getException());
        });
        pool.submit(task);
    }

    /**
     * Apply the threshold to separate the foreground from the background
     * 
     * @param selectedImages
     * @throws IOException
     */
    public void thresholdForeground(ObservableList<String> selectedImages) throws IOException {
        submitTask(new TissueDetectionTask(qupath, selectedImages, 20, ".jpeg"));
    }

    /**
     * Tiles each WSI and saves them in a temporary folder
     * 
     * @param selectedImages
     * @throws IOException // In case there is an issue reading the image
     */
    public void tileWSIs(ObservableList<String> selectedImages) throws IOException {
        submitTask(new TilerTask(qupath, selectedImages, 4096, 2048, 1, ".jpeg"));
    }

    /**
     * Detects glomeruli in the WSI patches
     * 
     * @param selectedImages
     * @throws IOException
     */
    public void detectGlomeruli(ObservableList<String> selectedImages) throws IOException {
        submitTask(new GlomerulusDetectionTask(qupath, selectedImages, "cascade_R_50_FPN_1x", "external", 1));
    }

    /**
     * Exports the annotations of each WSI to images
     * 
     * @param selectedImages
     */
    public void exportAnnotations(ObservableList<String> selectedImages) {
        submitTask(new AnnotationExportTask(qupath, selectedImages, 300, 1));
    }

    /**
     * Classifies annotated glomeruli
     * 
     * @param selectedImages
     * @throws IOException
     */
    public void classifyGlomeruli(ObservableList<String> selectedImages) throws IOException {
        submitTask(new ClassificationTask(qupath, selectedImages, "swin_transformer"));
    }

}
