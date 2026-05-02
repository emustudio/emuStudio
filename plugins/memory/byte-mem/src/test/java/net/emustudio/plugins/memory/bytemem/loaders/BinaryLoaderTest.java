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
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class BinaryLoaderTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private ByteMemoryContext memory;
    private int selectedBank;
    private final Map<Integer, Byte> memoryData = new HashMap<>();

    @Before
    public void setUp() {
        selectedBank = 0;
        memoryData.clear();
        memory = createNiceMock(ByteMemoryContext.class);

        expect(memory.getSelectedBank()).andAnswer(() -> selectedBank).anyTimes();
        memory.selectBank(anyInt());
        expectLastCall().andAnswer(() -> {
            selectedBank = (int) getCurrentArguments()[0];
            return null;
        }).anyTimes();
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
    public void testLoadWritesContentAndSelectsBank() throws IOException {
        File binFile = tmpFolder.newFile("test.bin");
        try (FileOutputStream fos = new FileOutputStream(binFile)) {
            fos.write(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});
        }

        new BinaryLoader().load(binFile.toPath(), memory, Loader.MemoryBank.of(1, 0x100));

        assertEquals(1, selectedBank);
        assertEquals(Byte.valueOf((byte) 0x01), memoryData.get(0x100));
        assertEquals(Byte.valueOf((byte) 0x02), memoryData.get(0x101));
        assertEquals(Byte.valueOf((byte) 0x05), memoryData.get(0x104));
    }
}
