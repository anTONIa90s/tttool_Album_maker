package tiptoieditor.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.audio.AudioConvertService;
import service.audio.AudioCopyService;
import service.audio.AudioFileNameService;
import service.tttool.TttoolService;

import java.io.File;

public class MainWindow {

    private TextArea outputArea;

    private TextField productNameField;
    private TextField productIdField;
    private RowConvertAudio rowConvertAudio;
    private RowCreateYaml rowCreateYaml;
    private RowYamlToGme rowYamlToGme;

    private final TttoolService tttoolService = new TttoolService();
    private final AudioCopyService audioCopyService = new AudioCopyService();
    private final AudioConvertService audioConvertService = new AudioConvertService();
    private final AudioFileNameService audioFileNameService = new AudioFileNameService();

    public void show(Stage stage) {
        Label title = new Label("TTTool GUI");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        productNameField = new TextField();
        productNameField.setPromptText("Album name (default: tttoolAlbum)");

        productIdField = new TextField();
        productIdField.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));
        productIdField.setPrefColumnCount(4);
        productIdField.setPromptText("Enter Product ID");

        HBox rowInput = new HBox(10, productNameField, productIdField);
        rowInput.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(productNameField, Priority.ALWAYS);

        Button runButton = new Button("Run tttool");
        HBox runRow = new HBox(10, runButton);
        runRow.setMaxWidth(Double.MAX_VALUE);
        runButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(runButton, Priority.ALWAYS);

        Button selectAudioFolderButton = new Button("Select Audio Folder");
        Label selectedAudioFolderLabel = new Label("No folder selected");
        Button convertAudioButton = new Button("Convert Audio");

        Button selectAlbumFolderButton = new Button("Select Album Folder");
        Label selectedAlbumFolderLabel = new Label("No folder selected");
        Button createYamlButton = new Button("Create YAML");

        Button selectYamlFileButton = new Button("Select YAML file");
        Label selectedYamlFileLabel = new Label("No YAML file selected");
        Button createGmeButton = new Button("Create GME");

        rowConvertAudio = new RowConvertAudio(stage, selectAudioFolderButton, selectedAudioFolderLabel,
                convertAudioButton, audioCopyService, audioConvertService,
                productNameField::getText, this::log);
        rowYamlToGme = new RowYamlToGme(stage, selectYamlFileButton, selectedYamlFileLabel,
                createGmeButton, tttoolService, audioFileNameService, this::log);
        rowCreateYaml = new RowCreateYaml(stage, selectAlbumFolderButton, selectedAlbumFolderLabel,
                createYamlButton, productIdField::getText,
                rowYamlToGme::setSelectedYamlFile, this::log);

        ExpandableSubActions prepAudioPane = new ExpandableSubActions(
                "Only prep audio", selectAudioFolderButton, selectedAudioFolderLabel,
                convertAudioButton);
        ExpandableSubActions createYamlPane = new ExpandableSubActions(
                "Only create YAML", selectAlbumFolderButton, selectedAlbumFolderLabel,
                createYamlButton);
        ExpandableSubActions createGmePane = new ExpandableSubActions(
                "Only create GME", selectYamlFileButton, selectedYamlFileLabel,
                createGmeButton);
        Accordion workflowPanes = new Accordion(prepAudioPane, createYamlPane, createGmePane);

        VBox buttonBox = new VBox(10, rowInput, runRow, workflowPanes);

        outputArea = new TextArea();
        outputArea.setEditable(false);

        TitledPane logPane = new TitledPane("Logs", outputArea);
        logPane.setExpanded(false);
        logPane.setMaxHeight(200);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setTop(title);
        root.setCenter(buttonBox);
        root.setBottom(logPane);
        BorderPane.setMargin(title, new Insets(0, 0, 10, 0));
        BorderPane.setMargin(buttonBox, new Insets(0, 0, 10, 0));

        runButton.setOnAction(e -> runTool());

        Scene scene = new Scene(root, 500, 350);
        stage.setTitle("TTTool Album Creator");
        stage.setScene(scene);
        stage.show();

        javafx.application.Platform.runLater(root::requestFocus);
    }

    private void runTool() {
        rowConvertAudio.runToolCopyAndConvert(audioFolder -> {
            File selectedAlbumFolder = audioFolder.getParentFile();
            rowCreateYaml.setSelectedAlbumFolder(selectedAlbumFolder);
            log("Selected album folder: " + selectedAlbumFolder.getAbsolutePath());

            if (rowCreateYaml.runToolCreateYaml()) {
                rowYamlToGme.runToolCreateGmeFromYaml();
            }
        });
    }

    private void log(String message) {
        Runnable appendMessage = () -> outputArea.appendText(message + "\n");
        if (javafx.application.Platform.isFxApplicationThread()) {
            appendMessage.run();
        } else {
            javafx.application.Platform.runLater(appendMessage);
        }
    }
}
