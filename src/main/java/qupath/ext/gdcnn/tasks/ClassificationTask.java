package qupath.ext.gdcnn.tasks;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import qupath.ext.gdcnn.env.VirtualEnvironment;
import qupath.ext.gdcnn.listeners.ProgressListener;
import qupath.ext.gdcnn.utils.Utils;
import qupath.lib.common.ColorTools;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;
import qupath.lib.scripting.QP;

/**
 * Class to classify glomeruli in the WSI patches, and update the detected
 * objects in the image hierarchy
 */
public class ClassificationTask extends Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationTask.class);

    private static final HashMap<String, Integer> CLASS_COLORS = new HashMap<String, Integer>() {
        {
            put("NoSclerotic", ColorTools.GREEN);
            put("Sclerotic", ColorTools.RED);
        }
    };

    private QuPathGUI qupath;

    private List<String> selectedImages;

    private String modelName;

    private ProgressListener progressListener;

    public ClassificationTask(QuPathGUI quPath, List<String> selectedImages, String modelName,
            ProgressListener progressListener) {
        this.qupath = quPath;
        this.selectedImages = selectedImages;
        this.modelName = modelName;
        this.progressListener = progressListener;
    }

    @Override
    protected Void call() throws Exception {
        try {
            Project<BufferedImage> project = qupath.getProject();
            String outputBaseDir = Utils.getBaseDir(qupath);
            if (project != null) {
                runClassification(outputBaseDir);
                classifyGlomeruliProject(project, outputBaseDir);
            } else {
                ImageData<BufferedImage> imageData = qupath.getImageData();
                if (imageData != null) {
                    runClassification(outputBaseDir);
                    classifyGlomeruli(imageData, outputBaseDir);
                } else {
                    logger.error("No image or project is open");
                }
            }

            // The temp folder is not needed anymore
            File tempFolder = new File(QP.buildFilePath(outputBaseDir, TaskPaths.TMP_FOLDER));
            if (tempFolder.exists()) {
                Utils.deleteFolder(tempFolder);
            }
        } catch (IOException e) {
            logger.error("Error with I/O of files: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Runs the classification of glomeruli for all the annotations exported
     * previously
     * 
     * @param outputBaseDir
     * @throws InterruptedException
     * @throws IOException
     */
    private void runClassification(String outputBaseDir)
            throws IOException, InterruptedException {
        VirtualEnvironment venv = new VirtualEnvironment(this.getClass().getSimpleName(), progressListener);

        // This is the list of commands after the 'python' call
        List<String> arguments = Arrays.asList(TaskPaths.CLASSIFICATION_COMMAND, "-e", QP.buildFilePath(outputBaseDir),
                "--netB",
                modelName);
        venv.setArguments(arguments);

        // Check if the thread has been interrupted before starting the process
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        // Run the command
        logger.info("Running classification of glomeruli");
        venv.runCommand();
        logger.info("Classification of glomeruli finished");
    }

    /**
     * Classifies glomeruli in the WSI and updates the detected objects in the
     * image hierarchy
     * 
     * @param imageData
     * @param outputBaseDir
     * @throws InterruptedException
     * @throws IOException
     * @throws NumberFormatException
     */
    public void classifyGlomeruli(ImageData<BufferedImage> imageData, String outputBaseDir)
            throws IOException, InterruptedException, NumberFormatException {
        String imageName = imageData.getServer().getMetadata().getName();
        String reportPath = QP.buildFilePath(outputBaseDir, "Report", "B-swin_transformer_M-None",
                GeneralTools.stripExtension(imageName) + ".csv");

        Collection<PathObject> annotations = imageData.getHierarchy().getAnnotationObjects();
        logger.info("Updating {} annotations for {}", annotations.size(), imageName);

        // Check if the thread has been interrupted before reading the report
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        try (FileReader fileReader = new FileReader(reportPath);
                CSVParser csvParser = new CSVParser(fileReader, CSVFormat.newFormat(';'))) {
            // Skip the header
            csvParser.iterator().next();
            for (CSVRecord record : csvParser) {
                String filename = record.get(0);
                // The annotation ID is after the first underscore after 'Glomerulus'
                // until the next underscore in filename
                String annotationId = filename.substring(filename.indexOf("_", filename.indexOf("Glomerulus")) + 1,
                        filename.indexOf("_", filename.indexOf("Glomerulus") + "Glomerulus".length() + 1));
                logger.info("Updating annotation {}", annotationId);

                PathObject annotation = annotations.stream().filter(a -> a.getID().toString().equals(annotationId))
                        .findFirst().orElse(null);

                if (annotation != null) {
                    String predictedClass = record.get(1);
                    Integer color = CLASS_COLORS.get(predictedClass);
                    PathClass pathClass = PathClass.getInstance(predictedClass, color);

                    annotation.setPathClass(pathClass);
                }

                // Check if the thread has been interrupted before updating the
                // next annotation
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        }
        // Update progress
        progressListener.updateProgress();
    }

    /**
     * Classifies glomeruli in the WSIs in a project and updates the detected
     * objects in each image hierarchy
     * 
     * @param project
     * @param outputBaseDir
     * @throws InterruptedException
     * @throws IOException
     * @throws NumberFormatException
     */
    private void classifyGlomeruliProject(Project<BufferedImage> project, String outputBaseDir)
            throws IOException, InterruptedException, NumberFormatException {
        List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
        logger.info("Running classification for {} images", selectedImages.size());
        // Only process the selected images
        for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
            ImageData<BufferedImage> imageData = imageEntry.readImageData();
            if (selectedImages.contains(GeneralTools.stripExtension(imageData.getServer().getMetadata().getName()))) {
                classifyGlomeruli(imageData, outputBaseDir);
                imageEntry.saveImageData(imageData);
            }
        }
        logger.info("Classification for {} images in the project finished", selectedImages.size());
    }

}
