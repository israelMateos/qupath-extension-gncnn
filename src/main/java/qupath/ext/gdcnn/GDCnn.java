package qupath.ext.gdcnn;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import qupath.lib.common.ThreadTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.projects.Project;
import qupath.fx.dialogs.Dialogs;

/**
 * Glomeruli detection and classification using deep learning
 * 
 * @author Israel Mateos Aparicio
 */
public class GDCnn {

    private static final Logger logger = LoggerFactory.getLogger(GDCnn.class);

    private ExecutorService pool = Executors.newSingleThreadExecutor(ThreadTools.createThreadFactory("GDCnn", true));

    private final int TOTAL_TASKS = 4;

    private int completedTasks = 0;

    private QuPathGUI qupath;

    public GDCnn(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    /**
     * Submits a task to the thread pool to run in the background
     * 
     * @param task
     */
    private void submitTask(Task<?> task) {
        task.setOnSucceeded(e -> {
            logger.info("Task succeeded");
            Dialogs.showInfoNotification("Task succeeded", task.getClass().getSimpleName() + " succeeded");
        });
        task.setOnFailed(e -> {
            logger.error("Task failed", e.getSource().getException());
            Dialogs.showErrorMessage("Task failed", e.getSource().getException());
        });
        pool.submit(task);
        task.stateProperty().addListener((obs, oldState, newState) -> taskStateChange(task, newState));
    }

    /**
     * Handles the state change of a task
     * 
     * @param task
     * @param newState
     */
    private void taskStateChange(Task<?> task, Task.State newState) {
        if (newState == Task.State.SUCCEEDED) {
            logger.info("Task {} succeeded", task);
            completedTasks++;
        } else if (newState == Task.State.FAILED) {
            logger.error("Task {} failed", task);
            completedTasks++;
        } else if (newState == Task.State.CANCELLED) {
            logger.info("Task {} cancelled", task);
            completedTasks++;
        }
    }

    /**
     * Apply the threshold to separate the foreground from the background
     * 
     * @throws IOException
     */
    public void thresholdForeground() throws IOException {
        submitTask(new ThresholdTask(qupath, 20, ".jpeg"));
    }

    /**
     * Tiles each WSI and saves them in a temporary folder
     * 
     * @throws IOException // In case there is an issue reading the image
     */
    public void tileWSIs() throws IOException {
        submitTask(new TilerTask(qupath, 4096, 2048, 1, ".jpeg"));
    }

    /**
     * Detects glomeruli in the WSI patches
     * 
     * @throws IOException
     */
    public void detectGlomeruli() throws IOException {
        submitTask(new DetectionTask(qupath, "cascade_R_50_FPN_1x", "external", 1));
        // If dealing with a project, remove the image data from the viewer
        // to refresh the viewer after the detection
        Project<BufferedImage> project = qupath.getProject();
        ImageData<BufferedImage> currentImageData = qupath.getViewer().getImageData();
        if (project != null && currentImageData != null) {
            qupath.getViewer().setImageData(null);
        }
    }

    /**
     * Exports the annotations of each WSI to images
     * 
     */
    public void exportAnnotations() {
        submitTask(new AnnotationExportTask(qupath, 300, 1));
    }

    /**
     * Classifies annotated glomeruli
     * 
     * @throws IOException
     */
    public void classifyGlomeruli() throws IOException {
        submitTask(new ClassificationTask(qupath, "swin_transformer"));
        // If dealing with a project, remove the image data from the viewer
        // to refresh the viewer after the detection
        Project<BufferedImage> project = qupath.getProject();
        ImageData<BufferedImage> currentImageData = qupath.getViewer().getImageData();
        if (project != null && currentImageData != null) {
            qupath.getViewer().setImageData(null);
        }
    }
}