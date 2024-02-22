package qupath.ext.gdcnn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.images.ImageData;
import qupath.lib.images.writers.TileExporter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Class to tile the WSI into the given size patches and save them in a
 * temporary folder
 * 
 * @author Israel Mateos Aparicio
 */
public class Tiler {

    private static final Logger logger = LoggerFactory.getLogger(Tiler.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int tileSize;

    private int tileOverlap;

    private int downsample;

    private String imageExtension;

    public Tiler(int tileSize, int tileOverlap, int downsample, String imageExtension) {
        this.tileSize = tileSize;
        this.tileOverlap = tileOverlap;
        this.downsample = downsample;
        this.imageExtension = imageExtension;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getTileOverlap() {
        return tileOverlap;
    }

    public int getDownsample() {
        return downsample;
    }

    public String getImageExtension() {
        return imageExtension;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }

    public void setTileOverlap(int tileOverlap) {
        this.tileOverlap = tileOverlap;
    }

    public void setDownsample(int downsample) {
        this.downsample = downsample;
    }

    public void setImageExtension(String imageExtension) {
        this.imageExtension = imageExtension;
    }

    /**
     * Tiles the image data and saves the tiles in the given output path
     * 
     * @param imageData
     * @param outputPath
     * @throws IOException
     */
     public void tileWSIAsync(ImageData<BufferedImage> imageData, String outputPath) {
        executor.submit(() -> {
            try {
                tileWSI(imageData, outputPath);
            } catch (IOException e) {
                logger.error("Error tiling image data", e);
            }
        });
    }

    /**
     * Tiles the image data and saves the tiles in the given output path
     * 
     * @param imageData
     * @param outputPath
     * @throws IOException
     */
    private void tileWSI(ImageData<BufferedImage> imageData, String outputPath) throws IOException {
        logger.info("Tiling image data {} into patches of size {}", imageData.getServer().getMetadata().getName(),
                tileSize);

        new TileExporter(imageData)
                .tileSize(tileSize)
                .imageExtension(imageExtension)
                .overlap(tileOverlap)
                .downsample(downsample)
                .annotatedTilesOnly(false)
        .writeTiles(outputPath);
        logger.info("Tiling finished. Tiles saved in {}", outputPath);
    }

}
