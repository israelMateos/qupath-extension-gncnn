package qupath.ext.gdcnn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.images.ImageData;
import qupath.lib.images.writers.TileExporter;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Class to tile the WSI into the given size patches and save them in a temporary folder
 * 
 * @author Israel Mateos Aparicio
 */
public class Tiler {

    private static final Logger logger = LoggerFactory.getLogger(Tiler.class);

    private int tileSize;

    public Tiler(int tileSize) {
        this.tileSize = tileSize;
    }

    public int getTileSize() {
        return tileSize;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }

    /**
     * Tiles the image data and saves the tiles in the given output path
     * @param imageData
     * @param outputPath
     * @throws IOException 
     */
    public void tileWSI(ImageData<BufferedImage> imageData, String outputPath) throws IOException {
        logger.info("Tiling image data {} into patches of size {}", imageData.getServer().getMetadata().getName(), tileSize);
        new TileExporter(imageData)
            .tileSize(tileSize)
            .imageExtension("png")
            .overlap(tileSize / 2)
            .downsample(0)
            .annotatedTilesOnly(false)
            .writeTiles(outputPath);
        logger.info("Tiling finished. Tiles saved in {}", outputPath);
    }

}
