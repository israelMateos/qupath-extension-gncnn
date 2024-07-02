/**
 * Copyright (C) 2024 Israel Mateos-Aparicio-Ruiz
 */
package qupath.ext.gdcnn.entities;

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
        double newProgress = currentProgress + progressStep * partialProgressStep;
        // If the new progress rounds to 1.0, set it to 0.99 to avoid the
        // progress indicator to be full
        // This fixes a bug when detecting glomeruli and not classifying them
        if (newProgress >= 0.994) {
            progress.set(0.99);
        } else {
            progress.set(newProgress);
        }
        logger.info("Progress: " + progress.get());
    }

    public double getProgress() {
        return progress.get();
    }

    public SimpleDoubleProperty progressProperty() {
        return progress;
    }
}
