package tiptoieditor.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.audio.AudioConvertService;
import service.audio.AudioCopyService;
import service.audio.AudioFileNameService;
import service.tttool.TttoolService;
import service.yaml.GenerateYamlService;

import java.io.File;

public class MainWindow {

    private TextArea outputArea;
    private Label selectedAudioFolderLabel;
    private Label selectedYamlFileLabel;
    private Label statusLabel;
    private Button selectYamlFileButton;

    private File selectedFolder;
    private File selectedAudioFolder;
    private File selectedAlbumFolder;
    private TextField productNameField;
    private TextField productIdField;
    private File selectedYamlFile;

    private final TttoolService tttoolService = new TttoolService();

    private final AudioCopyService audioCopyService = new AudioCopyService();
    private final AudioConvertService audioConvertService = new AudioConvertService();
    private final AudioFileNameService audioFileNameService = new AudioFileNameService();

    public void show(Stage stage) {

        // --- Title ---
        Label title = new Label("TTTool GUI");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        productNameField = new TextField();
        productNameField.setPromptText("Album name (default: tttoolAlbum)");

        // --- Convert Audio ---
        Button selectAudioFolderButton = new Button("Select Audio Folder");
        selectedAudioFolderLabel = new Label("No folder selected");
        Button runConvertButton = new Button("Convert Audio");
        Label statusConvertLabel = new Label("");
        HBox RowConvertAudio = new HBox(10, selectAudioFolderButton, selectedAudioFolderLabel, runConvertButton,
                statusConvertLabel);

        // --- Create Yaml ---
        Button selectAlbumFolderButton = new Button("Select Album Folder");
        Label selectedAlbumFolderLabel = new Label("No folder selected");
        productIdField = new TextField();
        productIdField.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));
        productIdField.setPrefColumnCount(4);
        productIdField.setPromptText("Enter Product ID");
        Button runCreateYamlButton = new Button("Create Yaml");
        Label statusCreateYamlLabel = new Label("");
        HBox RowCreateYaml = new HBox(10, selectAlbumFolderButton, selectedAlbumFolderLabel,
                productIdField,
                runCreateYamlButton,
                statusCreateYamlLabel);

        // --- Convert Yaml to Gme ---
        selectYamlFileButton = new Button("Select yaml file");
        selectedYamlFileLabel = new Label("No YAML file selected");
        Button runGmeButton = new Button("Create GME");
        Label statusGmeLabel = new Label("");
        HBox RowYamlToGme = new HBox(10, selectYamlFileButton, selectedYamlFileLabel, runGmeButton, statusGmeLabel);

        // --- Run button ---
        Button runButton = new Button("Run tttool");
        statusLabel = new Label(""); // initially empty
        HBox runRow = new HBox(10, runButton, statusLabel);

        // Stack buttons vertically
        VBox buttonBox = new VBox(10, productNameField, RowConvertAudio, RowCreateYaml, RowYamlToGme, runRow);

        // --- Output / Logger ---
        outputArea = new TextArea();
        outputArea.setEditable(false);

        TitledPane logPane = new TitledPane();
        logPane.setText("Logs");
        logPane.setContent(outputArea);
        logPane.setExpanded(false); // collapsed by default
        logPane.setMaxHeight(200); // prevent it from growing too much

        // --- Root layout (IMPORTANT: BorderPane) ---
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        root.setTop(title);
        root.setCenter(buttonBox);
        root.setBottom(logPane);

        // spacing tweaks (optional but nice)
        BorderPane.setMargin(title, new Insets(0, 0, 10, 0));
        BorderPane.setMargin(buttonBox, new Insets(0, 0, 10, 0));

        // --- Actions ---

        selectAudioFolderButton.setOnAction(e -> {
            File folder = loadFolder(stage, selectedAudioFolder);
            if (folder != null) {
                selectedAudioFolder = folder;
                selectedAudioFolderLabel.setText(folder.getName());
                statusLabel.setText("");
                log("Selected audio folder: " + folder.getAbsolutePath());
            }
        });
        selectAlbumFolderButton.setOnAction(e -> {
            File folder = loadFolder(stage, selectedAlbumFolder);
            if (folder != null) {
                selectedAlbumFolder = folder;
                selectedAlbumFolderLabel.setText(folder.getName());
                statusLabel.setText("");
                log("Selected album folder: " + folder.getAbsolutePath());
            }
        });
        selectYamlFileButton.setOnAction(e -> loadYamlFile(stage));
        runButton.setOnAction(e -> runTool());
        runConvertButton.setOnAction(e -> runToolCopyAndConvert());
        runCreateYamlButton.setOnAction(e -> runToolCreateYaml());
        runGmeButton.setOnAction(e -> runToolCreateGmeFromYaml());

        // --- Scene ---
        Scene scene = new Scene(root, 500, 350);
        stage.setTitle("TTTool GUI");
        stage.setScene(scene);
        stage.show();
    }

    private void loadYamlFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Yaml File");

        // start folder search at former parent-directory
        if (selectedYamlFile != null && selectedYamlFile.exists()) {
            fileChooser.setInitialDirectory(selectedYamlFile.getParentFile());
        }

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("YAML Files", "*.yaml", "*.yml", "*.YAML", "*.YML"));

        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedYamlFile = file;

            // show folder name next to button
            selectedYamlFileLabel.setText(file.getName());
            statusLabel.setText("");

            log("Selected Yaml File: " + file.getAbsolutePath());
        }
    }

    private File loadFolder(Stage stage, File currentFolder) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");

        // start folder search at former parent-directory
        if (currentFolder != null && currentFolder.exists()) {
            File parentFolder = currentFolder.getParentFile();
            directoryChooser.setInitialDirectory(parentFolder != null ? parentFolder : currentFolder);
        }

        return directoryChooser.showDialog(stage);
    }

    private void runToolCreateGmeFromYaml() {
        if (selectedYamlFile == null) {
            statusLabel.setText("Please select a yaml file first.");
            log("Please select a YAML file first.");
            return;
        }
        File yamlFile = selectedYamlFile;
        log("Creating GME from: " + yamlFile.getAbsolutePath());
        statusLabel.setText("Creating GME...");

        new Thread(() -> {
            try {
                File audioFolder = yamlFile.toPath().getParent().resolve("audio").toFile();
                audioFileNameService.renameFiles(audioFolder);
                log("Audio files renamed in: " + audioFolder.getAbsolutePath());
                String output = tttoolService.assemble(yamlFile.toPath());
                javafx.application.Platform.runLater(() -> {
                    log(output.isBlank() ? "tttool finished successfully." : output);
                    statusLabel.setText("GME created.");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    log("Could not create GME: " + e.getMessage());
                    statusLabel.setText("Could not create GME.");
                });
            }
        }, "tttool-assemble").start();
    }

    private void runToolCopyAndConvert() {
        if (selectedAudioFolder == null) {
            statusLabel.setText("Please select a folder first.");
            log("Please select a folder first.");
            return;
        }

        String albumName = productNameField.getText();
        String resolvedAlbumName = albumName == null || albumName.isBlank() ? "tttoolAlbum" : albumName.trim();

        log("Preparing folder structure for album: " + resolvedAlbumName);
        statusLabel.setText("⏳ Processing...");

        new Thread(() -> {
            // Step 1: copy files
            File audioFolder = audioCopyService.prepareAudioFolder(selectedAudioFolder, resolvedAlbumName);
            log("Audio copied to: " + audioFolder.getAbsolutePath());
            statusLabel.setText("Audio copied.");

            // Step 2: process audio
            log("Processing audio...");
            statusLabel.setText("Processing audio...");
            audioConvertService.processFolder(audioFolder);

            // done
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("✅ Done!");
            });
            log("Done.");
        }).start();

    }

    private void runToolCreateYaml() {
        String productIdText = productIdField.getText().trim();
        if (productIdText.isEmpty()) {
            statusLabel.setText("Please enter a product ID.");
            log("Please enter a product ID.");
            return;
        } else if (selectedAlbumFolder == null) {
            statusLabel.setText("Please select a folder first.");
            log("Please select a folder first.");
            return;
        }

        try {
            statusLabel.setText("creating YAML...");
            log("creating YAML...");
            GenerateYamlService.GeneratedYamlFiles generatedFiles = new GenerateYamlService()
                    .generate(Integer.parseInt(productIdText), selectedAlbumFolder);
            selectedYamlFile = generatedFiles.yamlFile().toFile();
            selectedYamlFileLabel.setText(selectedYamlFile.getName());
            statusLabel.setText("YAML created.");
            log("YAML created: " + selectedYamlFile.getAbsolutePath());
        } catch (Exception e) {
            statusLabel.setText("Could not create YAML.");
            log("Could not create YAML: " + e.getMessage());
        }
    }

    private void runTool() {
    }

    private void log(String message) {
        outputArea.appendText(message + "\n");
    }
}
