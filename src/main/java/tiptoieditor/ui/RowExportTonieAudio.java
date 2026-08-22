package tiptoieditor.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.tonie.TonieAudioExportService;
import service.tonie.TonieExportDestinationService;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Controls for selecting a Tonie audio file and exporting its OGG payload. */
public class RowExportTonieAudio {

    private final Stage stage;
    private final Label selectedTonieFileLabel;
    private final TonieAudioExportService exportService;
    private final TonieExportDestinationService exportDestinationService;
    private final Supplier<String> titleSupplier;
    private final Consumer<String> logger;
    private final Consumer<File> exportCompleteConsumer;
    private final Consumer<String> statusUpdater;
    private final WorkflowTaskManager taskManager;
    private File selectedTonieFile;
    private String cancelText = "Tonie audio export cancelled.";

    public RowExportTonieAudio(Stage stage, Button selectTonieFileButton, Label selectedTonieFileLabel,
            Button exportButton, TonieAudioExportService exportService,
            TonieExportDestinationService exportDestinationService,
            Supplier<String> titleSupplier,
            Consumer<String> logger, Consumer<File> exportCompleteConsumer,
            Consumer<String> statusUpdater, WorkflowTaskManager taskManager) {
        this.stage = stage;
        this.selectedTonieFileLabel = selectedTonieFileLabel;
        this.exportService = exportService;
        this.exportDestinationService = exportDestinationService;
        this.titleSupplier = titleSupplier;
        this.logger = logger;
        this.exportCompleteConsumer = exportCompleteConsumer;
        this.statusUpdater = statusUpdater;
        this.taskManager = taskManager;
        selectTonieFileButton.setOnAction(e -> selectTonieFile());
        exportButton.setOnAction(e -> exportAudio());
    }

    private void selectTonieFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Tonie Audio File");
        if (selectedTonieFile != null && selectedTonieFile.getParentFile().isDirectory()) {
            chooser.setInitialDirectory(selectedTonieFile.getParentFile());
        }
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            setSelectedTonieFile(file);
        }
    }

    /**
     * Sets the Tonie file used by both the export pane and the automatic Run
     * workflow.
     */
    public void setSelectedTonieFile(File selectedTonieFile) {
        this.selectedTonieFile = selectedTonieFile;
        File parentDirectory = selectedTonieFile.getParentFile();
        selectedTonieFileLabel.setText(parentDirectory == null
                ? selectedTonieFile.getName()
                : parentDirectory.getName() + "/" + selectedTonieFile.getName());
        logger.accept("Selected Tonie file: " + selectedTonieFile.getAbsolutePath());
    }

    private void exportAudio() {
        if (selectedTonieFile == null) {
            logger.accept("Please select a Tonie file first.");
            return;
        }

        File outputDirectory;
        try {
            outputDirectory = exportDestinationService.createExportFolder(selectedTonieFile, titleSupplier.get());
            logger.accept("Created Tonie export folder: " + outputDirectory.getAbsolutePath());
        } catch (IOException e) {
            logger.accept("Could not create Tonie export folder: " + e.getMessage());
            statusUpdater.accept("Tonie audio export failed.");
            return;
        }
        runToolExportAudio(selectedTonieFile, outputDirectory, exportCompleteConsumer);
    }

    /**
     * Exports a known Tonie file to a known directory and continues on the JavaFX
     * thread.
     */
    public void runToolExportAudio(File inputFile, File outputDirectory, Consumer<File> onComplete) {
        runToolExportAudio(inputFile, outputDirectory, onComplete, null);
    }

    /** Exports Tonie audio and reports a failed workflow step to {@code onFailure}. */
    public void runToolExportAudio(File inputFile, File outputDirectory, Consumer<File> onComplete,
            Consumer<String> onFailure) {
        runToolExportAudio(inputFile, outputDirectory, onComplete, onFailure, true);
    }

    /**
     * Exports Tonie audio, optionally reusing a legal confirmation that was already
     * accepted for a batch workflow.
     */
    public void runToolExportAudio(File inputFile, File outputDirectory, Consumer<File> onComplete,
            Consumer<String> onFailure, boolean requireConfirmation) {
        if (inputFile == null || outputDirectory == null) {
            logger.accept("Please select a Tonie file and an output folder first.");
            notifyFailure(onFailure, "Please select a Tonie file and an output folder first.");
            return;
        }
        if (requireConfirmation && !confirmExport()) {
            notifyFailure(onFailure, "Tonie audio export cancelled.");
            return;
        }

        String title = titleSupplier.get();
        logger.accept("Exporting chapter audio from: " + inputFile.getAbsolutePath());
        statusUpdater.accept("Exporting Tonie audio fiiles...");
        taskManager.start("tonie-audio-export", () -> {
            try {
                var results = exportService.export(inputFile.toPath(), outputDirectory.toPath(), title);
                if (Thread.currentThread().isInterrupted()) {
                    logger.accept(cancelText);
                    statusUpdater.accept(cancelText);
                    notifyFailure(onFailure, cancelText);
                    return;
                }
                for (TonieAudioExportService.ExportResult result : results) {
                    logger.accept("Exported OGG: " + result.exportedFile().toAbsolutePath());
                }
                logger.accept("Exported " + results.size() + " OGG file(s), SHA-1: " + results.getFirst().audioSha1());
                statusUpdater.accept("Exported Tonie audio fiiles. Done!");
                if (onComplete != null) {
                    Platform.runLater(() -> onComplete.accept(outputDirectory));
                }
            } catch (InterruptedIOException e) {
                logger.accept(cancelText);
                statusUpdater.accept(cancelText);
                notifyFailure(onFailure, cancelText);
            } catch (FileAlreadyExistsException e) {
                logger.accept(
                        "Export skipped: the output file already exists. Choose another folder or remove it first.");
                statusUpdater.accept("Tonie audio export skipped.");
                if (onComplete != null) {
                    Platform.runLater(() -> onComplete.accept(outputDirectory));
                }
            } catch (Exception e) {
                logger.accept("Could not export Tonie audio: " + e.getMessage());
                statusUpdater.accept("Tonie audio export failed.");
                notifyFailure(onFailure, "Tonie audio export failed.");
            }
        });
    }

    private static void notifyFailure(Consumer<String> onFailure, String message) {
        if (onFailure != null) {
            Platform.runLater(() -> onFailure.accept(message));
        }
    }

    /** Shows the legal notice before a batch that contains one or more Tonie files. */
    public boolean confirmExportOnce() {
        return confirmExport();
    }

    private boolean confirmExport() {
        Alert legalNotice = new Alert(Alert.AlertType.CONFIRMATION);
        legalNotice.setTitle("Legal information");
        legalNotice.setHeaderText("Export audio content from a Tonie file?");
        legalNotice.setContentText("Only export content you are allowed to use. Do not share exported files; "
                + "they may contain information that identifies the Tonie and sharing may be illegal.");
        Optional<ButtonType> response = legalNotice.showAndWait();
        return response.isPresent() && response.get() == ButtonType.OK;
    }
}
