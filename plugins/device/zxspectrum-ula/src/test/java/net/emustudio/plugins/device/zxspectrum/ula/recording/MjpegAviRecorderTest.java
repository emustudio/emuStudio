/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.recording;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MjpegAviRecorderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testWritesIndexedAviWithVideoAndAudioChunks() throws IOException {
        MjpegAviRecorder recorder = new MjpegAviRecorder(8, 8, 69888, 3_500_000, 48_000);
        BufferedImage frame = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = frame.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 8, 8);
        graphics.setColor(Color.YELLOW);
        graphics.fillRect(2, 2, 4, 4);
        graphics.dispose();

        recorder.recordVideoFrame(frame);
        recorder.recordAudio(new byte[]{1, 2, 3, 4}, 4);

        Path output = temporaryFolder.newFile("capture.avi").toPath();
        recorder.moveTo(output);

        byte[] bytes = Files.readAllBytes(output);
        assertEquals("RIFF", ascii(bytes, 0));
        assertEquals(bytes.length - 8, readInt(bytes, 4));
        assertEquals("AVI ", ascii(bytes, 8));
        assertTrue(findAscii(bytes, "hdrl") >= 0);
        int moviOffset = findAscii(bytes, "movi");
        int idx1Offset = findAscii(bytes, "idx1");
        int firstVideoChunkOffset = findAscii(bytes, "00dc");
        int firstAudioChunkOffset = findAscii(bytes, "01wb");
        int mjpgOffset = findAscii(bytes, "MJPG");

        assertTrue(moviOffset >= 0);
        assertTrue(idx1Offset > moviOffset);
        assertTrue(firstVideoChunkOffset > moviOffset);
        assertTrue(firstAudioChunkOffset > firstVideoChunkOffset);
        assertTrue(mjpgOffset >= 0);
        assertEquals("00dc", ascii(bytes, idx1Offset + 8));
        assertEquals(firstVideoChunkOffset, moviOffset + readInt(bytes, idx1Offset + 16));
        assertEquals("01wb", ascii(bytes, idx1Offset + 24));
        assertEquals(firstAudioChunkOffset, moviOffset + readInt(bytes, idx1Offset + 32));
        assertEquals("MJPG", ascii(bytes, mjpgOffset));
    }

    private static String ascii(byte[] bytes, int offset) {
        return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
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

    private static int readInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }
}
