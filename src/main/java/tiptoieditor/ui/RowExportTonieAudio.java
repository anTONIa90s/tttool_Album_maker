package tiptoieditor.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.tonie.TonieAudioExportService;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Controls for selecting a Tonie audio file and exporting its OGG payload. */
public class RowExportTonieAudio {

    private final Stage stage;
    private final Label selectedTonieFileLabel;
    private final TonieAudioExportService exportService;
    private final Supplier<String> titleSupplier;
    private final Consumer<String> logger;
    private File selectedTonieFile;

    public RowExportTonieAudio(Stage stage, Button selectTonieFileButton, Label selectedTonieFileLabel,
                               Button exportButton, TonieAudioExportService exportService,
                               Supplier<String> titleSupplier,
                               Consumer<String> logger) {
        this.stage = stage;
        this.selectedTonieFileLabel = selectedTonieFileLabel;
        this.exportService = exportService;
        this.titleSupplier = titleSupplier;
        this.logger = logger;
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
            selectedTonieFile = file;
            File parentDirectory = file.getParentFile();
            selectedTonieFileLabel.setText(parentDirectory == null ? file.getName() : parentDirectory.getName());
            logger.accept("Selected Tonie file: " + file.getAbsolutePath());
        }
    }

    private void exportAudio() {
        if (selectedTonieFile == null) {
            logger.accept("Please select a Tonie file first.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Audio Export Folder");
        chooser.setInitialDirectory(selectedTonieFile.getParentFile());
        File outputDirectory = chooser.showDialog(stage);
        if (outputDirectory == null) {
            return;
        }

        runToolExportAudio(selectedTonieFile, outputDirectory, null);
    }

    /** Exports a known Tonie file to a known directory and continues on the JavaFX thread. */
    public void runToolExportAudio(File inputFile, File outputDirectory, Consumer<File> onComplete) {
        if (inputFile == null || outputDirectory == null) {
            logger.accept("Please select a Tonie file and an output folder first.");
            return;
        }
        if (!confirmExport()) {
            return;
        }

        String title = titleSupplier.get();
        logger.accept("Exporting chapter audio from: " + inputFile.getAbsolutePath());
        new Thread(() -> {
            try {
                var results = exportService.export(inputFile.toPath(), outputDirectory.toPath(), title);
                for (TonieAudioExportService.ExportResult result : results) {
                    logger.accept("Exported OGG: " + result.exportedFile().toAbsolutePath());
                }
                logger.accept("Exported " + results.size() + " OGG file(s), SHA-1: " + results.getFirst().audioSha1());
                if (onComplete != null) {
                    Platform.runLater(() -> onComplete.accept(outputDirectory));
                }
            } catch (FileAlreadyExistsException e) {
                logger.accept("Export skipped: the output file already exists. Choose another folder or remove it first.");
            } catch (Exception e) {
                logger.accept("Could not export Tonie audio: " + e.getMessage());
            }
        }, "tonie-audio-export").start();
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
