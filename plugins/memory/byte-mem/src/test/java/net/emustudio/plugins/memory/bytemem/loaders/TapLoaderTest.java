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

    @Test
    public void testLoadWritesMultipleMemoryBlocks() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
        putTapBlock(buffer, headerBlock((byte) 3, 0x1000, 2));
        putTapBlock(buffer, dataBlock(new byte[]{0x11, 0x22}));
        putTapBlock(buffer, headerBlock((byte) 3, 0x2000, 1));
        putTapBlock(buffer, dataBlock(new byte[]{0x33}));

        File tapFile = writeFile("multi.tap", buffer);
        new TapLoader().load(tapFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x11), memoryData.get(0x1000));
        assertEquals(Byte.valueOf((byte) 0x22), memoryData.get(0x1001));
        assertEquals(Byte.valueOf((byte) 0x33), memoryData.get(0x2000));
    }

    @Test(expected = IOException.class)
    public void testLoadRejectsInvalidChecksum() throws IOException {
        byte[] header = headerBlock((byte) 3, 0x1000, 1);
        header[header.length - 1] ^= 1;
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        putTapBlock(buffer, header);

        new TapLoader().load(writeFile("invalid.tap", buffer).toPath(), memory, Loader.MemoryBank.of(0, 0));
    }

    private File createTapFile(String name, byte headerType, int address, byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(32 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        putTapBlock(buffer, headerBlock(headerType, address, data.length));
        putTapBlock(buffer, dataBlock(data));
        return writeFile(name, buffer);
    }

    private byte[] headerBlock(byte headerType, int address, int dataLength) {
        ByteBuffer buffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0).put(headerType).put(new byte[10]);
        buffer.putShort((short) dataLength).putShort((short) address).putShort((short) 0).put((byte) 0);
        setChecksum(buffer.array());
        return buffer.array();
    }

    private byte[] dataBlock(byte[] data) {
        byte[] block = new byte[data.length + 2];
        block[0] = (byte) 0xFF;
        System.arraycopy(data, 0, block, 1, data.length);
        setChecksum(block);
        return block;
    }

    private void putTapBlock(ByteBuffer buffer, byte[] block) {
        buffer.putShort((short) block.length).put(block);
    }

    private void setChecksum(byte[] block) {
        byte checksum = 0;
        for (int i = 0; i < block.length - 1; i++) {
            checksum ^= block[i];
        }
        block[block.length - 1] = checksum;
    }

    private File writeFile(String name, ByteBuffer buffer) throws IOException {
        File tapFile = tmpFolder.newFile(name);
        try (FileOutputStream fos = new FileOutputStream(tapFile)) {
            fos.write(buffer.array(), 0, buffer.position());
        }
        return tapFile;
    }
}
