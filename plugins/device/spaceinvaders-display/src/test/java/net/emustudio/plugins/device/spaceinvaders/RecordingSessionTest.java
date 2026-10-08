/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

public class RecordingSessionTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void stopDrainsFramesAndExportsMatchingVideoAndAudioDurations() throws Exception {
        RecordingSession session = new RecordingSession(32, 32);
        byte[] audio = new byte[RecordingSession.AUDIO_FRAMES_PER_VIDEO_FRAME * 4];
        ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 2000).putShort((short) 2000);
        for (int i = 0; i < 60; i++) {
            session.capture(frame(32), audio);
        }
        Path output = temporaryFolder.getRoot().toPath().resolve("capture.mp4");
        session.stop(output);
        try (var channel = NIOUtils.readableChannel(output.toFile());
             var demuxer = MP4Demuxer.createMP4Demuxer(channel)) {
            assertEquals(1, demuxer.getVideoTracks().size());
            assertEquals(1, demuxer.getAudioTracks().size());
            var video = demuxer.getVideoTrack().getMeta();
            var sound = demuxer.getAudioTracks().get(0).getMeta();
            assertEquals(60, video.getTotalFrames());
            assertEquals(1.0, video.getTotalDuration(), 0.001);
            assertEquals(1.0, sound.getTotalDuration(), 0.001);
            assertEquals(48000, sound.getAudioCodecMeta().getSampleRate());
            assertEquals(2, sound.getAudioCodecMeta().getChannelCount());
            assertEquals(32, video.getVideoCodecMeta().getSize().getWidth());
        }
        session.capture(frame(32), audio);
        session.stop(null);
        assertTrue(Files.exists(output));
    }

    @Test
    public void discardDeletesTemporaryVideoAndAudio() throws Exception {
        RecordingSession session = new RecordingSession(32, 32);
        Field recorderField = RecordingSession.class.getDeclaredField("recorder");
        recorderField.setAccessible(true);
        VideoRecorder recorder = (VideoRecorder) recorderField.get(session);
        Path video = temporaryFile(recorder, "tempVideoFile");
        Path audio = temporaryFile(recorder, "tempAudioFile");
        session.capture(frame(32), new byte[3200]);
        session.stop(null);
        assertFalse(Files.exists(video));
        assertFalse(Files.exists(audio));
    }

    @Test
    public void brokenCaptureDiscardsFilesAndReportsExportFailure() throws Exception {
        VideoRecorder recorder = new VideoRecorder(32, 32, 1, 60, 48000);
        Path video = temporaryFile(recorder, "tempVideoFile");
        Path audio = temporaryFile(recorder, "tempAudioFile");
        recorder.captureVideo(frame(34));
        Path output = temporaryFolder.getRoot().toPath().resolve("broken.mp4");
        assertThrows(IOException.class, () -> recorder.stop(output));
        assertFalse(Files.exists(output));
        assertFalse(Files.exists(video));
        assertFalse(Files.exists(audio));
    }

    @Test
    public void videoIsEncodedBeforeStopAndTemporaryAudioRemainsSmallAndLossless() throws Exception {
        int width = 224;
        int height = 256;
        int frames = 120;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        byte[] pcm = new byte[RecordingSession.AUDIO_FRAMES_PER_VIDEO_FRAME * 4];
        Arrays.fill(pcm, (byte) 7);
        VideoRecorder recorder = new VideoRecorder(width, height, 1, 60, 48000);
        Path video = temporaryFile(recorder, "tempVideoFile");
        Path audio = temporaryFile(recorder, "tempAudioFile");
        try {
            for (int i = 0; i < frames; i++) {
                image.setRGB(i, 0, Color.GREEN.getRGB());
                recorder.captureVideo(image);
                recorder.captureAudio(pcm);
            }
            // Video packets already exist before Stop; Save must not encode the frames again.
            assertTrue(Files.size(video) > 100);
            assertEquals(0x66747970, ByteBuffer.wrap(Files.readAllBytes(video)).getInt(4)); // MP4 ftyp
            closeTemporaryOutput(recorder, "tempAudioFileOutputStream");
            assertTrue(Files.size(video) < (long) width * height * Integer.BYTES * frames / 100);
            assertTrue(Files.size(audio) < (long) pcm.length * frames / 10);
            try (var audioInput = new GZIPInputStream(Files.newInputStream(audio))) {
                for (int i = 0; i < frames; i++) {
                    assertArrayEquals(pcm, audioInput.readNBytes(pcm.length));
                }
                assertEquals(-1, audioInput.read());
            }
            Path output = temporaryFolder.getRoot().toPath().resolve("already-encoded.mp4");
            recorder.stop(output);
            try (var channel = NIOUtils.readableChannel(output.toFile());
                 var demuxer = MP4Demuxer.createMP4Demuxer(channel)) {
                assertEquals(frames, demuxer.getVideoTrack().getMeta().getTotalFrames());
                assertEquals(2.0, demuxer.getAudioTracks().get(0).getMeta().getTotalDuration(), 0.001);
            }
        } finally {
            recorder.stop(null);
        }
        assertFalse(Files.exists(video));
        assertFalse(Files.exists(audio));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fullQueueRejectsFramesWithoutBlockingAndDiscardsFailedRecording() throws Exception {
        RecordingSession session = new RecordingSession(32, 32);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Field queueField = RecordingSession.class.getDeclaredField("queue");
        queueField.setAccessible(true);
        BlockingQueue<Runnable> queue = (BlockingQueue<Runnable>) queueField.get(session);
        queue.put(() -> {
            entered.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Path output = temporaryFolder.getRoot().toPath().resolve("overloaded.mp4");
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            int capacity = queue.remainingCapacity();
            for (int i = 0; i < capacity; i++) {
                assertTrue(session.capture(frame(32), new byte[3200]));
            }
            assertFalse(session.capture(frame(32), new byte[3200]));
            assertNotNull(session.getFailure());
            release.countDown();
            assertThrows(IOException.class, () -> session.stop(output));
            assertFalse(Files.exists(output));
        } finally {
            release.countDown();
            session.stop(null);
        }
    }

    private static void closeTemporaryOutput(VideoRecorder recorder, String name) throws Exception {
        Field field = VideoRecorder.class.getDeclaredField(name);
        field.setAccessible(true);
        ((OutputStream) field.get(recorder)).close();
    }

    private static Path temporaryFile(VideoRecorder recorder, String name) throws Exception {
        Field field = VideoRecorder.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Path) field.get(recorder);
    }

    private static BufferedImage frame(int size) {
        BufferedImage frame = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        frame.setRGB(0, 0, Color.GREEN.getRGB());
        return frame;
    }
}
