package tiptoieditor.ui;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Shared folder-selection dialog for UI rows.
 */
public final class FolderSelectionDialog {

    private FolderSelectionDialog() {
    }

    public static File chooseFolder(Stage stage, File currentFolder) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");

        if (currentFolder != null && currentFolder.exists()) {
            File parentFolder = currentFolder.getParentFile();
            directoryChooser.setInitialDirectory(parentFolder != null ? parentFolder : currentFolder);
        }

        return directoryChooser.showDialog(stage);
    }
}
