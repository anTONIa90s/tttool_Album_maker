package tiptoieditor.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import service.tttool.TttoolService;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Consumer;

/** Controls for creating an OID table PDF from an album YAML file. */
public class RowCreateOidTable {

    private final Label selectedAlbumFolderLabel;
    private final TttoolService tttoolService;
    private final Consumer<String> logger;
    private final Consumer<String> statusUpdater;
    private final WorkflowTaskManager taskManager;
    private File selectedAlbumFolder;

    public RowCreateOidTable(Stage stage, Button selectAlbumFolderButton, Label selectedAlbumFolderLabel,
            Button createOidTableButton, TttoolService tttoolService,
            Consumer<String> logger, Consumer<String> statusUpdater,
            WorkflowTaskManager taskManager) {
        this.selectedAlbumFolderLabel = selectedAlbumFolderLabel;
        this.tttoolService = tttoolService;
        this.logger = logger;
        this.statusUpdater = statusUpdater;
        this.taskManager = taskManager;
        selectAlbumFolderButton.setOnAction(e -> selectAlbumFolder(stage));
        createOidTableButton.setOnAction(e -> runToolCreateOidTable());
    }

    private void selectAlbumFolder(Stage stage) {
        File folder = FolderSelectionDialog.chooseFolder(stage, selectedAlbumFolder);
        if (folder != null) {
            setSelectedAlbumFolder(folder);
            logger.accept("Selected album folder: " + folder.getAbsolutePath());
        }
    }

    public void setSelectedAlbumFolder(File selectedAlbumFolder) {
        this.selectedAlbumFolder = selectedAlbumFolder;
        selectedAlbumFolderLabel.setText(selectedAlbumFolder.getName());
    }

    public void runToolCreateOidTable() {
        if (selectedAlbumFolder == null) {
            logger.accept("Please select an album folder first.");
            return;
        }

        File yamlFile = findAlbumYamlFile(selectedAlbumFolder);
        if (yamlFile == null) {
            logger.accept("The selected album folder must contain a YAML file.");
            statusUpdater.accept("Could not created OID table. YAML file missing.");
            return;
        }

        logger.accept("Creating OID table from: " + yamlFile.getAbsolutePath());
        statusUpdater.accept("Creating OID table...");
        taskManager.start("tttool-oid-table", () -> {
            try {
                String output = tttoolService.createOidTable(yamlFile.toPath());
                Platform.runLater(() -> {
                    statusUpdater.accept("Created OID table. Done!");
                    logger.accept(output.isBlank() ? "tttool finished successfully." : output);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    logger.accept("OID table creation cancelled.");
                    statusUpdater.accept("OID table creation cancelled.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.accept("Could not create OID table: " + e.getMessage());
                    statusUpdater.accept("OID table creation failed.");
                });
            }
        });
    }

    /** Uses the main album YAML, never its generated codes YAML. */
    private File findAlbumYamlFile(File albumFolder) {
        File[] yamlFiles = albumFolder.listFiles(file -> {
            if (!file.isFile()) {
                return false;
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            return (name.endsWith(".yaml") || name.endsWith(".yml")) && !name.endsWith(".codes.yaml");
        });
        return yamlFiles == null ? null
                : Arrays.stream(yamlFiles)
                        .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
                        .findFirst()
                        .orElse(null);
    }
}
