package qupath.ext.gdcnn;

/**
 * Singleton class to store the path to the python executable
 * 
 * @author Israel Mateos Aparicio
 */
public class GDCnnSetup {
    private static final GDCnnSetup instance = new GDCnnSetup();
    private String pythonPath = null;
    private String gdcnnPath = null;

    public static GDCnnSetup getInstance() {
        return instance;
    }

    public String getPythonPath() {
        return pythonPath;
    }

    public void setPythonPath(String path) {
        this.pythonPath = path;
    }

    public String getGdcnnPath() {
        return gdcnnPath;
    }

    public void setGdcnnPath(String path) {
        this.gdcnnPath = path;
    }
}