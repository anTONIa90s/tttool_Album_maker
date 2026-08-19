package service.tonie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TonieAudioExportServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsOneChapterWithTheParentDirectoryAsTheDefaultPrefix() throws IOException {
        Path tonieFile = temporaryDirectory.resolve("500304E0");
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.writeBytes(new byte[]{0, 0, 0, 3, 0, 0, 0});
        file.writeBytes(oggPage(0, 2, 0, "OpusHead".getBytes()));
        file.writeBytes(oggPage(1, 0, 0, "OpusTags".getBytes()));
        file.writeBytes(oggPage(2, 0, 960, "audio".getBytes()));
        Files.write(tonieFile, file.toByteArray());

        TonieAudioExportService.ExportResult result = new TonieAudioExportService()
                .export(tonieFile, temporaryDirectory);

        assertEquals(7, result.audioOffset());
        assertEquals(temporaryDirectory.getFileName() + "-01.ogg", result.exportedFile().getFileName().toString());
        assertEquals(List.of(0, 1, 2), pageSequenceNumbers(result.exportedFile()));
    }

    @Test
    void rejectsFilesWithoutAnOggPayload() throws IOException {
        Path tonieFile = temporaryDirectory.resolve("invalid-tonie");
        Files.write(tonieFile, new byte[]{0, 0, 0, 1, 0, 1, 2, 3});

        assertThrows(IOException.class, () -> new TonieAudioExportService().findAudioOffset(tonieFile));
    }

    @Test
    void splitsChaptersIntoSelfContainedOggStreamsUsingTheOptionalTitle() throws IOException {
        Path tonieFile = temporaryDirectory.resolve("500304E0");
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.writeBytes(new byte[]{0, 0, 0, 4, 0x22, 0x02, 0x00, 0x03}); // chapter pages 0 and 3
        file.writeBytes(oggPage(0, 2, 0, "OpusHead".getBytes()));
        file.writeBytes(oggPage(1, 0, 0, "OpusTags".getBytes()));
        file.writeBytes(oggPage(2, 0, 960, "chapter one".getBytes()));
        file.writeBytes(oggPage(3, 0, 1920, "chapter two".getBytes()));
        Files.write(tonieFile, file.toByteArray());

        List<TonieAudioExportService.ExportResult> results = new TonieAudioExportService()
                .export(tonieFile, temporaryDirectory, "My: Album");

        assertEquals(List.of("My_ Album-01.ogg", "My_ Album-02.ogg"),
                results.stream().map(result -> result.exportedFile().getFileName().toString()).toList());
        assertEquals(List.of(0, 1, 2), pageSequenceNumbers(results.get(0).exportedFile()));
        assertEquals(List.of(0, 1, 2), pageSequenceNumbers(results.get(1).exportedFile()));
        assertEquals(0x04, pageHeaderTypes(results.get(0).exportedFile()).getLast() & 0x04);
        assertEquals(0x04, pageHeaderTypes(results.get(1).exportedFile()).getLast() & 0x04);
        assertEquals(960, lastGranulePosition(results.get(1).exportedFile()));
    }

    private byte[] oggPage(int sequenceNumber, int headerType, long granulePosition, byte[] body) {
        byte[] page = new byte[28 + body.length];
        page[0] = 'O';
        page[1] = 'g';
        page[2] = 'g';
        page[3] = 'S';
        page[5] = (byte) headerType;
        for (int i = 0; i < Long.BYTES; i++) {
            page[6 + i] = (byte) (granulePosition >>> (i * 8));
        }
        for (int i = 0; i < Integer.BYTES; i++) {
            page[18 + i] = (byte) (sequenceNumber >>> (i * 8));
        }
        page[26] = 1;
        page[27] = (byte) body.length;
        System.arraycopy(body, 0, page, 28, body.length);
        return page;
    }

    private List<Integer> pageSequenceNumbers(Path ogg) throws IOException {
        return pageHeaders(ogg).stream().map(header -> littleEndianInt(header, 18)).toList();
    }

    private List<Integer> pageHeaderTypes(Path ogg) throws IOException {
        return pageHeaders(ogg).stream().map(header -> Byte.toUnsignedInt(header[5])).toList();
    }

    private long lastGranulePosition(Path ogg) throws IOException {
        List<byte[]> headers = pageHeaders(ogg);
        byte[] header = headers.getLast();
        long value = 0;
        for (int i = 7; i >= 0; i--) {
            value = (value << 8) | Byte.toUnsignedInt(header[6 + i]);
        }
        return value;
    }

    private List<byte[]> pageHeaders(Path ogg) throws IOException {
        byte[] data = Files.readAllBytes(ogg);
        var headers = new java.util.ArrayList<byte[]>();
        for (int position = 0; position < data.length; ) {
            byte[] header = java.util.Arrays.copyOfRange(data, position, position + 27);
            headers.add(header);
            int bodyLength = 0;
            for (int segment = 0; segment < Byte.toUnsignedInt(header[26]); segment++) {
                bodyLength += Byte.toUnsignedInt(data[position + 27 + segment]);
            }
            position += 27 + Byte.toUnsignedInt(header[26]) + bodyLength;
        }
        return headers;
    }

    private int littleEndianInt(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset])
                | (Byte.toUnsignedInt(data[offset + 1]) << 8)
                | (Byte.toUnsignedInt(data[offset + 2]) << 16)
                | (Byte.toUnsignedInt(data[offset + 3]) << 24);
    }
}
