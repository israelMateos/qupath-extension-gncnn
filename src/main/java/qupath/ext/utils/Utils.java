package qupath.ext.utils;

import java.io.File;

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
}
