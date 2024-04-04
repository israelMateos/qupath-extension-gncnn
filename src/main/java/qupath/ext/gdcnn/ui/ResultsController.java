package qupath.ext.gdcnn.ui;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import qupath.ext.gdcnn.entities.ImageResult;

/**
 * Controller for the results pane
 */
public class ResultsController {

    private static final Logger logger = LoggerFactory.getLogger(ResultsController.class);

    @FXML
    private TableView<ImageResult> resultsTable;
    @FXML
    private Button saveBtn;

    @FXML
    /**
     * Initializes the controller
     */
    private void initialize() {
        logger.info("Initializing...");
    }

    public void fillTable(List<ImageResult> results) {
        resultsTable.getItems().addAll(results);
    }
}
