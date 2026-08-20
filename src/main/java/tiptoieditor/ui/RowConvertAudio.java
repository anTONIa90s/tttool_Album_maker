package tiptoieditor.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import service.audio.AudioConvertService;
import service.audio.AudioCopyService;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Controls for selecting and converting an audio folder.
 */
public class RowConvertAudio {

    private final Label selectedAudioFolderLabel;
    private final AudioCopyService audioCopyService;
    private final AudioConvertService audioConvertService;
    private final Supplier<String> albumNameSupplier;
    private final Consumer<String> logger;
    private Consumer<File> selectedAudioFolderConsumer = folder -> { };
    private Consumer<File> audioPreparedConsumer = folder -> { };
    private File selectedAudioFolder;

    public RowConvertAudio(Stage stage, Button selectAudioFolderButton, Label selectedAudioFolderLabel,
            Button convertButton, AudioCopyService audioCopyService,
            AudioConvertService audioConvertService, Supplier<String> albumNameSupplier, Consumer<String> logger) {
        this.selectedAudioFolderLabel = selectedAudioFolderLabel;
        this.audioCopyService = audioCopyService;
        this.audioConvertService = audioConvertService;
        this.albumNameSupplier = albumNameSupplier;
        this.logger = logger;
        selectAudioFolderButton.setOnAction(e -> selectAudioFolder(stage));
        convertButton.setOnAction(e -> runToolCopyAndConvert());
    }

    public void selectAudioFolder(Stage stage) {
        File folder = FolderSelectionDialog.chooseFolder(stage, selectedAudioFolder);
        if (folder != null) {
            setSelectedAudioFolder(folder);
            logger.accept("Selected audio folder: " + folder.getAbsolutePath());
        }
    }

    public void runToolCopyAndConvert() {
        runToolCopyAndConvert(null);
    }

    public void runToolCopyAndConvert(Consumer<File> onComplete) {
        if (selectedAudioFolder == null) {
            logger.accept("Please select a folder first.");
            return;
        }

        File sourceAudioFolder = selectedAudioFolder;
        String albumName = albumNameSupplier.get();
        String resolvedAlbumName = albumName == null || albumName.isBlank() ? "tttoolAlbum" : albumName.trim();

        logger.accept("Preparing folder structure for album: " + resolvedAlbumName);

        new Thread(() -> {
            File audioFolder = audioCopyService.prepareAudioFolder(sourceAudioFolder, resolvedAlbumName);
            logger.accept("Audio copied to: " + audioFolder.getAbsolutePath());
            processAudioFolder(audioFolder, onComplete);
        }, "audio-convert").start();
    }

    /** Processes an existing album audio directory and then continues on the JavaFX thread. */
    public void runToolProcessAudio(File audioFolder, Consumer<File> onComplete) {
        new Thread(() -> processAudioFolder(audioFolder, onComplete), "audio-process").start();
    }

    /** Copies an existing album's exported audio into {@code audio} and processes it. */
    public void runToolCopyAndConvertForExistingAlbum(File albumFolder, Consumer<File> onComplete) {
        new Thread(() -> {
            File audioFolder = audioCopyService.prepareAudioFolderForExistingAlbum(albumFolder);
            logger.accept("Audio copied to: " + audioFolder.getAbsolutePath());
            processAudioFolder(audioFolder, onComplete);
        }, "audio-convert").start();
    }

    private void processAudioFolder(File audioFolder, Consumer<File> onComplete) {
        logger.accept("Processing audio...");
        audioConvertService.processFolder(audioFolder);

        logger.accept("Audio successfully processed.");
        Platform.runLater(() -> {
            audioPreparedConsumer.accept(audioFolder);
            if (onComplete != null) {
                onComplete.accept(audioFolder);
            }
        });
    }

    public File getSelectedAudioFolder() {
        return selectedAudioFolder;
    }

    public void setSelectedAudioFolder(File selectedAudioFolder) {
        this.selectedAudioFolder = selectedAudioFolder;
        selectedAudioFolderLabel.setText(selectedAudioFolder.getName());
        selectedAudioFolderConsumer.accept(selectedAudioFolder);
    }

    /** Registers a listener for audio-folder selections made by any workflow. */
    public void setOnSelectedAudioFolder(Consumer<File> selectedAudioFolderConsumer) {
        this.selectedAudioFolderConsumer = selectedAudioFolderConsumer;
    }

    /** Registers a listener that receives the {@code audio} folder after preparation is complete. */
    public void setOnAudioPrepared(Consumer<File> audioPreparedConsumer) {
        this.audioPreparedConsumer = audioPreparedConsumer;
    }

}
