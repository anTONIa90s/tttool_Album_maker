package service.audio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AudioCopyService {

    private static final String EXPORT_SUFFIX = "_export";
    private static final String ALBUM_SUFFIX = "_album";

    public File prepareAudioFolder(File sourceFolder) {
        return prepareAudioFolder(sourceFolder, "tttoolAlbum");
    }

    /**
     * Copies audio files into an {@code audio} folder below {@code <album-name>_album}.
     * An empty album name falls back to {@code tttoolAlbum}.
     */
    public File prepareAudioFolder(File sourceFolder, String albumName) {
        String resolvedAlbumName = albumName == null || albumName.isBlank()
                ? "tttoolAlbum"
                : albumName.trim();

        String baseAlbumName = withoutExportSuffix(resolvedAlbumName);
        File albumFolder = sourceFolder.getName().endsWith(EXPORT_SUFFIX)
                ? new File(sourceFolder.getParentFile(), baseAlbumName + ALBUM_SUFFIX)
                : new File(sourceFolder, baseAlbumName + ALBUM_SUFFIX);
        return copyAudioFiles(sourceFolder, albumFolder);
    }

    /** Copies audio files from an existing album folder into its {@code audio} subdirectory. */
    public File prepareAudioFolderForExistingAlbum(File albumFolder) {
        File targetAlbumFolder = albumFolder.getName().endsWith(EXPORT_SUFFIX)
                ? new File(albumFolder.getParentFile(), withoutExportSuffix(albumFolder.getName()) + ALBUM_SUFFIX)
                : albumFolder;
        return copyAudioFiles(albumFolder, targetAlbumFolder);
    }

    private String withoutExportSuffix(String name) {
        return name.endsWith(EXPORT_SUFFIX)
                ? name.substring(0, name.length() - EXPORT_SUFFIX.length())
                : name;
    }

    private File copyAudioFiles(File sourceFolder, File albumFolder) {
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
