package qupath.ext.gdcnn.entities;

import java.awt.image.BufferedImage;

public class ImageResult {
    
    private BufferedImage thumbnail;

    private String name;

    private String mostPredictedClass;

    private int nGlomeruli;

    private int noSclerotic;

    private int sclerotic;

    private int noClassified;

    public ImageResult(BufferedImage thumbnail, String name, String mostPredictedClass, int nGlomeruli, int noSclerotic, int sclerotic, int noClassified) {
        this.thumbnail = thumbnail;
        this.name = name;
        this.mostPredictedClass = mostPredictedClass;
        this.nGlomeruli = nGlomeruli;
        this.noSclerotic = noSclerotic;
        this.sclerotic = sclerotic;
        this.noClassified = noClassified;
    }

    public BufferedImage getThumbnail() {
        return thumbnail;
    }

    public String getName() {
        return name;
    }

    public String getMostPredictedClass() {
        return mostPredictedClass;
    }

    public int getnGlomeruli() {
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

    public void setThumbnail(BufferedImage thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMostPredictedClass(String mostPredictedClass) {
        this.mostPredictedClass = mostPredictedClass;
    }

    public void setnGlomeruli(int nGlomeruli) {
        this.nGlomeruli = nGlomeruli;
    }

    public void setNoSclerotic(int noSclerotic) {
        this.noSclerotic = noSclerotic;
    }

    public void setSclerotic(int sclerotic) {
        this.sclerotic = sclerotic;
    }

    public void setNoClassified(int noClassified) {
        this.noClassified = noClassified;
    }
}
