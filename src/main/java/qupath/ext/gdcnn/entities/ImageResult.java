/**
 * Copyright (C) 2024 Israel Mateos-Aparicio-Ruiz
 */
package qupath.ext.gdcnn.entities;

import java.util.HashMap;

import javafx.scene.image.ImageView;

public class ImageResult {

    private ImageView thumbnail;

    private String name;

    private String mostPredictedClass;

    private int nGlomeruli;

    private HashMap<String, Integer> diseaseCounts;

    public ImageResult(ImageView thumbnail, String name, String mostPredictedClass, int nGlomeruli,
            HashMap<String, Integer> diseaseCounts) {
        this.thumbnail = thumbnail;
        this.name = name;
        this.mostPredictedClass = mostPredictedClass;
        this.nGlomeruli = nGlomeruli;
        this.diseaseCounts = diseaseCounts;
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
        return diseaseCounts.getOrDefault("Non-sclerotic", 0);
    }

    public int getSclerotic() {
        return diseaseCounts.getOrDefault("Sclerotic", 0);
    }

    public int getABMGN() {
        return diseaseCounts.getOrDefault("ABMGN", 0);
    }

    public int getANCA() {
        return diseaseCounts.getOrDefault("ANCA", 0);
    }

    public int getC3GN() {
        return diseaseCounts.getOrDefault("C3-GN", 0);
    }

    public int getCryoglobulinemicGN() {
        return diseaseCounts.getOrDefault("CryoglobulinemicGN", 0);
    }

    public int getDDD() {
        return diseaseCounts.getOrDefault("DDD", 0);
    }

    public int getFibrillary() {
        return diseaseCounts.getOrDefault("Fibrillary", 0);
    }

    public int getIAGN() {
        return diseaseCounts.getOrDefault("IAGN", 0);
    }

    public int getIgAGN() {
        return diseaseCounts.getOrDefault("IgAGN", 0);
    }

    public int getMPGN() {
        return diseaseCounts.getOrDefault("MPGN", 0);
    }

    public int getMembranous() {
        return diseaseCounts.getOrDefault("Membranous", 0);
    }

    public int getPGNMID() {
        return diseaseCounts.getOrDefault("PGNMID", 0);
    }

    public int getSLEGNIV() {
        return diseaseCounts.getOrDefault("SLEGN-IV", 0);
    }

    public int getNoClassified() {
        return diseaseCounts.getOrDefault("Non-classified", 0);
    }

    /*
     * Returns a CSV row with the results, separated by ';'
     * 
     * @return CSV row corresponding to the results
     */
    public String toCSVRow() {
        return name + ";" + mostPredictedClass + ";" + nGlomeruli + ";" + getNoSclerotic() + ";" + getSclerotic() + ";"
                + getABMGN() + ";" + getANCA() + ";" + getC3GN() + ";" + getCryoglobulinemicGN() + ";" + getDDD() + ";"
                + getFibrillary() + ";" + getIAGN() + ";" + getIgAGN() + ";" + getMPGN() + ";" + getMembranous() + ";"
                + getPGNMID() + ";" + getSLEGNIV() + ";" + getNoClassified();
    }

    /**
     * Returns the header for the CSV file
     * 
     * @return CSV header
     */
    public static String getCSVHeader() {
        return "Image;Most predicted class;Number of glomeruli;Non-sclerotic;Sclerotic;ABMGN;ANCA;C3-GN;CryoglobulinemicGN;DDD;Fibrillary;IAGN;IgAGN;MPGN;Membranous;PGNMID;SLEGN-IV;Non-classified";
    }
}
