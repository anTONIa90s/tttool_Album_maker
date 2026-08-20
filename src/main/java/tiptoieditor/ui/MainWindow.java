package tiptoieditor.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import service.audio.AudioConvertService;
import service.audio.AudioCopyService;
import service.audio.AudioFileNameService;
import service.tonie.TonieAudioExportService;
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
    private final AudioConvertService audioConvertService = new AudioConvertService(this::log);
    private final AudioFileNameService audioFileNameService = new AudioFileNameService();
    private final TonieAudioExportService tonieAudioExportService = new TonieAudioExportService();

    public void show(Stage stage) {
        Label title = new Label("TTTool GUI");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        productNameField = new TextField();
        productNameField.setPromptText("Album name (default: tttoolAlbum)");

        productIdField = new TextField();
        productIdField.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));
        productIdField.setPrefColumnCount(5);
        productIdField.setPromptText("Enter Product ID");

        Button selectDirectoryButton = new Button("Select Directory");
        HBox rowInput = new HBox(10, selectDirectoryButton, productNameField, productIdField);
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

        Button selectTonieFileButton = new Button("Select Tonie File");
        Label selectedTonieFileLabel = new Label("No file selected");
        Button exportTonieAudioButton = new Button("Export OGG");

        Button selectGmeFolderButton = new Button("Select GME Folder");
        Label selectedGmeFolderLabel = new Label("No folder selected");
        Button listGmeProductIdsButton = new Button("List Product IDs");
        ObservableList<RowListGmeProductIds.ProductIdTableRow> productIdRows = FXCollections.observableArrayList();
        TableView<RowListGmeProductIds.ProductIdTableRow> productIdTable = createProductIdTable(productIdRows);

        rowConvertAudio = new RowConvertAudio(stage, selectAudioFolderButton, selectedAudioFolderLabel,
                convertAudioButton, audioCopyService, audioConvertService,
                productNameField::getText, this::log);
        selectedAudioFolderLabel.textProperty().addListener((observable, oldValue, newValue) ->
                productNameField.setText(newValue));
        selectDirectoryButton.setOnAction(e -> rowConvertAudio.selectAudioFolder(stage));
        rowYamlToGme = new RowYamlToGme(stage, selectYamlFileButton, selectedYamlFileLabel,
                createGmeButton, tttoolService, audioFileNameService, this::log);
        rowCreateYaml = new RowCreateYaml(stage, selectAlbumFolderButton, selectedAlbumFolderLabel,
                createYamlButton, productIdField::getText,
                rowYamlToGme::setSelectedYamlFile, this::log);
        new RowExportTonieAudio(stage, selectTonieFileButton, selectedTonieFileLabel,
                exportTonieAudioButton, tonieAudioExportService, productNameField::getText, this::log);
        new RowListGmeProductIds(stage, selectGmeFolderButton, selectedGmeFolderLabel,
                listGmeProductIdsButton, tttoolService, productIdRows, this::log);

        ExpandableSubActions prepAudioPane = new ExpandableSubActions(
                "Only prep audio", selectAudioFolderButton, selectedAudioFolderLabel,
                convertAudioButton);
        ExpandableSubActions createYamlPane = new ExpandableSubActions(
                "Only create YAML", selectAlbumFolderButton, selectedAlbumFolderLabel,
                createYamlButton);
        ExpandableSubActions createGmePane = new ExpandableSubActions(
                "Only create GME", selectYamlFileButton, selectedYamlFileLabel,
                createGmeButton);
        ExpandableSubActions exportToniePane = new ExpandableSubActions(
                "Export Tonie audio", selectTonieFileButton, selectedTonieFileLabel,
                exportTonieAudioButton);
        ExpandableSubActions listGmeProductIdsPane = new ExpandableSubActions(
                "List GME Product IDs", selectGmeFolderButton, selectedGmeFolderLabel,
                listGmeProductIdsButton, productIdTable);
        Accordion workflowPanes = new Accordion(prepAudioPane, createYamlPane, createGmePane, exportToniePane,
                listGmeProductIdsPane);

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

        Scene scene = new Scene(root, 500, 400);
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

    private TableView<RowListGmeProductIds.ProductIdTableRow> createProductIdTable(
            ObservableList<RowListGmeProductIds.ProductIdTableRow> productIdRows) {
        TableColumn<RowListGmeProductIds.ProductIdTableRow, String> gmeColumn = new TableColumn<>("gme");
        gmeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().gme()));
        gmeColumn.setPrefWidth(300);

        TableColumn<RowListGmeProductIds.ProductIdTableRow, Integer> productIdColumn = new TableColumn<>("Product ID");
        productIdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().productId()).asObject());
        productIdColumn.setPrefWidth(130);

        TableView<RowListGmeProductIds.ProductIdTableRow> table = new TableView<>(productIdRows);
        table.getColumns().add(gmeColumn);
        table.getColumns().add(productIdColumn);
        table.getSortOrder().add(gmeColumn);
        table.setPlaceholder(new Label("No Product IDs listed yet"));
        table.setPrefHeight(200);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        return table;
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
