package tiptoieditor.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.audio.AudioFileNameService;
import service.tttool.TttoolService;

import java.io.File;
import java.util.function.Consumer;

/**
 * Controls for selecting a YAML file and creating its GME file.
 */
public class RowYamlToGme {

    private final Label selectedYamlFileLabel;
    private final TttoolService tttoolService;
    private final AudioFileNameService audioFileNameService;
    private final Consumer<String> logger;
    private File selectedYamlFile;

    public RowYamlToGme(Stage stage, Button selectYamlFileButton, Label selectedYamlFileLabel,
                        Button createGmeButton, TttoolService tttoolService,
                        AudioFileNameService audioFileNameService, Consumer<String> logger) {
        this.selectedYamlFileLabel = selectedYamlFileLabel;
        this.tttoolService = tttoolService;
        this.audioFileNameService = audioFileNameService;
        this.logger = logger;
        selectYamlFileButton.setOnAction(e -> loadYamlFile(stage));
        createGmeButton.setOnAction(e -> runToolCreateGmeFromYaml());
    }

    public void loadYamlFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Yaml File");

        if (selectedYamlFile != null && selectedYamlFile.exists()) {
            fileChooser.setInitialDirectory(selectedYamlFile.getParentFile());
        }

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("YAML Files", "*.yaml", "*.yml", "*.YAML", "*.YML"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            setSelectedYamlFile(file);
            logger.accept("Selected Yaml File: " + file.getAbsolutePath());
        }
    }

    public void runToolCreateGmeFromYaml() {
        if (selectedYamlFile == null) {
            logger.accept("Please select a YAML file first.");
            return;
        }

        File yamlFile = selectedYamlFile;
        logger.accept("Creating GME from: " + yamlFile.getAbsolutePath());

        new Thread(() -> {
            try {
                File audioFolder = yamlFile.toPath().getParent().resolve("audio").toFile();
                audioFileNameService.renameFiles(audioFolder);
                logger.accept("Audio files renamed in: " + audioFolder.getAbsolutePath());
                String output = tttoolService.assemble(yamlFile.toPath());
                Platform.runLater(() -> {
                    logger.accept(output.isBlank() ? "tttool finished successfully." : output);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.accept("Could not create GME: " + e.getMessage());
                });
            }
        }, "tttool-assemble").start();
    }

    public void setSelectedYamlFile(File selectedYamlFile) {
        this.selectedYamlFile = selectedYamlFile;
        selectedYamlFileLabel.setText(selectedYamlFile.getName());
    }

}
