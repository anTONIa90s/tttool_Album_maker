package tiptoieditor.ui;

import javafx.scene.control.ToggleButton;
import service.workflow.AlbumFolderWorkflowResolver;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Continues the UI workflow after audio preparation or when opening an existing album. */
public class AlbumWorkflowContinuation {

    private final RowCreateYaml rowCreateYaml;
    private final RowYamlToGme rowYamlToGme;
    private final ToggleButton createNewYamlToggle;
    private final Supplier<File> selectedFolderSupplier;
    private final AlbumFolderWorkflowResolver workflowResolver;
    private final Consumer<String> logger;

    public AlbumWorkflowContinuation(RowCreateYaml rowCreateYaml, RowYamlToGme rowYamlToGme,
                                     ToggleButton createNewYamlToggle, Supplier<File> selectedFolderSupplier,
                                     AlbumFolderWorkflowResolver workflowResolver, Consumer<String> logger) {
        this.rowCreateYaml = rowCreateYaml;
        this.rowYamlToGme = rowYamlToGme;
        this.createNewYamlToggle = createNewYamlToggle;
        this.selectedFolderSupplier = selectedFolderSupplier;
        this.workflowResolver = workflowResolver;
        this.logger = logger;
    }

    public void continueFromAudioFolder(File audioFolder) {
        File albumFolder = audioFolder.getParentFile();
        rowCreateYaml.setSelectedAlbumFolder(albumFolder);
        logger.accept("Selected album folder: " + albumFolder.getAbsolutePath());

        if (rowCreateYaml.runToolCreateYaml()) {
            rowYamlToGme.runToolCreateGmeFromYaml();
        }
    }

    public void continueFromExistingAlbum(File albumFolder, File yamlFile) {
        rowCreateYaml.setSelectedAlbumFolder(albumFolder);
        if (yamlFile != null && !createNewYamlToggle.isSelected()) {
            rowYamlToGme.setSelectedYamlFile(yamlFile);
            logger.accept("Using existing YAML: " + yamlFile.getAbsolutePath());
            rowYamlToGme.runToolCreateGmeFromYaml();
            return;
        }

        if (rowCreateYaml.runToolCreateYaml()) {
            rowYamlToGme.runToolCreateGmeFromYaml();
        }
    }

    public void updateCreateNewYamlToggle() {
        boolean showToggle = workflowResolver.hasExistingAlbumYaml(selectedFolderSupplier.get());
        createNewYamlToggle.setSelected(false);
        createNewYamlToggle.setManaged(showToggle);
        createNewYamlToggle.setVisible(showToggle);
    }
}
