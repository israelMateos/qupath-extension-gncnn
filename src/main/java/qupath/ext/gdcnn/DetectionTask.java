package qupath.ext.gdcnn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javafx.concurrent.Task;

import java.util.List;
import java.util.Arrays;

import qupath.lib.gui.QuPathGUI;
import qupath.ext.env.VirtualEnvironment;
import qupath.lib.common.GeneralTools;
import qupath.lib.images.ImageData;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;

/**
 * Class to detect glomeruli in the WSI patches, and will return a list of the
 * detected objects
 */
// public class DetectionTask extends Task<List<PathObject>> {
public class DetectionTask extends Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger(DetectionTask.class);

    private QuPathGUI qupath;

    private String modelName;

    private String trainConfig;

    private int undersampling;

    private String pythonPath;

    private String gdcnnPath;

    public DetectionTask(QuPathGUI quPath, String modelName, String trainConfig, int undersampling, String pythonPath,
            String gdcnnPath) {
        this.qupath = quPath;
        this.modelName = modelName;
        this.trainConfig = trainConfig;
        this.undersampling = undersampling;
        this.pythonPath = pythonPath;
        this.gdcnnPath = gdcnnPath;
    }

    @Override
    // protected List<PathObject> call() throws Exception {
    protected Void call() throws Exception {
        Project<BufferedImage> project = qupath.getProject();
        if (project != null) {
            // return detectGlomeruliProject(project);
            detectGlomeruliProject(project);
        } else {
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                // return detectGlomeruli(imageData);
                detectGlomeruli(imageData);
            } else {
                logger.error("No image or project is open");
            }
        }
        return null;
    }

    /**
     * Detects glomeruli in the WSI and returns a list of the detected objects
     * 
     * @param imageData
     * @return List of detected objects
     * @throws InterruptedException
     * @throws IOException
     */
    // public List<PathObject> detectGlomeruli(ImageData<BufferedImage> imageData)
    // throws IOException, InterruptedException {
    public void detectGlomeruli(ImageData<BufferedImage> imageData) throws IOException, InterruptedException {
        String imageName = GeneralTools.stripExtension(imageData.getServer().getMetadata().getName());
        VirtualEnvironment venv = new VirtualEnvironment(this.getClass().getSimpleName(), pythonPath, gdcnnPath);

        // If gdcnnPath does not end with a slash, add it
        if (!gdcnnPath.endsWith("/")) {
            gdcnnPath += "/";
        }
        String scriptPath = gdcnnPath + "mescnn/detection/qupath/segment.py";

        double pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSizeMicrons();

        // This is the list of commands after the 'python' call
        List<String> arguments = Arrays.asList(scriptPath, "--wsi", imageName, "--export", gdcnnPath, "--model",
                modelName, "--train-config", trainConfig, "--undersampling", Integer.toString(undersampling),
                "--pixel-size", Double.toString(pixelSize));
        venv.setArguments(arguments);

        // Run the command
        logger.info("Running detection for {}", imageName);
        venv.runCommand();
        logger.info("Detection for {} finished", imageName);
    }

    /**
     * Detects glomeruli in the WSIs in a project and returns a list of the detected
     * objects
     * 
     * @param project
     * @return List of detected objects
     * @throws InterruptedException
     * @throws IOException
     */
    // public List<PathObject> detectGlomeruliProject(Project<BufferedImage>
    // project) throws IOException, InterruptedException {
    public void detectGlomeruliProject(Project<BufferedImage> project) throws IOException, InterruptedException {
        List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
        logger.info("Running detection for {} images", imageEntryList.size());
        for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
            ImageData<BufferedImage> imageData = imageEntry.readImageData();
            detectGlomeruli(imageData);
        }
        logger.info("Detection for the images in the project finished", imageEntryList.size());
    }

}
