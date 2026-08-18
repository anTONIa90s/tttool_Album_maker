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
import service.tttool.TttoolService;

import java.io.File;

public class MainWindow {

    private TextArea outputArea;
    private Label selectedFolderLabel;
    private Label selectedYamlFileLabel;
    private Label statusLabel;

    private File selectedFolder;
    private File selectedYamlFile;

    private final TttoolService tttoolService = new TttoolService();

    private final AudioCopyService audioCopyService = new AudioCopyService();
    private final AudioConvertService audioConvertService = new AudioConvertService();

    public void show(Stage stage) {

        // --- Title ---
        Label title = new Label("TTTool GUI");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // --- Select Folder button + folder label ---
        Button selectFolderButton = new Button("Select Folder");
        selectedFolderLabel = new Label("No folder selected");
        HBox loadRow = new HBox(10, selectFolderButton, selectedFolderLabel);

        // --- Select File button + folder label ---
        Button selectYamlFileButton = new Button("Select yaml file");
        selectedYamlFileLabel = new Label("No YAML file selected");
        HBox loadRowYaml = new HBox(10, selectYamlFileButton, selectedYamlFileLabel);

        // --- Run button ---
        Button runButton = new Button("Run tttool");
        statusLabel = new Label(""); // initially empty
        HBox runRow = new HBox(10, runButton, statusLabel);

        // Stack buttons vertically
        VBox buttonBox = new VBox(10, loadRow, loadRowYaml, runRow);

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

        selectFolderButton.setOnAction(e -> loadFolder(stage));
        selectYamlFileButton.setOnAction(e -> loadYamlFile(stage));
        // runButton.setOnAction(e -> runToolCreateGmeFromYaml());
        runButton.setOnAction(e -> runToolCopyAndConvert());

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

    private void loadFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");

        // start folder search at former parent-directory
        if (selectedFolder != null && selectedFolder.exists()) {
            directoryChooser.setInitialDirectory(selectedFolder.getParentFile());
        }

        File folder = directoryChooser.showDialog(stage);

        if (folder != null) {
            selectedFolder = folder;

            // show folder name next to button
            selectedFolderLabel.setText(folder.getName());
            statusLabel.setText("");

            log("Selected folder: " + folder.getAbsolutePath());
        }
    }

    private void runToolCreateGmeFromYaml() {
        if (selectedYamlFile == null) {
            statusLabel.setText("Please select a yaml file first.");
            log("Please select a YAML file first.");
            return;
        }
        log("Processing folder: " + selectedYamlFile.getAbsolutePath());

        try {

            String output = tttoolService.assemble(selectedYamlFile.toPath());

            System.out.println(output);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runToolCopyAndConvert() {
        if (selectedFolder == null) {
            statusLabel.setText("Please select a folder first.");
            log("Please select a folder first.");
            return;
        }

        log("Preparing folder structure...");
        statusLabel.setText("⏳ Processing...");

        new Thread(() -> {
            // Step 1: copy files
            File audioFolder = audioCopyService.prepareAudioFolder(selectedFolder);
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

    private void log(String message) {
        outputArea.appendText(message + "\n");
    }
}