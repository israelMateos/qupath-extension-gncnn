package qupath.ext.gdcnn;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import qupath.lib.common.ThreadTools;
import qupath.lib.gui.QuPathGUI;
import qupath.ext.env.VirtualEnvironment;
import qupath.fx.dialogs.Dialogs;

/**
 * Glomeruli detection and classification using deep learning
 * 
 * @author Israel Mateos Aparicio
 */
public class GDCnn {

    private static final Logger logger = LoggerFactory.getLogger(GDCnn.class);

    private GDCnnSetup gdcnnSetup = GDCnnSetup.getInstance();

    private ExecutorService pool = Executors.newCachedThreadPool(ThreadTools.createThreadFactory("GDCnn", true));

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
     * Runs the whole pipeline of detection and classification of glomeruli using
     * Python
     *
     * @throws IOException          Exception in case files could not be read
     * @throws InterruptedException Exception in case of command thread has some
     *                              failing
     */
    public void runPipeline() throws IOException, InterruptedException {
        // Make sure that the Python path is not empty
        if (gdcnnSetup.getPythonPath().isEmpty()) {
            throw new IllegalStateException("Python path is empty. Please set it in Edit > Preferences");
        }

        VirtualEnvironment venv = new VirtualEnvironment(
                this.getClass().getSimpleName(), gdcnnSetup.getPythonPath(), gdcnnSetup.getGdcnnPath());

        // This is the list of commands after the 'python' call
        List<String> arguments = Arrays.asList(
                "/home/visilab/Israel/GDCnn/mescnn/detection/qupath/segment.py",
                "--wsi",
                "/home/visilab/Israel/GDCnn/Data/Dataset/WSI/NC 18.3 1 NTX pas-4 - 2020-02-07 10.58.48MC.ndpi",
                "--export",
                "/home/visilab/Israel/GDCnn/Data/Export/cascade_R_50_FPN_3x",
                "--model",
                "cascade_R_50_FPN_3x");
        venv.setArguments(arguments);

        // Run the pipeline
        venv.runCommand();

        logger.info("Pipeline finished running");
    }

    /**
     * Tiles each WSI and saves them in a temporary folder
     * 
     * @throws IOException // In case there is an issue reading the image
     */
    public void tileWSIs() throws IOException {
        submitTask(new TilerTask(qupath, 4096, 2048, 0, ".png"));
    }
}
