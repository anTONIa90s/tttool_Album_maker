package tiptoieditor.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import service.yaml.GenerateYamlService;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Controls for selecting an album folder and generating its YAML files.
 */
public class RowCreateYaml {

    private final Label selectedAlbumFolderLabel;
    private final Supplier<String> productIdSupplier;
    private final Consumer<File> yamlFileConsumer;
    private final Consumer<String> logger;
    private File selectedAlbumFolder;

    public RowCreateYaml(Stage stage, Button selectAlbumFolderButton, Label selectedAlbumFolderLabel,
                         Button createYamlButton, Supplier<String> productIdSupplier,
                         Consumer<File> yamlFileConsumer, Consumer<String> logger) {
        this.selectedAlbumFolderLabel = selectedAlbumFolderLabel;
        this.productIdSupplier = productIdSupplier;
        this.yamlFileConsumer = yamlFileConsumer;
        this.logger = logger;
        selectAlbumFolderButton.setOnAction(e -> selectAlbumFolder(stage));
        createYamlButton.setOnAction(e -> runToolCreateYaml());
    }

    private void selectAlbumFolder(Stage stage) {
        File folder = FolderSelectionDialog.chooseFolder(stage, selectedAlbumFolder);
        if (folder != null) {
            setSelectedAlbumFolder(folder);
            logger.accept("Selected album folder: " + folder.getAbsolutePath());
        }
    }

    public File getSelectedAlbumFolder() {
        return selectedAlbumFolder;
    }

    public void setSelectedAlbumFolder(File selectedAlbumFolder) {
        this.selectedAlbumFolder = selectedAlbumFolder;
        selectedAlbumFolderLabel.setText(selectedAlbumFolder.getName());
    }

    public boolean runToolCreateYaml() {
        String productIdText = productIdSupplier.get().trim();
        if (productIdText.isEmpty()) {
            logger.accept("Please enter a product ID.");
            return false;
        } else if (selectedAlbumFolder == null) {
            logger.accept("Please select a folder first.");
            return false;
        }

        try {
            logger.accept("Creating YAML...");
            GenerateYamlService.GeneratedYamlFiles generatedFiles = new GenerateYamlService()
                    .generate(Integer.parseInt(productIdText), selectedAlbumFolder);
            File yamlFile = generatedFiles.yamlFile().toFile();
            yamlFileConsumer.accept(yamlFile);
            logger.accept("YAML created: " + yamlFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.accept("Could not create YAML: " + e.getMessage());
            return false;
        }
    }
}
