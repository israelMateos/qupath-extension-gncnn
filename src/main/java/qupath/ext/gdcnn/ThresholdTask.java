package qupath.ext.gdcnn;

import java.awt.image.BufferedImage;
import java.io.File;
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
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.writers.ImageWriterTools;
import qupath.lib.io.PathIO;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;
import qupath.lib.regions.RegionRequest;
import qupath.lib.scripting.QP;

/**
 * Class to process the images using a thresholding method in order to separate
 * foreground from background
 */
public class ThresholdTask extends Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdTask.class);

    private QuPathGUI qupath;

    private int downsample;

    private String imageExtension;

    private String pythonPath;

    private String gdcnnPath;

    public ThresholdTask(QuPathGUI quPath, int downsample, String imageExtension, String pythonPath, String gdcnnPath) {
        this.qupath = quPath;
        this.downsample = downsample;
        this.imageExtension = imageExtension;
        this.pythonPath = pythonPath;
        this.gdcnnPath = gdcnnPath;
    }

    @Override
    protected Void call() throws Exception {
        Project<BufferedImage> project = qupath.getProject();
        String outputBaseDir = QP.PROJECT_BASE_DIR;
        if (project != null) {
            thresholdForegroundProject(project, outputBaseDir);
        } else {
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                outputBaseDir = Paths.get(imageData.getServer().getPath()).toString();
                // Take substring from the first slash after file: to the last slash
                outputBaseDir = outputBaseDir.substring(outputBaseDir.indexOf("file:") + 5,
                        outputBaseDir.lastIndexOf("/"));
                thresholdForeground(imageData, outputBaseDir);
            } else {
                logger.error("No image or project is open");
            }
        }

        // Low-resolution images are not needed anymore
        File lowresOutputFolder = new File(
                QP.buildFilePath(outputBaseDir, TaskPaths.TMP_FOLDER, TaskPaths.LOWRES_OUTPUT_FOLDER));
        if (lowresOutputFolder.exists())
            Utils.deleteFolder(lowresOutputFolder);

        // Tissue detections are already added to the image hierarchy, so they
        // are not needed
        File thresholdOutputFolder = new File(
                QP.buildFilePath(outputBaseDir, TaskPaths.TMP_FOLDER, TaskPaths.THRESHOLD_OUTPUT_FOLDER));
        if (thresholdOutputFolder.exists())
            Utils.deleteFolder(thresholdOutputFolder);

        return null;
    }

    public void exportLowResolutionImage(ImageData<BufferedImage> imageData, String outputBaseDir) throws IOException {
        ImageServer<BufferedImage> server = imageData.getServer();
        String imageName = GeneralTools.stripExtension(server.getMetadata().getName());
        String outputPath = TaskPaths.getLowResOutputDir(outputBaseDir, imageName);
        // Create the output folder if it does not exist
        Utils.createFolder(outputPath);

        logger.info("Exporting low-res {} [downsample={}]", imageName, downsample);
        RegionRequest request = RegionRequest.createInstance(server, downsample);
        outputPath += "/" + imageName + imageExtension;
        ImageWriterTools.writeImageRegion(server, request, outputPath);
        logger.info("Low-res exporting of {} finished: {}", imageName, outputPath);
    }

    /**
     * Applies the thresholding algorithm to the image, separating the
     * foreground from the background, and adds the foreground objects as
     * annotations to the image hierarchy
     * 
     * @param imageData
     * @param outputBaseDir
     * @throws InterruptedException
     * @throws IOException
     */
    public void thresholdForeground(ImageData<BufferedImage> imageData, String outputBaseDir)
            throws IOException, InterruptedException {
        exportLowResolutionImage(imageData, outputBaseDir);

        String imageName = GeneralTools.stripExtension(imageData.getServer().getMetadata().getName());
        VirtualEnvironment venv = new VirtualEnvironment(this.getClass().getSimpleName(), pythonPath, gdcnnPath);

        String scriptPath = TaskPaths.getThresholdScriptPath(gdcnnPath);

        double pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSizeMicrons();

        // This is the list of commands after the 'python' call
        List<String> arguments = Arrays.asList(scriptPath, "--wsi", imageName, "--export",
                QP.buildFilePath(outputBaseDir), "--undersampling", Integer.toString(downsample), "--pixel-size",
                Double.toString(pixelSize));
        venv.setArguments(arguments);

        // Run the command
        logger.info("Running thresholding algorithm for {}", imageName);
        venv.runCommand();
        logger.info("Thresholding algorithm for {} finished", imageName);

        // Read the annotations from the GeoJSON file
        String geoJSONPath = TaskPaths.getThresholdResultsPath(outputBaseDir, imageName);
        List<PathObject> detectedObjects = PathIO.readObjects(Paths.get(geoJSONPath));

        // Add the detected objects to the image hierarchy
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        hierarchy.addObjects(detectedObjects);
        logger.info("Added {} detected objects to {}", detectedObjects.size(), imageName);
    }

    /**
     * Applies the thresholding algorithm to each image in the project, separating
     * the foreground from the background, and adds the foreground objects as
     * annotations to each image hierarchy
     * 
     * @param project
     * @param outputBaseDir
     * @throws InterruptedException
     * @throws IOException
     */
    public void thresholdForegroundProject(Project<BufferedImage> project, String outputBaseDir)
            throws IOException, InterruptedException {
        List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
        logger.info("Running detection for {} images", imageEntryList.size());
        for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
            ImageData<BufferedImage> imageData = imageEntry.readImageData();
            thresholdForeground(imageData, outputBaseDir);
            imageEntry.saveImageData(imageData);
        }
        logger.info("Detection for {} images in the project finished", imageEntryList.size());
    }

}
