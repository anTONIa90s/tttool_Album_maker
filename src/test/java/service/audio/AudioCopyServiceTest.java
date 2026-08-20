package service.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioCopyServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void turnsAnExportFolderIntoASiblingAlbumFolder() throws IOException {
        Path exportFolder = Files.createDirectory(temporaryDirectory.resolve("name_export"));
        Files.createFile(exportFolder.resolve("chapter.ogg"));

        File audioFolder = new AudioCopyService().prepareAudioFolderForExistingAlbum(exportFolder.toFile());

        assertEquals(temporaryDirectory.resolve("name_album").resolve("audio").toFile(), audioFolder);
        assertTrue(audioFolder.toPath().resolve("chapter.ogg").toFile().isFile());
    }
}
