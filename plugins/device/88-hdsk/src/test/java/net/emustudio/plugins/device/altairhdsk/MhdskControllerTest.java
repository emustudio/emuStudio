/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.plugins.device.mits88pio.PioBoard;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

/** Exercise the actual CPU ports, including the initialization and handshakes from MITS errata. */
public class MhdskControllerTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();
    private PioBoard pio;
    private MhdskController controller;

    @Before public void setUp() {
        pio = new PioBoard(0xA0, 2);
        controller = new MhdskController(pio);
        pio.attachPeripheral(controller);
        initializePio();
    }

    private void initializePio() {
        // MITS table 3-C: A sides are input/pulse; B sides are output/handshake or pulse.
        out(0xA0, 0); out(0xA1, 0); out(0xA0, 0x2C);
        out(0xA2, 0); out(0xA3, 255); out(0xA2, 0x24);
        out(0xA4, 0); out(0xA5, 0); out(0xA4, 0x2C);
        out(0xA6, 0); out(0xA7, 255); out(0xA6, 0x2C);
    }

    @After public void tearDown() { controller.close(); }
    private void out(int port, int value) { pio.write(port, (byte) value); }
    private int in(int port) { return pio.read(port) & 255; }
    private boolean ready(int port) { return (in(port) & 128) != 0; }

    private void command(int high, int low) {
        in(0xA1); in(0xA3); in(0xA5); in(0xA7); // Acknowledge old flags.
        out(0xA7, low); // Low byte first; does not issue a command.
        out(0xA3, high);
        assertTrue("Command acknowledged", ready(0xA2));
    }

    private int response() {
        assertTrue("Controller ready", ready(0xA0));
        int status = in(0xA1);
        assertFalse("Status read clears ready", ready(0xA0));
        return status;
    }

    private Path mount(int platter, boolean readOnly) throws Exception {
        Path path = files.getRoot().toPath().resolve("platter" + platter);
        MhdskController.createImage(path);
        controller.attach(platter, path, readOnly);
        return path;
    }

    private byte[] pattern(int seed) {
        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) { data[i] = (byte) (i * 31 + seed); }
        return data;
    }

    private void writeBuffer(int buffer, byte[] data) {
        command(0x40 | buffer, data.length & 255);
        assertFalse("Transfer still pending", ready(0xA0));
        assertTrue(ready(0xA6));
        for (byte value : data) { out(0xA7, value & 255); }
        assertEquals(0, response());
    }

    private byte[] readBuffer(int buffer, int length) {
        command(0x50 | buffer, length & 255);
        assertFalse(ready(0xA0));
        assertTrue("Firmware signals data-ready once for the block", ready(0xA4));
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) in(0xA5);
            assertFalse("Further bytes advance through read strobes", ready(0xA4));
        }
        assertFalse("No extra byte after requested count", ready(0xA4));
        assertEquals(0, response());
        return result;
    }

    @Test public void powerOnStatusIsAllOnes() { assertEquals(255, response()); }

    @Test public void allFourBuffersRoundTripAndPartialWritePreservesTail() {
        for (int buffer = 0; buffer < 4; buffer++) {
            byte[] data = pattern(buffer);
            writeBuffer(buffer, data);
            assertArrayEquals(data, readBuffer(buffer, 256));
            writeBuffer(buffer, new byte[]{42});
            data[0] = 42;
            assertArrayEquals(data, readBuffer(buffer, 256));
        }
    }

    @Test public void lastCylinderAllUnitsAndHeadsUseIndependentPlatterOffsets() throws Exception {
        for (int platter = 0; platter < 8; platter++) { mount(platter, false); }
        for (int unit = 0; unit < 4; unit++) {
            int cylinder = 405 - unit;
            command(unit * 4 | 1, cylinder & 255);
            assertEquals(0, response());
            for (int head = 0; head < 4; head++) {
                byte[] data = pattern(unit * 4 + head);
                writeBuffer(head, data);
                command(0x20 | unit * 4 | head, head * 32 + 23);
                assertEquals(0, response());
            }
        }
        for (int unit = 0; unit < 4; unit++) {
            // No new seek: other units' operations must not change this unit's cylinder.
            for (int head = 0; head < 4; head++) {
                command(0x30 | unit * 4 | head, head * 32 + 23);
                assertEquals(0, response());
                assertArrayEquals(pattern(unit * 4 + head), readBuffer(head, 256));
                Path image = controller.getImage(unit * 2 + head / 2);
                try (RandomAccessFile file = new RandomAccessFile(image.toFile(), "r")) {
                    file.seek(((long) (405 - unit) * 2 + (head & 1)) * 24 * 256 + 23 * 256);
                    byte[] stored = new byte[256]; file.readFully(stored);
                    assertArrayEquals(pattern(unit * 4 + head), stored);
                }
            }
        }
    }

    @Test public void invalidSectorHeadAndCylinderReportErrorsWithoutChangingPosition() throws Exception {
        mount(0, false);
        command(0x30, 24); assertEquals(2, response());
        command(0x30, 128); assertEquals(0x40, response());
        command(1, 150); assertEquals(0x20, response()); // Cylinder 406.
        byte[] data = pattern(99);
        writeBuffer(0, data);
        command(0x20, 0); assertEquals(0, response());
        try (RandomAccessFile file = new RandomAccessFile(controller.getImage(0).toFile(), "r")) {
            byte[] stored = new byte[256]; file.readFully(stored); assertArrayEquals(data, stored);
        }
    }

    @Test public void missingOrTruncatedMediaNeverReturnsSuccess() throws Exception {
        command(0x30, 0); assertEquals(1, response());
        Path image = mount(0, false);
        try (RandomAccessFile file = new RandomAccessFile(image.toFile(), "rw")) { file.setLength(1); }
        command(0x30, 0); assertEquals(1, response());
        command(0x20, 0); assertEquals(1, response());
    }

    @Test public void writeProtectedReadWorksAndWriteAndFormatPreserveImage() throws Exception {
        Path image = mount(0, true);
        writeBuffer(0, pattern(10));
        command(0x20, 0); assertEquals(128, response());
        command(0xC0, 0); assertEquals(128, response());
        command(0x30, 0); assertEquals(128, response());
        assertArrayEquals(new byte[256], readBuffer(0, 256));
        assertEquals(Platter.CAPACITY, Files.size(image));
    }

    @Test public void setIvWaitsForDataAndReadStatusReturnsByteWithAcknowledgements() throws Exception {
        mount(0, false);
        command(0x80, 34);
        assertFalse(ready(0xA0)); assertTrue(ready(0xA6));
        in(0xA7); out(0xA7, 0xA5);
        assertEquals(0, response());
        command(0x60, 34);
        assertTrue(ready(0xA4)); assertEquals(0, response());
        assertEquals(0xA5, in(0xA5)); assertFalse(ready(0xA4));
    }

    @Test public void formatPreservesDataAndMovesToLastCylinder() throws Exception {
        mount(0, false);
        writeBuffer(0, pattern(5));
        command(0x20, 0); assertEquals(0, response());
        command(0xC0, 0); assertEquals(0, response());
        command(0x60, 19); assertEquals(0, response());
        assertEquals((~405) & 255, in(0xA5));
        command(0, 0); assertEquals(0, response());
        command(0x30, 0); assertEquals(0, response());
        assertArrayEquals(pattern(5), readBuffer(0, 256));
    }

    @Test public void readStatusDoesNotRewriteCylinderIvBytesSetByGuest() throws Exception {
        mount(0, false);
        for (int address : new int[]{18, 19}) {
            command(0x80, address); out(0xA7, 0x5A); assertEquals(0, response());
            command(0x60, address); assertEquals(0, response()); assertEquals(0x5A, in(0xA5));
        }
    }

    @Test public void resetAbortsIncompleteTransferAndPreservesMountedImage() throws Exception {
        Path image = mount(0, false);
        command(0x40, 2); out(0xA7, 0xAA);
        assertFalse(ready(0xA0));
        pio.reset(); initializePio();
        assertEquals(255, response());
        assertEquals(image, controller.getImage(0));
        assertArrayEquals(new byte[256], readBuffer(0, 256));
    }

    @Test public void failedMountAndCreateDoNotReplaceExistingMedia() throws Exception {
        Path image = mount(0, false);
        Path bad = files.newFile("short").toPath();
        try { controller.attach(0, bad, false); fail("Short image must fail"); }
        catch (IOException expected) { }
        assertEquals(image, controller.getImage(0));
        try { MhdskController.createImage(image); fail("Create must not overwrite"); }
        catch (IOException expected) { }
        assertEquals(Platter.CAPACITY, Files.size(image));
    }
}
