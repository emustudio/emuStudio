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
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0x10).putShort((short) 1000).putShort((short) 19).put((byte) 0).put((byte) 3);
        buffer.put(new byte[10]).putShort((short) 3).putShort((short) 0x5000).putShort((short) 0).put((byte) 0);
        buffer.put((byte) 0x10).putShort((short) 1000).putShort((short) 5).put((byte) 0xFF);
        buffer.put(new byte[]{0x44, 0x55, 0x66}).put((byte) 0);

        File tzxFile = tmpFolder.newFile("test.tzx");
        try (FileOutputStream fos = new FileOutputStream(tzxFile)) {
            fos.write(buffer.array(), 0, buffer.position());
        }

        new TzxLoader().load(tzxFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x44), memoryData.get(0x5000));
        assertEquals(Byte.valueOf((byte) 0x55), memoryData.get(0x5001));
        assertEquals(Byte.valueOf((byte) 0x66), memoryData.get(0x5002));
    }
}
