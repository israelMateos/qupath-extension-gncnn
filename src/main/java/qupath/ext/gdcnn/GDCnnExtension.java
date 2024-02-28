package qupath.ext.gdcnn;

import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.MenuItem;
import qupath.fx.prefs.controlsfx.PropertyItemBuilder;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.prefs.PathPrefs;

/**
 * QuPath extension to detect and classify glomeruli using deep learning.
 * 
 * @author Israel Mateos Aparicio
 */
public class GDCnnExtension implements QuPathExtension, GitHubProject {

	private static final Logger logger = LoggerFactory.getLogger(GDCnnExtension.class);

	private static final String EXTENSION_NAME = "GDCnn";
	private static final String EXTENSION_DESCRIPTION = "An extension which detects and classifies glomeruli in QuPath using deep learning.";
	private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.0");
	private static final GitHubRepo EXTENSION_REPOSITORY = GitHubRepo.create(
			EXTENSION_NAME, "israelMateos", "qupath-extension-gdcnn");

	private boolean isInstalled = false;
	private BooleanProperty enableExtensionProperty = PathPrefs.createPersistentPreference(
			"enableExtension", true);
	private StringProperty pythonPathProperty = PathPrefs.createPersistentPreference(
			"pythonPath", "/home/visilab/anaconda3/envs/GDCnn/bin/python3.8");
	private StringProperty gdcnnPathProperty = PathPrefs.createPersistentPreference(
			"gdcnnPath", "/home/visilab/Israel/GDCnn");

	@Override
	public void installExtension(QuPathGUI qupath) {
		if (isInstalled) {
			logger.debug("{} is already installed", getName());
			return;
		}
		isInstalled = true;
		addMenuItems(qupath);
		addPreferences();
	}

	/**
	 * Adds the needed preferences to the QuPath preferences dialog.
	 */
	private void addPreferences() {
		// Get an instance of the options
		GDCnnSetup options = GDCnnSetup.getInstance();

		// Set options to current values
		options.setPythonPath(pythonPathProperty.get());
		options.setGdcnnPath(gdcnnPathProperty.get());

		// Listen for property changes
		pythonPathProperty.addListener((v, o, n) -> options.setPythonPath(n));
		gdcnnPathProperty.addListener((v, o, n) -> options.setGdcnnPath(n));

		// Create the items for the preferences dialog
		PropertySheet.Item enableExtensionItem = new PropertyItemBuilder<>(enableExtensionProperty, Boolean.class)
				.propertyType(PropertyItemBuilder.PropertyType.GENERAL)
				.name("Enable " + EXTENSION_NAME)
				.category(EXTENSION_NAME)
				.description("Enable or disable the " + EXTENSION_NAME + " extension.")
				.build();

		PropertySheet.Item pythonPathItem = new PropertyItemBuilder<>(pythonPathProperty, String.class)
				.propertyType(PropertyItemBuilder.PropertyType.FILE)
				.name("Python path")
				.category(EXTENSION_NAME)
				.description("Path to the Python executable.")
				.build();

		PropertySheet.Item gdcnnPathItem = new PropertyItemBuilder<>(gdcnnPathProperty, String.class)
				.propertyType(PropertyItemBuilder.PropertyType.DIRECTORY)
				.name("GDCnn path")
				.category(EXTENSION_NAME)
				.description("Path to the GDCnn project.")
				.build();

		// Add the items to the preferences dialog
		QuPathGUI.getInstance().getPreferencePane().getPropertySheet().getItems().addAll(
				enableExtensionItem, pythonPathItem, gdcnnPathItem);
	}

	/**
	 * Adds a menu item to the Extensions menu.
	 * 
	 * @param qupath
	 */
	private void addMenuItems(QuPathGUI qupath) {
		var menu = qupath.getMenu("Extensions>" + EXTENSION_NAME, true);
		MenuItem tileItem = new MenuItem("Tile WSIs");
		MenuItem detectionItem = new MenuItem("Run detection");
		MenuItem exportItem = new MenuItem("Export annotations");

		GDCnn gdcnn = new GDCnn(qupath);

		tileItem.setOnAction(e -> {
			try {
				gdcnn.tileWSIs();
			} catch (Exception ex) {
				logger.error("Error tiling WSIs", ex);
			}
		});
		detectionItem.setOnAction(e -> {
			try {
				gdcnn.detectGlomeruli();
			} catch (Exception ex) {
				logger.error("Error running detection", ex);
			}
		});
		exportItem.setOnAction(e -> {
			try {
				gdcnn.exportAnnotations();
			} catch (Exception ex) {
				logger.error("Error exporting annotations", ex);
			}
		});

		tileItem.disableProperty().bind(enableExtensionProperty.not());
		detectionItem.disableProperty().bind(enableExtensionProperty.not());
		exportItem.disableProperty().bind(enableExtensionProperty.not());

		menu.getItems().addAll(tileItem, detectionItem, exportItem);
	}

	@Override
	public String getName() {
		return EXTENSION_NAME;
	}

	@Override
	public String getDescription() {
		return EXTENSION_DESCRIPTION;
	}

	@Override
	public Version getQuPathVersion() {
		return EXTENSION_QUPATH_VERSION;
	}

	@Override
	public GitHubRepo getRepository() {
		return EXTENSION_REPOSITORY;
	}
}
