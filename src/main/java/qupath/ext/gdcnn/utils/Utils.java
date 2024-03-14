package qupath.ext.gdcnn.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.projects.Project;
import qupath.lib.scripting.QP;

/**
 * Utility class to provide some useful methods
 * 
 * @author Israel Mateos Aparicio
 */
public class Utils {

    /**
     * Deletes a folder and all its content
     * 
     * @param folder
     */
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    /**
     * Creates a folder if it does not exist, otherwise it deletes it and creates
     * 
     * @param path
     */
    public static void createFolder(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        } else {
            deleteFolder(folder);
            folder.mkdirs();
        }
    }

    /**
     * Returns the base directory of the project or the image
     * 
     * @param qupath
     * @return Base directory
     */
    public static String getBaseDir(QuPathGUI qupath) {
        Project<BufferedImage> project = qupath.getProject();
        String baseDir = QP.PROJECT_BASE_DIR;
        if (project == null) {
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                baseDir = Paths.get(imageData.getServer().getPath()).toString();
                // Take substring from the first slash after file: to the last slash
                baseDir = baseDir.substring(baseDir.indexOf("file:") + 5,
                        baseDir.lastIndexOf("/"));
            }
        }
        return baseDir;
    }
}
