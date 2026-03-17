/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.recording;

import net.emustudio.plugins.device.zxspectrum.ula.audio.Beeper;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static net.emustudio.plugins.device.zxspectrum.ula.recording.VideoRecorder.TEMP_AUDIO_FILE_PREFIX;
import static net.emustudio.plugins.device.zxspectrum.ula.recording.VideoRecorder.TEMP_VIDEO_FILE_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class VideoRecorderTest {
    private static final int FRAME_SIZE = 32;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testExportsMp4WhenSavingAsMp4() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame());

        Path output = temporaryFolder.newFile("capture.mp4").toPath();
        recorder.stop(Optional.of(output));

        assertIsoBaseMediaFile(output, 0);
    }

    @Test
    public void testExportsAudioTrackWhenAudioWasRecorded() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame());
        byte[] audio = createAudioFrames(960);
        recorder.captureAudio(audio);

        Path output = temporaryFolder.newFile("capture-with-audio.mp4").toPath();
        recorder.stop(Optional.of(output));

        assertIsoBaseMediaFile(output, 1);
    }

    @Test
    public void testDiscardsRecordingImmediatelyAfterIoError() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame(FRAME_SIZE));
        recorder.captureVideo(createFrame(FRAME_SIZE + 1));

        Path output = temporaryFolder.newFile("discarded.mp4").toPath();
        IOException error = assertThrows(IOException.class, () -> recorder.stop(Optional.of(output)));

        assertEquals("Recording was discarded after an earlier I/O error", error.getMessage());
    }

    @Test
    public void testConstructorRejectsZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoRecorder(0, FRAME_SIZE, 69_888, 3_500_000, 48_000));
    }

    @Test
    public void testConstructorRejectsNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoRecorder(FRAME_SIZE, -1, 69_888, 3_500_000, 48_000));
    }

    @Test
    public void testConstructorRejectsZeroVideoScale() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 0, 3_500_000, 48_000));
    }

    @Test
    public void testConstructorRejectsNegativeVideoRate() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, -1, 48_000));
    }

    @Test
    public void testConstructorRejectsZeroAudioSampleRate() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 0));
    }

    @Test
    public void testStopWithEmptyOptionalDiscardsRecording() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame());
        recorder.stop(Optional.empty());
        // no exception, recording silently discarded
    }

    @Test
    public void testStopWithNoVideoFramesThrows() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        Path output = temporaryFolder.newFile("empty.mp4").toPath();

        IOException error = assertThrows(IOException.class, () -> recorder.stop(Optional.of(output)));
        assertEquals("Recording contains no video frames", error.getMessage());
    }

    @Test
    public void testMultipleVideoFramesProduceValidMp4() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        for (int i = 0; i < 5; i++) {
            recorder.captureVideo(createFrame());
        }

        Path output = temporaryFolder.newFile("multi-frame.mp4").toPath();
        recorder.stop(Optional.of(output));

        assertIsoBaseMediaFile(output, 0);
        assertTrue(Files.size(output) > 0);
    }

    @Test
    public void testMultipleVideoFramesWithAudioProduceValidMp4() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        for (int i = 0; i < 3; i++) {
            recorder.captureVideo(createFrame());
            recorder.captureAudio(createAudioFrames(480));
        }

        Path output = temporaryFolder.newFile("multi-av.mp4").toPath();
        recorder.stop(Optional.of(output));

        assertIsoBaseMediaFile(output, 1);
    }

    @Test
    public void testMisalignedAudioAbortsRecording() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame());
        // 3 bytes is not a whole PCM frame (frame size = 4 bytes)
        recorder.captureAudio(new byte[]{1, 2, 3});

        Path output = temporaryFolder.newFile("misaligned.mp4").toPath();
        IOException error = assertThrows(IOException.class, () -> recorder.stop(Optional.of(output)));
        assertEquals("Recording was discarded after an earlier I/O error", error.getMessage());
    }

    @Test
    public void testCaptureVideoAfterStopIsIgnored() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame());

        Path output = temporaryFolder.newFile("after-stop.mp4").toPath();
        recorder.stop(Optional.of(output));

        // These should be silently ignored — no exception
        recorder.captureVideo(createFrame());
        recorder.captureAudio(createAudioFrames(10));
    }

    @Test
    public void testCaptureAudioWithNullThrowsNpe() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        assertThrows(NullPointerException.class, () -> recorder.captureAudio(null));
    }

    @Test
    public void testStopWithNullTargetThrowsNpe() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        assertThrows(NullPointerException.class, () -> recorder.stop(null));
    }

    @Test
    public void testTempFilesAreCleanedUpAfterExport() throws IOException {
        long before = countUlaTempFiles();

        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame());
        recorder.captureAudio(createAudioFrames(480));

        Path output = temporaryFolder.newFile("cleanup.mp4").toPath();
        recorder.stop(Optional.of(output));

        assertTrue(Files.exists(output));
        assertEquals("Temp files should be cleaned up", before, countUlaTempFiles());
    }

    @Test
    public void testDiscardDeletesTempFiles() throws IOException {
        long before = countUlaTempFiles();

        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame());
        recorder.stop(Optional.empty());

        assertEquals("Temp files should be deleted on discard", before, countUlaTempFiles());
    }

    @Test
    public void testExportCreatesParentDirectories() throws IOException {
        VideoRecorder recorder = new VideoRecorder(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
        recorder.captureVideo(createFrame());

        Path output = temporaryFolder.getRoot().toPath().resolve("sub/dir/nested.mp4");
        assertFalse(Files.exists(output.getParent()));

        recorder.stop(Optional.of(output));

        assertTrue(Files.exists(output));
        assertIsoBaseMediaFile(output, 0);
    }

    private static BufferedImage createFrame() {
        return createFrame(FRAME_SIZE);
    }

    private static BufferedImage createFrame(int size) {
        BufferedImage frame = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = frame.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, size, size);
        graphics.setColor(Color.YELLOW);
        graphics.fillRect(8, 8, size / 2, size / 2);
        graphics.dispose();
        return frame;
    }

    private static String ascii(byte[] bytes, int offset) {
        return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
    }

    private static byte[] createAudioFrames(int frameCount) {
        ByteBuffer buffer = ByteBuffer.allocate(frameCount * Beeper.FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frameCount; i++) {
            short sample = (short) ((i % 32) * 512);
            buffer.putShort(sample);
            buffer.putShort(sample);
        }
        return buffer.array();
    }

    private static void assertIsoBaseMediaFile(Path output, int expectedAudioTracks) throws IOException {
        byte[] bytes = Files.readAllBytes(output);
        assertEquals("ftyp", ascii(bytes, 4));
        assertTrue(findAscii(bytes, "mdat") >= 0);
        assertTrue(findAscii(bytes, "moov") > findAscii(bytes, "mdat"));

        FileChannelWrapper channel = NIOUtils.readableChannel(output.toFile());
        try {
            MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(channel);
            try {
                assertEquals(1, demuxer.getVideoTracks().size());
                assertEquals(expectedAudioTracks, demuxer.getAudioTracks().size());
            } finally {
                demuxer.close();
            }
        } finally {
            channel.close();
        }
    }

    private static long countUlaTempFiles() throws IOException {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        try (var stream = Files.list(tempDir)) {
            return stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(TEMP_VIDEO_FILE_PREFIX) || name.startsWith(TEMP_AUDIO_FILE_PREFIX);
            }).count();
        }
    }

    private static int findAscii(byte[] bytes, String value) {
        byte[] pattern = value.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i <= bytes.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (bytes[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }
}
