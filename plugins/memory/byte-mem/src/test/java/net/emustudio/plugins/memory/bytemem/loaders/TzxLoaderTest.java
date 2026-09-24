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

public class TzxLoaderTest {
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
        ByteBuffer buffer = createTzxBuffer();
        putStandardBlock(buffer, headerBlock(0x5000, 3));
        putStandardBlock(buffer, dataBlock(new byte[]{0x44, 0x55, 0x66}));

        new TzxLoader().load(writeFile("test.tzx", buffer).toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x44), memoryData.get(0x5000));
        assertEquals(Byte.valueOf((byte) 0x55), memoryData.get(0x5001));
        assertEquals(Byte.valueOf((byte) 0x66), memoryData.get(0x5002));
    }

    @Test
    public void testLoadWritesTurboSpeedBlock() throws IOException {
        ByteBuffer buffer = createTzxBuffer();
        putTurboBlock(buffer, headerBlock(0x6000, 2));
        putTurboBlock(buffer, dataBlock(new byte[]{0x12, 0x34}));

        new TzxLoader().load(writeFile("turbo.tzx", buffer).toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x12), memoryData.get(0x6000));
        assertEquals(Byte.valueOf((byte) 0x34), memoryData.get(0x6001));
    }

    @Test(expected = IOException.class)
    public void testLoadRejectsMissingSignature() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'N', 'o', 't', 'T', 'Z', 'X', '!', 0x1A, 1, 20});

        new TzxLoader().load(writeFile("invalid.tzx", buffer).toPath(), memory, Loader.MemoryBank.of(0, 0));
    }

    private ByteBuffer createTzxBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'Z', 'X', 'T', 'a', 'p', 'e', '!', 0x1A, 1, 20});
        return buffer;
    }

    private byte[] headerBlock(int address, int dataLength) {
        ByteBuffer buffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0).put((byte) 3).put(new byte[10]);
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

    private void putStandardBlock(ByteBuffer buffer, byte[] block) {
        buffer.put((byte) 0x10).putShort((short) 1000).putShort((short) block.length).put(block);
    }

    private void putTurboBlock(ByteBuffer buffer, byte[] block) {
        buffer.put((byte) 0x11);
        for (int i = 0; i < 6; i++) {
            buffer.putShort((short) 1000);
        }
        buffer.put((byte) 8).putShort((short) 1000);
        buffer.put((byte) block.length).put((byte) 0).put((byte) 0).put(block);
    }

    private void setChecksum(byte[] block) {
        byte checksum = 0;
        for (int i = 0; i < block.length - 1; i++) {
            checksum ^= block[i];
        }
        block[block.length - 1] = checksum;
    }

    private File writeFile(String name, ByteBuffer buffer) throws IOException {
        File tzxFile = tmpFolder.newFile(name);
        try (FileOutputStream fos = new FileOutputStream(tzxFile)) {
            fos.write(buffer.array(), 0, buffer.position());
        }
        return tzxFile;
    }
}
