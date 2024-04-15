package qupath.ext.gdcnn.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import qupath.ext.gdcnn.entities.ImageResult;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;
import qupath.lib.regions.RegionRequest;
import qupath.lib.scripting.QP;

/**
 * Utility class to provide some useful methods
 * 
 * @author Israel Mateos Aparicio
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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

    /**
     * Checks if there are "Glomerulus" annotations in the selected images, and
     * returns the images with "Glomerulus" annotations
     * 
     * @param qupath
     * @param selectedImages
     * @return The images with "Glomerulus" annotations
     * @throws IOException
     */
    public static List<String> getImgsWithGlomeruli(QuPathGUI qupath, ObservableList<String> selectedImages)
            throws IOException {
        List<String> imgsWithGlomeruli = new ArrayList<String>();

        Project<BufferedImage> project = qupath.getProject();
        if (project != null) {
            // Check for glomerulus annotations in the selected images
            List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
            for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
                String imageName = GeneralTools.stripExtension(imageEntry.getImageName());
                if (selectedImages.contains(imageName)) {
                    ImageData<BufferedImage> imageData = imageEntry.readImageData();
                    PathObjectHierarchy hierarchy = imageData.getHierarchy();
                    Collection<PathObject> annotations = hierarchy.getAnnotationObjects();

                    for (PathObject annotation : annotations) {
                        if (annotation.getPathClass() != null
                                && annotation.getPathClass().getName().equals("Glomerulus")) {
                            imgsWithGlomeruli.add(imageName);
                            break;
                        }
                    }
                }
            }
        } else {
            // Check for glomerulus annotations in the current image
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                String imageName = GeneralTools.stripExtension(imageData.getServer().getMetadata().getName());
                PathObjectHierarchy hierarchy = imageData.getHierarchy();
                Collection<PathObject> annotations = hierarchy.getAnnotationObjects();

                for (PathObject annotation : annotations) {
                    if (annotation.getPathClass() != null
                            && annotation.getPathClass().getName().equals("Glomerulus")) {
                        imgsWithGlomeruli.add(imageName);
                        break;
                    }
                }
            } else {
                logger.error("No project or image is open");
            }
        }

        return imgsWithGlomeruli;
    }

    /**
     * Returns a thumbnail of the image
     * 
     * @param imageData
     * @param desiredSize
     * @return The thumbnail of the image
     * @throws IOException
     */
    private static BufferedImage getThumbnail(ImageData<BufferedImage> imageData, double desiredSize)
            throws IOException {
        ImageServer<BufferedImage> server = imageData.getServer();

        int width = server.getWidth();
        int height = server.getHeight();

        // Calculate downsample factor depending on the desired size
        double downsample = Math.max(width / desiredSize, height / desiredSize);

        RegionRequest request = RegionRequest.createInstance(imageData.getServerPath(), downsample, 0, 0, width,
                height);

        BufferedImage img = server.readRegion(request);

        return img;
    }

    /**
     * Returns the results of the detection and classification of the glomeruli,
     * including all images in the project (or the current image if no project
     * exists)
     * 
     * @param qupath
     * @param selectedImages
     * @return The results of the detection and classification of the glomeruli
     * @throws IOException
     */
    public static ObservableList<ImageResult> getResults(QuPathGUI qupath, ObservableList<String> selectedImages)
            throws IOException {
        ObservableList<ImageResult> results = FXCollections.observableArrayList();

        Project<BufferedImage> project = qupath.getProject();
        if (project != null) {
            // Check for glomerulus annotations in the selected images
            List<ProjectImageEntry<BufferedImage>> imageEntryList = project.getImageList();
            for (ProjectImageEntry<BufferedImage> imageEntry : imageEntryList) {
                String imageName = GeneralTools.stripExtension(imageEntry.getImageName());
                if (selectedImages.contains(imageName)) {
                    ImageData<BufferedImage> imageData = imageEntry.readImageData();
                    PathObjectHierarchy hierarchy = imageData.getHierarchy();
                    Collection<PathObject> annotations = hierarchy.getAnnotationObjects();

                    int nGlomeruli = 0;
                    HashMap<String, Integer> diseaseCounts = new HashMap<String, Integer>() {
                        {
                            put("Non-sclerotic", 0);
                            put("Sclerotic", 0);
                            put("ABMGN", 0);
                            put("ANCA", 0);
                            put("C3-GN", 0);
                            put("CryoglobulinemicGN", 0);
                            put("DDD", 0);
                            put("Fibrillary", 0);
                            put("IAGN", 0);
                            put("IgAN", 0);
                            put("MPGN", 0);
                            put("Membranous", 0);
                            put("PGNMID", 0);
                            put("SLEGN-IV", 0);
                            put("Non-classified", 0);
                        }
                    };

                    for (PathObject annotation : annotations) {
                        PathClass pathClass = annotation.getPathClass();
                        if (pathClass != null) {
                            String className = pathClass.getName();
                            // Adapt the class names to the ones in the report
                            className = className.replace("Glomerulus", "Non-classified");
                            className = className.replace("NoSclerotic", "Non-sclerotic");
                            diseaseCounts.put(className, diseaseCounts.get(className) + 1);
                            nGlomeruli++;
                        }
                    }

                    String mostPredictedClass = "";
                    int maxCount = 0;
                    for (Map.Entry<String, Integer> entry : diseaseCounts.entrySet()) {
                        if (entry.getValue() > maxCount) {
                            mostPredictedClass = entry.getKey();
                            maxCount = entry.getValue();
                        }
                    }

                    ImageView thumbnail = new ImageView(SwingFXUtils.toFXImage(getThumbnail(imageData, 200), null));
                    results.add(new ImageResult(thumbnail, imageName, mostPredictedClass, nGlomeruli, diseaseCounts));
                }
            }
        } else {
            // Check for glomerulus annotations in the current image
            ImageData<BufferedImage> imageData = qupath.getImageData();
            if (imageData != null) {
                String imageName = GeneralTools.stripExtension(imageData.getServer().getMetadata().getName());
                PathObjectHierarchy hierarchy = imageData.getHierarchy();
                Collection<PathObject> annotations = hierarchy.getAnnotationObjects();

                int nGlomeruli = 0;
                HashMap<String, Integer> diseaseCounts = new HashMap<String, Integer>() {
                    {
                        put("Non-sclerotic", 0);
                        put("Sclerotic", 0);
                        put("ABMGN", 0);
                        put("ANCA", 0);
                        put("C3-GN", 0);
                        put("CryoglobulinemicGN", 0);
                        put("DDD", 0);
                        put("Fibrillary", 0);
                        put("IAGN", 0);
                        put("IgAN", 0);
                        put("MPGN", 0);
                        put("Membranous", 0);
                        put("PGNMID", 0);
                        put("SLEGN-IV", 0);
                        put("Non-classified", 0);
                    }
                };

                for (PathObject annotation : annotations) {
                    PathClass pathClass = annotation.getPathClass();
                    if (pathClass != null) {
                        String className = pathClass.getName();
                        // Adapt the class names to the ones in the report
                        className = className.replace("Glomerulus", "Non-classified");
                        className = className.replace("NoSclerotic", "Non-sclerotic");
                        diseaseCounts.put(className, diseaseCounts.get(className) + 1);
                        nGlomeruli++;
                    }
                }

                String mostPredictedClass = "";
                int maxCount = 0;
                for (Map.Entry<String, Integer> entry : diseaseCounts.entrySet()) {
                    if (entry.getValue() > maxCount) {
                        mostPredictedClass = entry.getKey();
                        maxCount = entry.getValue();
                    }
                }

                ImageView thumbnail = new ImageView(SwingFXUtils.toFXImage(getThumbnail(imageData, 200), null));
                results.add(new ImageResult(thumbnail, imageName, mostPredictedClass, nGlomeruli, diseaseCounts));
            } else {
                logger.error("No project or image is open");
            }
        }

        return results;
    }

    /**
     * Filters a list of strings by a given filter
     * 
     * @param list
     * @param filter
     * @return The filtered list
     */
    public static ObservableList<String> filterList(ObservableList<String> list, String filter) {
        ObservableList<String> filteredList = FXCollections.observableArrayList();
        for (String item : list) {
            if (item.toLowerCase().contains(filter.toLowerCase())) {
                filteredList.add(item);
            }
        }
        return filteredList;
    }
}
