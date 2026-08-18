package service.audio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AudioCopyService {

    public File prepareAudioFolder(File sourceFolder) {
        // Create tttoolAlbum/audio structure
        File albumFolder = new File(sourceFolder, "tttoolAlbum");
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