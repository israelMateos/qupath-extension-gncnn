package qupath.ext.gdcnn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import javafx.concurrent.Task;

import java.util.List;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.common.GeneralTools;
import qupath.lib.images.ImageData;
import qupath.lib.images.writers.TileExporter;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;

/**
 * Class to tile the WSI into the given size patches and save them in a
 * temporary folder
 * 
 * @author Israel Mateos Aparicio
 */
public class TilerTask extends Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger(TilerTask.class);

    private QuPathGUI qupath;

    private int tileSize;

    private int tileOverlap;

    private int downsample;

    private String imageExtension;

    public TilerTask(QuPathGUI quPath, int tileSize, int tileOverlap, int downsample, String imageExtension) {
        this.qupath = quPath;
        this.tileSize = tileSize;
        this.tileOverlap = tileOverlap;
        this.downsample = downsample;
        this.imageExtension = imageExtension;
    }

    @Override
    protected Void call() throws Exception {
        Project<BufferedImage> project = qupath.getProject();
        List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
        int nImages = imageEntryList.size();
        int i = 1;

        logger.info("Tiling {} images in the project to patches of size {}", nImages, tileSize);
        // For each image, tile it and save the tiles in a temporary folder
        for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
            ImageData<BufferedImage> imageData = imageEntry.readImageData();
            logger.info("Tiling image {}/{} : {}", i, nImages, imageData.getServer().getMetadata().getName());
            String outputPath = GeneralTools.stripExtension(imageData.getServer().getMetadata().getName()) + "_tiles";
            // Create the output folder if it does not exist
            Files.createDirectories(Paths.get(outputPath));
            tileWSI(imageData, outputPath);
            logger.info("Finished tiling image {}/{} : {}", i, nImages, outputPath);
            i++;
        }

        logger.info("Tiling images in the project finished");
        return null;
    }

    /**
     * Tiles the image data and saves the tiles in the given output path
     * 
     * @param imageData
     * @param outputPath
     * @throws IOException
     */
    private void tileWSI(ImageData<BufferedImage> imageData, String outputPath) throws IOException {
        new TileExporter(imageData)
                .tileSize(tileSize)
                .imageExtension(imageExtension)
                .overlap(tileOverlap)
                .downsample(downsample)
                .annotatedTilesOnly(false)
                .writeTiles(outputPath);
    }

}
