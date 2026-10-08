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
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

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
