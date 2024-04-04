package qupath.ext.gdcnn.entities;

import javafx.scene.image.ImageView;

public class ImageResult {
    
    private ImageView thumbnail;

    private String name;

    private String mostPredictedClass;

    private int nGlomeruli;

    private int noSclerotic;

    private int sclerotic;

    private int noClassified;

    public ImageResult(ImageView thumbnail, String name, String mostPredictedClass, int nGlomeruli, int noSclerotic, int sclerotic, int noClassified) {
        this.thumbnail = thumbnail;
        this.name = name;
        this.mostPredictedClass = mostPredictedClass;
        this.nGlomeruli = nGlomeruli;
        this.noSclerotic = noSclerotic;
        this.sclerotic = sclerotic;
        this.noClassified = noClassified;
    }

    public ImageView getThumbnail() {
        return thumbnail;
    }

    public String getName() {
        return name;
    }

    public String getMostPredictedClass() {
        return mostPredictedClass;
    }

    public int getNGlomeruli() {
        return nGlomeruli;
    }

    public int getNoSclerotic() {
        return noSclerotic;
    }

    public int getSclerotic() {
        return sclerotic;
    }

    public int getNoClassified() {
        return noClassified;
    }
}
