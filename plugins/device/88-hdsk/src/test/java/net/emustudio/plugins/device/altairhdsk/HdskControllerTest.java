/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HdskControllerTest {
    private MemoryContext<Byte> memory;
    private HdskController controller;
    private Path image;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        memory = createMock(MemoryContext.class);
        controller = new HdskController(memory);
        image = Files.createTempFile("hdsk", ".img");
        controller.attach(0, image);
    }

    @After
    public void tearDown() throws Exception {
        controller.close();
        Files.deleteIfExists(image);
    }

    @Test
    public void readsSectorIntoDmaMemory() throws Exception {
        byte[] sector = new byte[128];
        for (int i = 0; i < sector.length; i++) {
            sector[i] = (byte) i;
        }
        Files.write(image, sector);

        expect(memory.getSize()).andReturn(0x10000);
        for (int i = 0; i < sector.length; i++) {
            memory.write(0x100 + i, sector[i]);
            expectLastCall();
        }
        replay(memory);

        command(HdskController.READ, 0, 0, 0, 0x100);
        assertEquals(HdskController.OK, controller.read(HdskController.PORT));
        verify(memory);
    }

    @Test
    public void writesDmaMemoryToSector() throws Exception {
        expect(memory.getSize()).andReturn(0x10000);
        for (int i = 0; i < 128; i++) {
            expect(memory.read(0x200 + i)).andReturn((byte) (255 - i));
        }
        replay(memory);

        command(HdskController.WRITE, 0, 0, 0, 0x200);
        assertEquals(HdskController.OK, controller.read(HdskController.PORT));

        byte[] expected = new byte[128];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) (255 - i);
        }
        assertArrayEquals(expected, Files.readAllBytes(image));
        verify(memory);
    }

    @Test
    public void returnsSimhParameterBlock() {
        replay(memory);
        controller.write(HdskController.PORT, (byte) HdskController.PARAM);
        controller.write(HdskController.PORT, (byte) 0);

        byte[] actual = new byte[19];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = controller.read(HdskController.PORT);
        }

        assertArrayEquals(new byte[]{32, 0, 5, 31, 1, (byte) 0xF9, 7, (byte) 0xFF, 3,
                (byte) 0xFF, 0, 0, 0, 6, 0, 0, 0, (byte) 0x80, 0}, actual);
    }

    @Test
    public void returnsErrorForUnattachedDrive() {
        replay(memory);
        command(HdskController.READ, 1, 0, 0, 0);
        assertEquals(HdskController.ERROR, controller.read(HdskController.PORT));
    }

    private void command(int operation, int drive, int sector, int track, int dma) {
        controller.write(HdskController.PORT, (byte) operation);
        controller.write(HdskController.PORT, (byte) drive);
        controller.write(HdskController.PORT, (byte) sector);
        controller.write(HdskController.PORT, (byte) track);
        controller.write(HdskController.PORT, (byte) (track >>> 8));
        controller.write(HdskController.PORT, (byte) dma);
        controller.write(HdskController.PORT, (byte) (dma >>> 8));
    }
}
