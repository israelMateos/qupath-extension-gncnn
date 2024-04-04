package qupath.ext.gdcnn.ui;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import qupath.ext.gdcnn.entities.ImageResult;

/**
 * Controller for the results pane
 */
public class ResultsController {

    private static final Logger logger = LoggerFactory.getLogger(ResultsController.class);

    @FXML
    private TableView<ImageResult> resultsTable;
    @FXML
    private TableColumn<ImageResult, ImageView> thumbnailCol;
    @FXML
    private TableColumn<ImageResult, String> imageCol;
    @FXML
    private TableColumn<ImageResult, String> mostPredictedClassCol;
    @FXML
    private TableColumn<ImageResult, Integer> nGlomeruliCol;
    @FXML
    private TableColumn<ImageResult, Integer> noScleroticCol;
    @FXML
    private TableColumn<ImageResult, Integer> scleroticCol;
    @FXML
    private TableColumn<ImageResult, Integer> noClassifiedCol;

    @FXML
    private Button saveBtn;

    @FXML
    /**
     * Initializes the controller
     */
    private void initialize() {
        logger.info("Initializing...");

        bindValueFactories();
    }

    /**
     * Bind the value factories to the table columns
     */
    private void bindValueFactories() {
        // Bindings
        PropertyValueFactory<ImageResult, ImageView> thumbnailColFactory = new PropertyValueFactory<>("thumbnail");
        PropertyValueFactory<ImageResult, String> imageColFactory = new PropertyValueFactory<>("name");
        PropertyValueFactory<ImageResult, String> mostPredictedClassColFactory = new PropertyValueFactory<>(
                "mostPredictedClass");
        PropertyValueFactory<ImageResult, Integer> nGlomeruliColFactory = new PropertyValueFactory<>("nGlomeruli");
        PropertyValueFactory<ImageResult, Integer> noScleroticColFactory = new PropertyValueFactory<>("noSclerotic");
        PropertyValueFactory<ImageResult, Integer> scleroticColFactory = new PropertyValueFactory<>("sclerotic");
        PropertyValueFactory<ImageResult, Integer> noClassifiedColFactory = new PropertyValueFactory<>("noClassified");

        // Set value factories
        thumbnailCol.setCellValueFactory(thumbnailColFactory);
        imageCol.setCellValueFactory(imageColFactory);
        mostPredictedClassCol.setCellValueFactory(mostPredictedClassColFactory);
        nGlomeruliCol.setCellValueFactory(nGlomeruliColFactory);
        noScleroticCol.setCellValueFactory(noScleroticColFactory);
        scleroticCol.setCellValueFactory(scleroticColFactory);
        noClassifiedCol.setCellValueFactory(noClassifiedColFactory);
    }

    /**
     * Fills the table with the results
     * 
     * @param results
     */
    public void fillTable(ObservableList<ImageResult> results) {
        resultsTable.setItems(results);
    }
}
