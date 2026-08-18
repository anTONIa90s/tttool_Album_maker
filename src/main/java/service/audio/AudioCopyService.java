package service.audio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AudioCopyService {

    public File prepareAudioFolder(File sourceFolder) {
        return prepareAudioFolder(sourceFolder, "tttoolAlbum");
    }

    /**
     * Copies audio files into an {@code audio} folder below the given album name.
     * An empty album name falls back to {@code tttoolAlbum}.
     */
    public File prepareAudioFolder(File sourceFolder, String albumName) {
        String resolvedAlbumName = albumName == null || albumName.isBlank()
                ? "tttoolAlbum"
                : albumName.trim();

        // Create <album name>/audio structure
        File albumFolder = new File(sourceFolder, resolvedAlbumName);
        File audioFolder = new File(albumFolder, "audio");

        if (!audioFolder.exists()) {
            audioFolder.mkdirs();
        }

        File[] files = sourceFolder.listFiles();
        if (files == null)
            return audioFolder;

        for (File file : files) {
            if (!file.isFile())
                continue;

            String name = file.getName().toLowerCase();

            if (name.endsWith(".mp3") || name.endsWith(".ogg")) {

                File target = new File(audioFolder, file.getName());

                try {
                    Files.copy(
                            file.toPath(),
                            target.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Copied: " + file.getName());

                } catch (IOException e) {
                    System.err.println("Failed to copy: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        return audioFolder;
    }
}
