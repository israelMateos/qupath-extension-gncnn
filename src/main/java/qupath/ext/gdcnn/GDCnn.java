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

    private GDCnnSetup gdcnnSetup = GDCnnSetup.getInstance();

    private ExecutorService pool = Executors.newSingleThreadExecutor(ThreadTools.createThreadFactory("GDCnn", true));

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
        task.setOnFailed(e -> {
            logger.error("Task failed", e.getSource().getException());
            Dialogs.showErrorMessage("Task failed", e.getSource().getException());
        });
        pool.submit(task);
    }

    /**
     * Tiles each WSI and saves them in a temporary folder
     * 
     * @throws IOException // In case there is an issue reading the image
     */
    public void tileWSIs() throws IOException {
        submitTask(new TilerTask(qupath, 4096, 2048, 1, ".jpeg", gdcnnSetup.getGdcnnPath()));
    }

    /**
     * Detects glomeruli in the WSI patches
     * @throws IOException 
     */
    public void detectGlomeruli() throws IOException {
        submitTask(new DetectionTask(qupath, "cascade_R_50_FPN_3x", "external", 1, gdcnnSetup.getPythonPath(),
                gdcnnSetup.getGdcnnPath()));
        // If dealing with a project, remove the image data from the viewer
        // to refresh the viewer after the detection
        Project<BufferedImage> project = qupath.getProject();
        ImageData<BufferedImage> currentImageData = qupath.getViewer().getImageData();
        if (project != null && currentImageData != null) {
            qupath.getViewer().setImageData(null);
        }
    }
}
