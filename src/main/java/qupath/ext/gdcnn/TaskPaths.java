package qupath.ext.gdcnn;

import qupath.lib.common.GeneralTools;
import qupath.lib.scripting.QP;

/**
 * Class to store the paths used in the different tasks
 */
public class TaskPaths {

    public static final String TMP_FOLDER = "Temp";

    public static final String TILER_OUTPUT_FOLDER = "tiler-output";
    public static final String ANN_EXPORT_OUTPUT_FOLDER = "ann-export-output";
    public static final String SEGMENT_OUTPUT_FOLDER = "segment-output";

    private static final String TILES_FOLDER = "Tiles";
    private static final String DETECTIONS_FOLDER = "Detections";
    private static final String REPORT_FOLDER = "Report";
    private static final String MODEL_FOLDER = "B-swin_transformer_M-None";

    /**
     * Returns the path to the folder where the tiles are stored
     * 
     * @param baseDir
     * @param imageName
     * @return Path to the folder where the tiles are stored
     */
    public static String getTilerOutputDir(String baseDir, String imageName) {
        return QP.buildFilePath(baseDir, TMP_FOLDER, TILER_OUTPUT_FOLDER, TILES_FOLDER, imageName);
    }

    /**
     * Returns the path to the folder where the annotations are stored
     * 
     * @param baseDir
     * @param imageName
     * @return Path to the folder where the annotations are stored
     */
    public static String getAnnotationOutputDir(String baseDir, String imageName) {
        return QP.buildFilePath(baseDir, TMP_FOLDER, ANN_EXPORT_OUTPUT_FOLDER, imageName);
    }

    /**
     * Returns the detection script path
     * 
     * @param gdcnnPath
     * @return Path to the detection script
     */
    public static String getDetectionScriptPath(String gdcnnPath) {
        return QP.buildFilePath(gdcnnPath, "mescnn", "detection", "qupath", "segment.py");
    }

    /**
     * Returns the path to the detection results
     * 
     * @param baseDir
     * @param imageName
     * @return Path to the detection results
     */
    public static String getDetectionResultsPath(String baseDir, String imageName) {
        return QP.buildFilePath(baseDir, TMP_FOLDER, SEGMENT_OUTPUT_FOLDER, DETECTIONS_FOLDER, imageName,
                "detections.geojson");
    }

    /**
     * Returns the classification script path
     * 
     * @param gdcnnPath
     * @return Path to the classification script
     */
    public static String getClassificationScriptPath(String gdcnnPath) {
        return QP.buildFilePath(gdcnnPath, "mescnn", "classification", "inference", "classify.py");
    }

    /**
     * Returns the path to the classification results
     * 
     * @param baseDir
     * @param imageName
     * @return Path to the classification results
     */
    public static String getClassificationResultsPath(String baseDir, String imageName) {
        return QP.buildFilePath(TMP_FOLDER, REPORT_FOLDER, MODEL_FOLDER,
                GeneralTools.stripExtension(imageName) + ".csv");
    }
}