/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TapLoaderTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private ByteMemoryContext memory;
    private final Map<Integer, Byte> memoryData = new HashMap<>();

    @Before
    public void setUp() {
        memoryData.clear();
        memory = createNiceMock(ByteMemoryContext.class);

        expect(memory.getSelectedBank()).andReturn(0).anyTimes();
        memory.selectBank(anyInt());
        expectLastCall().anyTimes();
        memory.write(anyInt(), anyObject(Byte[].class));
        expectLastCall().andAnswer(() -> {
            int address = (int) getCurrentArguments()[0];
            Byte[] data = (Byte[]) getCurrentArguments()[1];
            for (int i = 0; i < data.length; i++) {
                memoryData.put(address + i, data[i]);
            }
            return null;
        }).anyTimes();

        replay(memory);
    }

    @Test
    public void testLoadWritesMemoryBlock() throws IOException {
        File tapFile = createTapFile("memory.tap", (byte) 3, 0x1000, new byte[]{0x11, 0x22, 0x33});

        new TapLoader().load(tapFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x11), memoryData.get(0x1000));
        assertEquals(Byte.valueOf((byte) 0x22), memoryData.get(0x1001));
        assertEquals(Byte.valueOf((byte) 0x33), memoryData.get(0x1002));
    }

    @Test
    public void testLoadIgnoresNonMemoryBlocks() throws IOException {
        File tapFile = createTapFile("program.tap", (byte) 0, 0x2000, new byte[]{(byte) 0xAA, (byte) 0xBB});

        new TapLoader().load(tapFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertNull(memoryData.get(0x2000));
    }

    private File createTapFile(String name, byte headerType, int address, byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(32 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 19).put((byte) 0).put(headerType).put(new byte[10]);
        buffer.putShort((short) data.length).putShort((short) address).putShort((short) 0).put((byte) 0);
        buffer.putShort((short) (data.length + 2)).put((byte) 0xFF).put(data).put((byte) 0);

        File tapFile = tmpFolder.newFile(name);
        try (FileOutputStream fos = new FileOutputStream(tapFile)) {
            fos.write(buffer.array(), 0, buffer.position());
        }
        return tapFile;
    }
}
