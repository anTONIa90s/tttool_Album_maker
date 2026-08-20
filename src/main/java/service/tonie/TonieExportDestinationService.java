package service.tonie;

import java.io.File;
import java.io.IOException;

/** Creates the standard album destination used for audio exported from a Tonie file. */
public class TonieExportDestinationService {

    /** Creates {@code <tonie-parent>/<product-name-or-parent-name>_export}. */
    public File createExportFolder(File tonieFile, String productName) throws IOException {
        File parentFolder = tonieFile.getParentFile();
        if (parentFolder == null) {
            throw new IOException("Tonie file has no parent folder: " + tonieFile.getAbsolutePath());
        }

        String folderName = productName == null || productName.isBlank()
                ? parentFolder.getName()
                : productName.trim();
        File exportFolder = new File(parentFolder, folderName + "_export");
        if (exportFolder.isDirectory() || exportFolder.mkdirs()) {
            return exportFolder;
        }
        throw new IOException("Could not create export folder: " + exportFolder.getAbsolutePath());
    }
}
