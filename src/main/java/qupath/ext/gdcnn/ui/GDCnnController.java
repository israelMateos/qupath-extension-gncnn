package qupath.ext.gdcnn.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.controlsfx.control.CheckListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import qupath.ext.gdcnn.tasks.AnnotationExportTask;
import qupath.ext.gdcnn.tasks.ClassificationTask;
import qupath.ext.gdcnn.tasks.GlomerulusDetectionTask;
import qupath.ext.gdcnn.tasks.TaskPaths;
import qupath.ext.gdcnn.tasks.TilerTask;
import qupath.ext.gdcnn.tasks.TissueDetectionTask;
import qupath.ext.gdcnn.utils.Utils;
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
import qupath.lib.scripting.QP;

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

    private final ExecutorService pool = Executors
            .newSingleThreadExecutor(ThreadTools.createThreadFactory("GDCnn", true));

    private final LinkedHashMap<String, String> PROGRESS_MESSAGES = new LinkedHashMap<String, String>() {
        {
            put("TissueDetectionTask", "Detecting tissue...");
            put("TilerTask", "Tiling images...");
            put("GlomerulusDetectionTask", "Detecting glomeruli... (this may take a while)");
            put("AnnotationExportTask", "Exporting glomerular annotations...");
            put("ClassificationTask", "Classifying glomeruli...");
        }
    };

    private final ObservableSet<Task<?>> currentTasks = FXCollections.observableSet();

    private final BooleanBinding taskRunning = Bindings.isNotEmpty(currentTasks);

    public final boolean isTaskRunning() {
        return taskRunning.get();
    }

    @FXML
    /**
     * Initializes the controller
     */
    private void initialize() {
        logger.info("Initializing...");

        qupath = QuPathGUI.getInstance();

        setUpInterfaceElements();
        bindButtonsToSelectedImages();
    }

    /**
     * Sets the GDCnn window stage
     * 
     * @param stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Binds the buttons to the selected images in the check list
     */
    private void bindButtonsToSelectedImages() {
        BooleanBinding selectedImagesBinding = Bindings.isEmpty(imgsCheckList.getCheckModel().getCheckedItems());
        runAllBtn.disableProperty().bind(selectedImagesBinding);
        runDetectionBtn.disableProperty().bind(selectedImagesBinding);
        runClassificationBtn.disableProperty().bind(selectedImagesBinding);
        viewResultsBtn.disableProperty().bind(selectedImagesBinding);
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
                detectTissue(selectedImages);
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
                detectTissue(selectedImages);
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
            ObservableList<String> selectedImages = imgsCheckList.getCheckModel().getCheckedItems();
            List<String> imgsWithGlomeruli;
            boolean continueClassification = false;
            try {
                imgsWithGlomeruli = getImgsWithGlomeruli(selectedImages);
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
                ClassificationWarningPane warningPane = new ClassificationWarningPane(stage);
                continueClassification = warningPane.show(imgsWithGlomeruli);
            }

            if (continueClassification) {
                logger.info("Running classification pipeline");
                try {
                    exportAnnotations(imgsWithGlomeruli);
                    classifyGlomeruli(imgsWithGlomeruli);
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
     * Checks if there are "Glomerulus" annotations in the selected images, and
     * returns the images with "Glomerulus" annotations
     * 
     * @param selectedImages
     * @return The images with "Glomerulus" annotations
     * @throws IOException
     */
    private List<String> getImgsWithGlomeruli(ObservableList<String> selectedImages) throws IOException {
        List<String> imgsWithGlomeruli = new ArrayList<String>();

        Project<BufferedImage> project = qupath.getProject();
        if (project != null) {
            // Check for glomerulus annotations in the selected images
            List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
            for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
                String imageName = GeneralTools.stripExtension(imageEntry.getImageName());
                if (selectedImages.contains(imageName)) {
                    ImageData<BufferedImage> imageData = imageEntry.readImageData();
                    PathObjectHierarchy hierarchy = imageData.getHierarchy();
                    Collection<PathObject> annotations = hierarchy.getAnnotationObjects();

                    for (PathObject annotation : annotations) {
                        if (annotation.getPathClass() != null
                                && annotation.getPathClass().getName().equals("Glomerulus")) {
                            imgsWithGlomeruli.add(imageName);
                            break;
                        }
                    }
                }
            }
        } else {
            // Check for glomerulus annotations in the current image
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                String imageName = GeneralTools.stripExtension(imageData.getServer().getMetadata().getName());
                PathObjectHierarchy hierarchy = imageData.getHierarchy();
                Collection<PathObject> annotations = hierarchy.getAnnotationObjects();

                for (PathObject annotation : annotations) {
                    if (annotation.getPathClass() != null
                            && annotation.getPathClass().getName().equals("Glomerulus")) {
                        imgsWithGlomeruli.add(imageName);
                        break;
                    }
                }
            } else {
                logger.error("No project or image is open");
            }
        }

        return imgsWithGlomeruli;
    }

    /**
     * If dealing with a project and one of the selected images is selected in
     * the viewer, removes the image data from the viewer to fix a bug where the
     * annotations are not updated in the viewer although they are updated in
     * the hierarchy
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

    /**
     * Submits a task to the thread pool to run in the background
     * 
     * @param task
     */
    private void submitTask(Task<?> task) {
        task.setOnRunning(e -> {
            logger.trace("Task running");
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
            }
            logger.info("Task succeeded");
            Dialogs.showInfoNotification("Task succeeded", task.getClass().getSimpleName() + " succeeded");
            setProgressDone();
        });
        task.setOnFailed(e -> {
            logger.error("Task failed", e.getSource().getException());
            Dialogs.showErrorMessage("Task failed", e.getSource().getException());
        });
        pool.submit(task);
        currentTasks.add(task);
        task.stateProperty().addListener((Observable o) -> {
            if (task.isDone()) {
                currentTasks.remove(task);
            }
        });
    }

    /**
     * Cancels all the tasks in the thread pool
     */
    public void cancelAllTasks() {
        logger.info("Cancelling all tasks");
        pool.shutdownNow();

        // Clean the temporary files
        logger.info("Cleaning temporary files");
        String outputBaseDir = Utils.getBaseDir(qupath);
        File tempFolder = new File(QP.buildFilePath(outputBaseDir, TaskPaths.TMP_FOLDER));
        if (tempFolder.exists()) {
            Utils.deleteFolder(tempFolder);
        }
    }

    /**
     * Apply the threshold to separate the foreground from the background
     * 
     * @param selectedImages
     * @throws IOException
     */
    private void detectTissue(ObservableList<String> selectedImages) throws IOException {
        submitTask(new TissueDetectionTask(qupath, selectedImages, 20, ".jpeg"));
    }

    /**
     * Tiles each WSI and saves them in a temporary folder
     * 
     * @param selectedImages
     * @throws IOException // In case there is an issue reading the image
     */
    private void tileWSIs(ObservableList<String> selectedImages) throws IOException {
        submitTask(new TilerTask(qupath, selectedImages, 4096, 2048, 1, ".jpeg"));
    }

    /**
     * Detects glomeruli in the WSI patches
     * 
     * @param selectedImages
     * @throws IOException
     */
    private void detectGlomeruli(ObservableList<String> selectedImages) throws IOException {
        refreshViewer(selectedImages);
        submitTask(new GlomerulusDetectionTask(qupath, selectedImages, "cascade_R_50_FPN_1x", "external", 1));
    }

    /**
     * Exports the annotations of each WSI to images
     * 
     * @param selectedImages
     */
    private void exportAnnotations(List<String> selectedImages) {
        submitTask(new AnnotationExportTask(qupath, selectedImages, 300, 1));
    }

    /**
     * Classifies annotated glomeruli
     * 
     * @param selectedImages
     * @throws IOException
     */
    private void classifyGlomeruli(List<String> selectedImages) throws IOException {
        refreshViewer(selectedImages);
        submitTask(new ClassificationTask(qupath, selectedImages, "swin_transformer"));
    }

}
