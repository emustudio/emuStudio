/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88mds;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MdsControllerTest {
    private final MdsController controller = new MdsController();
    private Path image;
    private Context8080.CpuPortDevice status;
    private Context8080.CpuPortDevice control;
    private Context8080.CpuPortDevice data;

    @Before
    public void setUp() throws Exception {
        image = Files.createTempFile("88-mds", ".dsk");
        Files.write(image, new byte[(int) MdsDrive.IMAGE_SIZE]);
        controller.attach(0, image);
        status = controller.statusPort();
        control = controller.controlPort();
        data = controller.dataPort();
        status.write(MdsController.STATUS_PORT, (byte) 0);
        control.read(MdsController.CONTROL_PORT);
        control.read(MdsController.CONTROL_PORT);
    }

    @After
    public void tearDown() throws Exception {
        controller.close();
        Files.deleteIfExists(image);
    }

    @Test
    public void readsRawSectorBytes() throws Exception {
        byte[] imageBytes = Files.readAllBytes(image);
        for (int i = 0; i < MdsDrive.SECTOR_SIZE; i++) {
            imageBytes[i] = (byte) i;
        }
        Files.write(image, imageBytes);

        byte[] actual = new byte[MdsDrive.SECTOR_SIZE];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = data.read(MdsController.DATA_PORT);
        }

        byte[] expected = new byte[MdsDrive.SECTOR_SIZE];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) i;
        }
        assertArrayEquals(expected, actual);
    }

    @Test
    public void writesCompleteRawSector() throws Exception {
        control.write(MdsController.CONTROL_PORT, (byte) 0x80);
        for (int i = 0; i < MdsDrive.SECTOR_SIZE; i++) {
            data.write(MdsController.DATA_PORT, (byte) (255 - i));
        }
        controller.detach(0);

        byte[] imageBytes = Files.readAllBytes(image);
        for (int i = 0; i < MdsDrive.SECTOR_SIZE; i++) {
            assertEquals((byte) (255 - i), imageBytes[i]);
        }
    }

    @Test
    public void clampsTrackToMinidiskGeometry() {
        for (int i = 0; i < 50; i++) {
            control.write(MdsController.CONTROL_PORT, (byte) 1);
        }
        assertEquals(MdsDrive.TRACKS - 1, controller.track(0));
    }

    @Test
    public void reportsNoDriveWhenUnattachedDriveSelected() {
        status.write(MdsController.STATUS_PORT, (byte) 1);
        assertEquals(0xFF, status.read(MdsController.STATUS_PORT) & 0xFF);
        assertEquals(0xFF, control.read(MdsController.CONTROL_PORT) & 0xFF);
    }
}
