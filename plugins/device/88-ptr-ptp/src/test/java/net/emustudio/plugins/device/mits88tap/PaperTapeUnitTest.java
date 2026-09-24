/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PaperTapeUnitTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testIntelHexTapeIsDeliveredUnchanged() throws Exception {
        byte[] tape = ":03000000010203F7\r\n:00000001FF\r\n".getBytes(StandardCharsets.US_ASCII);
        Path input = temporaryFolder.newFile("loader.pt").toPath();
        Files.write(input, tape);
        PaperTapeUnit unit = new PaperTapeUnit();
        unit.attachReader(input);

        for (byte expected : tape) {
            assertEquals(3, unit.read(PaperTapeUnit.STATUS_PORT) & 0xFF);
            assertEquals(expected & 0xFF, unit.read(PaperTapeUnit.DATA_PORT) & 0xFF);
        }
        assertEquals(0x1A, unit.read(PaperTapeUnit.DATA_PORT) & 0xFF);
        assertEquals(2, unit.read(PaperTapeUnit.STATUS_PORT) & 0xFF);
        assertEquals(0, unit.read(PaperTapeUnit.DATA_PORT) & 0xFF);
    }

    @Test
    public void testReaderRewindRestartsStream() throws Exception {
        Path input = temporaryFolder.newFile("input.bin").toPath();
        Files.write(input, new byte[]{1, 2});
        PaperTapeUnit unit = new PaperTapeUnit();
        unit.attachReader(input);
        assertEquals(1, unit.read(PaperTapeUnit.DATA_PORT));
        unit.rewindReader();
        assertEquals(1, unit.read(PaperTapeUnit.DATA_PORT));
    }

    @Test
    public void testPunchWritesBytesInOrder() throws Exception {
        Path output = temporaryFolder.newFile("output.pt").toPath();
        PaperTapeUnit unit = new PaperTapeUnit();
        unit.attachPunch(output);
        unit.write(PaperTapeUnit.DATA_PORT, (byte) 0x10);
        unit.write(PaperTapeUnit.DATA_PORT, (byte) 0xFE);
        unit.detachPunch();

        assertArrayEquals(new byte[]{0x10, (byte) 0xFE}, Files.readAllBytes(output));
    }

    @Test
    public void testUnattachedReaderIsIdleAndPunchWritesAreIgnored() {
        PaperTapeUnit unit = new PaperTapeUnit();
        assertEquals(2, unit.read(PaperTapeUnit.STATUS_PORT) & 0xFF);
        assertEquals(0, unit.read(PaperTapeUnit.DATA_PORT) & 0xFF);
        unit.write(PaperTapeUnit.DATA_PORT, (byte) 1);
    }

    @Test
    public void testStatusResetAllowsAnotherEndMarker() throws Exception {
        Path input = temporaryFolder.newFile("empty.pt").toPath();
        PaperTapeUnit unit = new PaperTapeUnit();
        unit.attachReader(input);
        assertEquals(0x1A, unit.read(PaperTapeUnit.DATA_PORT) & 0xFF);
        unit.write(PaperTapeUnit.STATUS_PORT, (byte) 3);
        assertEquals(3, unit.read(PaperTapeUnit.STATUS_PORT) & 0xFF);
        assertEquals(0x1A, unit.read(PaperTapeUnit.DATA_PORT) & 0xFF);
    }
}
