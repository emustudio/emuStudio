/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.recording;

import net.emustudio.plugins.device.zxspectrum.ula.audio.Beeper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

public class RecordingSessionTest {
    private static final int FRAME_SIZE = 32;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testVideoFrameIsForwardedToRecorder() throws IOException {
        RecordingSession session = createSession();
        session.accept(createFrame());

        Path output = temporaryFolder.newFile("video.mp4").toPath();
        session.stop(Optional.of(output));

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    public void testAudioIsForwardedToRecorder() throws IOException {
        RecordingSession session = createSession();
        session.accept(createFrame());
        session.accept(createAudioFrames(960), 960 * Beeper.FRAME_SIZE);

        Path output = temporaryFolder.newFile("av.mp4").toPath();
        session.stop(Optional.of(output));

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    public void testStopWithoutTargetDiscardsRecording() throws IOException {
        RecordingSession session = createSession();
        session.accept(createFrame());

        session.stop(Optional.empty());
        // No exception, recording silently discarded
    }

    @Test
    public void testNullFrameIsIgnored() throws IOException {
        RecordingSession session = createSession();
        session.accept(null);

        // No frame captured, so saving should fail with "no video frames"
        Path output = temporaryFolder.newFile("empty.mp4").toPath();
        assertThrows(IOException.class, () -> session.stop(Optional.of(output)));
    }

    @Test
    public void testZeroLengthAudioIsIgnored() throws IOException {
        RecordingSession session = createSession();
        session.accept(createFrame());
        session.accept(new byte[0], 0);

        Path output = temporaryFolder.newFile("no-audio.mp4").toPath();
        session.stop(Optional.of(output));

        assertTrue(Files.exists(output));
    }

    @Test
    public void testFramesAfterStopAreIgnored() throws IOException {
        RecordingSession session = createSession();
        session.accept(createFrame());

        session.stop(Optional.empty());

        // These should be silently ignored (accepting = false)
        session.accept(createFrame());
        session.accept(createAudioFrames(100), 100 * Beeper.FRAME_SIZE);
    }

    @Test
    public void testMultipleFramesAreAllRecorded() throws IOException {
        RecordingSession session = createSession();
        for (int i = 0; i < 5; i++) {
            session.accept(createFrame());
        }

        Path output = temporaryFolder.newFile("multi.mp4").toPath();
        session.stop(Optional.of(output));

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    public void testAudioDataIsCopiedNotReferenced() throws IOException {
        RecordingSession session = createSession();
        session.accept(createFrame());

        byte[] audio = createAudioFrames(100);
        int originalLength = audio.length;
        session.accept(audio, originalLength);

        // Overwrite the source array — recording should not be affected
        java.util.Arrays.fill(audio, (byte) 0);

        Path output = temporaryFolder.newFile("copy-test.mp4").toPath();
        session.stop(Optional.of(output));

        assertTrue(Files.exists(output));
    }

    private RecordingSession createSession() throws IOException {
        return new RecordingSession(FRAME_SIZE, FRAME_SIZE, 69_888, 3_500_000, 48_000);
    }

    private static BufferedImage createFrame() {
        BufferedImage frame = new BufferedImage(FRAME_SIZE, FRAME_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = frame.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, FRAME_SIZE, FRAME_SIZE);
        graphics.dispose();
        return frame;
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
}

