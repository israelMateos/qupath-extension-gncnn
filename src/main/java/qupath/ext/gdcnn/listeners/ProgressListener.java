package qupath.ext.gdcnn.listeners;

import javafx.beans.property.SimpleDoubleProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to listen to the progress of the tasks and update the progress
 * indicator in the main JavaFX thread
 */
public class ProgressListener {

    private static final Logger logger = LoggerFactory.getLogger(ProgressListener.class);
    
    private SimpleDoubleProperty progress = new SimpleDoubleProperty(0.0);

    private double progressStep;

    public ProgressListener(double progressStep) {
        this.progressStep = progressStep;
    }

    public void updateProgress() {
        progress.set(progress.get() + progressStep);
        logger.info("Progress: " + progress.get());
    }

    public void updateProgress(double currentProgress, double partialProgressStep) {
        progress.set(currentProgress + progressStep * partialProgressStep);
        logger.info("Progress: " + progress.get());
    }

    public double getProgress() {
        return progress.get();
    }

    public SimpleDoubleProperty progressProperty() {
        return progress;
    }
}
