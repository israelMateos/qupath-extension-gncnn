package qupath.ext.gdcnn;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import qupath.ext.env.VirtualEnvironment;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.io.PathIO;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;
import qupath.lib.scripting.QP;

/**
 * Class to detect glomeruli in the WSI patches, and add the detected objects to
 * the image hierarchy
 */
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
    protected Void call() throws Exception {
        Project<BufferedImage> project = qupath.getProject();
        if (project != null) {
            detectGlomeruliProject(project);
        } else {
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                String outputBaseDir = Paths.get(imageData.getServer().getPath()).toString();
                // Take substring from the first slash after file: to the last slash
                outputBaseDir = outputBaseDir.substring(outputBaseDir.indexOf("file:") + 5, outputBaseDir.lastIndexOf("/"));
                detectGlomeruli(imageData, outputBaseDir);
            } else {
                logger.error("No image or project is open");
            }
        }
        return null;
    }

    /**
     * Detects glomeruli in the WSI and adds the detected objects to the image
     * hierarchy
     * 
     * @param imageData
     * @param outputBaseDir
     * @throws InterruptedException
     * @throws IOException
     */
    public void detectGlomeruli(ImageData<BufferedImage> imageData, String outputBaseDir)
            throws IOException, InterruptedException {
        String imageName = GeneralTools.stripExtension(imageData.getServer().getMetadata().getName());
        VirtualEnvironment venv = new VirtualEnvironment(this.getClass().getSimpleName(), pythonPath, gdcnnPath);

        String scriptPath = QP.buildFilePath(gdcnnPath, "mescnn", "detection", "qupath", "segment.py");

        double pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSizeMicrons();

        // This is the list of commands after the 'python' call
        List<String> arguments = Arrays.asList(scriptPath, "--wsi", imageName, "--export",
                QP.buildFilePath(outputBaseDir),
                "--model",
                modelName, "--train-config", trainConfig, "--undersampling", Integer.toString(undersampling),
                "--pixel-size", Double.toString(pixelSize));
        venv.setArguments(arguments);

        // Run the command
        logger.info("Running detection for {}", imageName);
        venv.runCommand();
        logger.info("Detection for {} finished", imageName);

        // Read the annotations from the GeoJSON file
        String geoJSONPath = QP.buildFilePath(outputBaseDir, "Temp", "segment-output", "Detections", imageName,
                "detections.geojson");
        List<PathObject> detectedObjects = PathIO.readObjects(Paths.get(geoJSONPath));

        // Add the detected objects to the image hierarchy
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        hierarchy.addObjects(detectedObjects);
        logger.info("Added {} detected objects to {}", detectedObjects.size(), imageName);
    }

    /**
     * Detects glomeruli in the WSIs in a project and adds the detected objects to
     * the each image hierarchy
     * 
     * @param project
     * @throws InterruptedException
     * @throws IOException
     */
    public void detectGlomeruliProject(Project<BufferedImage> project) throws IOException, InterruptedException {
        List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
        logger.info("Running detection for {} images", imageEntryList.size());
        for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
            ImageData<BufferedImage> imageData = imageEntry.readImageData();
            detectGlomeruli(imageData, QP.PROJECT_BASE_DIR);
            imageEntry.saveImageData(imageData);
        }
        logger.info("Detection for {} images in the project finished", imageEntryList.size());
    }

}
