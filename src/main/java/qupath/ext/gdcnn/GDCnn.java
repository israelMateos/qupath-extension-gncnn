package qupath.ext.gdcnn;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.ext.env.VirtualEnvironment;

/**
 * Glomeruli detection and classification using deep learning
 * 
 * @author Israel Mateos Aparicio
 */
public class GDCnn {

    private static final Logger logger = LoggerFactory.getLogger(GDCnn.class);

    private GDCnnSetup gdcnnSetup = GDCnnSetup.getInstance();

    public GDCnn() {
    }

    /**
     * Runs the whole pipeline of detection and classification of glomeruli using Python
     *
     * @throws IOException          Exception in case files could not be read
     * @throws InterruptedException Exception in case of command thread has some failing
     */
    public void runPipeline() throws IOException, InterruptedException {
        // Make sure that the Python path is not empty
        if (gdcnnSetup.getPythonPath().isEmpty()){
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
            "cascade_R_50_FPN_3x"
            );
        venv.setArguments(arguments);

        // Run the pipeline
        venv.runCommand();

        logger.info("Pipeline finished running");
    }
}
