package qupath.ext.gdcnn.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import qupath.ext.gdcnn.entities.ImageResult;
import qupath.ext.gdcnn.utils.Utils;

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
    private TableColumn<ImageResult, Integer> ABMGNCol;
    @FXML
    private TableColumn<ImageResult, Integer> ANCACol;
    @FXML
    private TableColumn<ImageResult, Integer> C3GNCol;
    @FXML
    private TableColumn<ImageResult, Integer> CryoglobulinemicGNCol;
    @FXML
    private TableColumn<ImageResult, Integer> DDDCol;
    @FXML
    private TableColumn<ImageResult, Integer> FibrillaryCol;
    @FXML
    private TableColumn<ImageResult, Integer> IAGNCol;
    @FXML
    private TableColumn<ImageResult, Integer> IgANCol;
    @FXML
    private TableColumn<ImageResult, Integer> MPGNCol;
    @FXML
    private TableColumn<ImageResult, Integer> MembranousCol;
    @FXML
    private TableColumn<ImageResult, Integer> PGNMIDCol;
    @FXML
    private TableColumn<ImageResult, Integer> SLEGNIVCol;
    @FXML
    private TableColumn<ImageResult, Integer> noClassifiedCol;
    @FXML
    private Button saveBtn;
    @FXML
    private TextField resultsSearchBar;

    private ObservableList<ImageResult> results;

    @FXML
    /**
     * Initializes the controller
     */
    private void initialize() {
        logger.info("Initializing...");

        bindValueFactories();
        bindSearchBar();
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
        PropertyValueFactory<ImageResult, Integer> ABMGNColFactory = new PropertyValueFactory<>("ABMGN");
        PropertyValueFactory<ImageResult, Integer> ANCAColFactory = new PropertyValueFactory<>("ANCA");
        PropertyValueFactory<ImageResult, Integer> C3GNColFactory = new PropertyValueFactory<>("C3GN");
        PropertyValueFactory<ImageResult, Integer> CryoglobulinemicGNColFactory = new PropertyValueFactory<>(
                "CryoglobulinemicGN");
        PropertyValueFactory<ImageResult, Integer> DDDColFactory = new PropertyValueFactory<>("DDD");
        PropertyValueFactory<ImageResult, Integer> FibrillaryColFactory = new PropertyValueFactory<>("Fibrillary");
        PropertyValueFactory<ImageResult, Integer> IAGNColFactory = new PropertyValueFactory<>("IAGN");
        PropertyValueFactory<ImageResult, Integer> IgANColFactory = new PropertyValueFactory<>("IgAN");
        PropertyValueFactory<ImageResult, Integer> MPGNColFactory = new PropertyValueFactory<>("MPGN");
        PropertyValueFactory<ImageResult, Integer> MembranousColFactory = new PropertyValueFactory<>("Membranous");
        PropertyValueFactory<ImageResult, Integer> PGNMIDColFactory = new PropertyValueFactory<>("PGNMID");
        PropertyValueFactory<ImageResult, Integer> SLEGNIVColFactory = new PropertyValueFactory<>("SLEGNIV");
        PropertyValueFactory<ImageResult, Integer> noClassifiedColFactory = new PropertyValueFactory<>("noClassified");

        // Set value factories
        thumbnailCol.setCellValueFactory(thumbnailColFactory);
        imageCol.setCellValueFactory(imageColFactory);
        mostPredictedClassCol.setCellValueFactory(mostPredictedClassColFactory);
        nGlomeruliCol.setCellValueFactory(nGlomeruliColFactory);
        noScleroticCol.setCellValueFactory(noScleroticColFactory);
        scleroticCol.setCellValueFactory(scleroticColFactory);
        ABMGNCol.setCellValueFactory(ABMGNColFactory);
        ANCACol.setCellValueFactory(ANCAColFactory);
        C3GNCol.setCellValueFactory(C3GNColFactory);
        CryoglobulinemicGNCol.setCellValueFactory(CryoglobulinemicGNColFactory);
        DDDCol.setCellValueFactory(DDDColFactory);
        FibrillaryCol.setCellValueFactory(FibrillaryColFactory);
        IAGNCol.setCellValueFactory(IAGNColFactory);
        IgANCol.setCellValueFactory(IgANColFactory);
        MPGNCol.setCellValueFactory(MPGNColFactory);
        MembranousCol.setCellValueFactory(MembranousColFactory);
        PGNMIDCol.setCellValueFactory(PGNMIDColFactory);
        SLEGNIVCol.setCellValueFactory(SLEGNIVColFactory);
        noClassifiedCol.setCellValueFactory(noClassifiedColFactory);
    }

    /**
     * Binds the search bar to the table
     */
    private void bindSearchBar() {
        // Filter the images in the check list using the search bar
        resultsSearchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            // If the search bar is empty, show all the images
            if (newValue.isEmpty() || newValue.isBlank() || newValue == null) {
                resultsTable.setItems(results);
            } else {
                // Filter the results by their name and most predicted class
                ObservableList<ImageResult> filteredResults = Utils.filterResults(results, newValue);
                resultsTable.setItems(filteredResults);
            }
        });
    }

    @FXML
    /**
     * Saves the results to a file
     */
    private void saveResults() {
        logger.info("Saving results...");

        ObservableList<ImageResult> results = resultsTable.getItems();

        // Open file manager to select the file path and name
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Results");
        fileChooser.setInitialFileName("results.csv"); // Set default file name
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) resultsTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                CSVFormat format = CSVFormat.DEFAULT;
                format.builder().setDelimiter(';');
                try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
                    // Write the header
                    csvPrinter.printRecord(ImageResult.getCSVHeader());
                    for (ImageResult result : results) {
                        csvPrinter.printRecord(result.toCSVRow());
                    }
                    logger.info("Results saved");
                } catch (Exception e) {
                    logger.error("Error occurred while writing CSV records", e);
                }
            } catch (IOException e) {
                logger.error("Error occurred while creating FileWriter", e);
            }
        }

        logger.info("Results saved to {}", file.getAbsolutePath());
    }

    /**
     * Fills the table with the results
     * 
     * @param results
     */
    public void fillTable(ObservableList<ImageResult> results) {
        resultsTable.setItems(results);
        this.results = results;
    }
}
