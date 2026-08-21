package service.yaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerateYamlServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void usesTheBaseNameForYamlFilesCreatedInAnAlbumFolder() throws IOException {
        Path albumFolder = Files.createDirectory(temporaryDirectory.resolve("name_album"));
        Path audioFolder = Files.createDirectory(albumFolder.resolve("audio"));
        Files.createFile(audioFolder.resolve("chapter.mp3"));

        GenerateYamlService.GeneratedYamlFiles files = new GenerateYamlService().generate(123, albumFolder);

        assertEquals("name.yaml", files.yamlFile().getFileName().toString());
        assertEquals("name.codes.yaml", files.codesFile().getFileName().toString());
    }

    @Test
    void usesTheProvidedTitleForYamlFilesCreatedInAnAlbumFolder() throws IOException {
        Path albumFolder = Files.createDirectory(temporaryDirectory.resolve("name_album"));
        Path audioFolder = Files.createDirectory(albumFolder.resolve("audio"));
        Files.createFile(audioFolder.resolve("chapter.mp3"));

        GenerateYamlService.GeneratedYamlFiles files = new GenerateYamlService().generate(890, albumFolder, "name_890");

        assertEquals("name_890.yaml", files.yamlFile().getFileName().toString());
        assertEquals("name_890.codes.yaml", files.codesFile().getFileName().toString());
    }
}
