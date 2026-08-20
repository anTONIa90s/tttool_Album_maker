package tiptoieditor.ui;

import javafx.application.Platform;
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
    private final Consumer<String> statusUpdater;
    private File selectedAlbumFolder;

    public RowCreateYaml(Stage stage, Button selectAlbumFolderButton, Label selectedAlbumFolderLabel,
            Button createYamlButton, Supplier<String> productIdSupplier,
            Consumer<File> yamlFileConsumer, Consumer<String> logger,
            Consumer<String> statusUpdater) {
        this.selectedAlbumFolderLabel = selectedAlbumFolderLabel;
        this.productIdSupplier = productIdSupplier;
        this.yamlFileConsumer = yamlFileConsumer;
        this.logger = logger;
        this.statusUpdater = statusUpdater;
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

    public void runToolCreateYaml() {
        runToolCreateYaml(null);
    }

    /**
     * Generates YAML in the background and invokes {@code onComplete} on the JavaFX
     * thread.
     */
    public void runToolCreateYaml(Consumer<File> onComplete) {
        String productIdText = productIdSupplier.get().trim();
        if (productIdText.isEmpty()) {
            logger.accept("Please enter a product ID.");
            return;
        } else if (selectedAlbumFolder == null) {
            logger.accept("Please select a folder first.");
            return;
        }

        int productId;
        try {
            productId = Integer.parseInt(productIdText);
        } catch (NumberFormatException e) {
            logger.accept("Please enter a valid product ID.");
            return;
        }
        File albumFolder = selectedAlbumFolder;
        statusUpdater.accept("Creating Yaml...");
        new Thread(() -> {
            try {
                logger.accept("Creating YAML...");
                GenerateYamlService.GeneratedYamlFiles generatedFiles = new GenerateYamlService()
                        .generate(productId, albumFolder);
                File yamlFile = generatedFiles.yamlFile().toFile();
                logger.accept("YAML created: " + yamlFile.getAbsolutePath());
                Platform.runLater(() -> {
                    yamlFileConsumer.accept(yamlFile);
                    statusUpdater.accept("Created Yaml. Done!");
                    if (onComplete != null) {
                        onComplete.accept(yamlFile);
                    }
                });
            } catch (Exception e) {
                logger.accept("Could not create YAML: " + e.getMessage());
            }
        }, "yaml-create").start();
    }
}
