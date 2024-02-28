package qupath.ext.gdcnn;

import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javafx.concurrent.Task;
import qupath.ext.env.VirtualEnvironment;
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
 * objects
 * in the image hierarchy
 */
public class ClassificationTask extends Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationTask.class);

    private QuPathGUI qupath;

    private String modelName;

    private String pythonPath;

    private String gdcnnPath;

    public ClassificationTask(QuPathGUI quPath, String modelName, String pythonPath,
            String gdcnnPath) {
        this.qupath = quPath;
        this.modelName = modelName;
        this.pythonPath = pythonPath;
        this.gdcnnPath = gdcnnPath;
    }

    @Override
    protected Void call() throws Exception {
        Project<BufferedImage> project = qupath.getProject();
        if (project != null) {
            runClassification();
            classifyGlomeruliProject(project);
        } else {
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                runClassification();
                classifyGlomeruli(imageData);
            } else {
                logger.error("No image or project is open");
            }
        }
        return null;
    }

    /**
     * Runs the classification of glomeruli for all the annotations exported
     * previously
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    public void runClassification()
            throws IOException, InterruptedException {
        VirtualEnvironment venv = new VirtualEnvironment(this.getClass().getSimpleName(), pythonPath, gdcnnPath);

        String scriptPath = QP.buildFilePath(gdcnnPath, "mescnn", "classification", "inference", "classify.py");

        // This is the list of commands after the 'python' call
        List<String> arguments = Arrays.asList(scriptPath, "-e", QP.buildFilePath(QP.PROJECT_BASE_DIR), "--netB",
                modelName);
        venv.setArguments(arguments);

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
     * @throws InterruptedException
     * @throws IOException
     * @throws NumberFormatException
     */
    public void classifyGlomeruli(ImageData<BufferedImage> imageData)
            throws IOException, InterruptedException, NumberFormatException {
        String imageName = imageData.getServer().getMetadata().getName();
        String reportPath = QP.buildFilePath(QP.PROJECT_BASE_DIR, "Report", "B-swin_transformer_M-None",
                GeneralTools.stripExtension(imageName) + ".csv");

        Collection<PathObject> annotations = imageData.getHierarchy().getAnnotationObjects();
        logger.info("Updating {} annotations for {}", annotations.size(), imageName);

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
                    double classProbability = Double.parseDouble(record.get(2));
                    PathClass pathClass = PathClass.getInstance(predictedClass);

                    annotation.setPathClass(pathClass, classProbability);
                }
            }
        }
    }

    /**
     * Classifies glomeruli in the WSIs in a project and updates the detected
     * objects in each image hierarchy
     * 
     * @param project
     * @throws InterruptedException
     * @throws IOException
     * @throws NumberFormatException
     */
    public void classifyGlomeruliProject(Project<BufferedImage> project)
            throws IOException, InterruptedException, NumberFormatException {
        List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
        logger.info("Running classification for {} images", imageEntryList.size());
        for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
            ImageData<BufferedImage> imageData = imageEntry.readImageData();
            classifyGlomeruli(imageData);
            imageEntry.saveImageData(imageData);
        }
        logger.info("Classification for {} images in the project finished", imageEntryList.size());
    }

}
