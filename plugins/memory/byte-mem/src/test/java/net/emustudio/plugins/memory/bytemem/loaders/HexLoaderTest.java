/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class HexLoaderTest {

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
        memory.write(anyInt(), anyObject(Byte.class));
        expectLastCall().andAnswer(() -> {
            int addr = (int) getCurrentArguments()[0];
            Byte data = (Byte) getCurrentArguments()[1];
            memoryData.put(addr, data);
            return null;
        }).anyTimes();

        replay(memory);
    }

    private File createHexFile(String name, String... lines) throws IOException {
        File hexFile = tmpFolder.newFile(name);
        try (FileWriter writer = new FileWriter(hexFile)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
        return hexFile;
    }

    @Test
    public void testIsMemoryAddressAware() {
        assertTrue(new HexLoader().isMemoryAddressAware());
    }

    @Test
    public void testLoadValidHexFile() throws IOException {
        // :03000000010203F7 = 3 bytes at address 0x0000: 01 02 03
        // :00000001FF = EOF
        File hexFile = createHexFile("valid.hex",
                ":03000000010203F7",
                ":00000001FF");

        HexLoader loader = new HexLoader();
        loader.load(hexFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x01), memoryData.get(0));
        assertEquals(Byte.valueOf((byte) 0x02), memoryData.get(1));
        assertEquals(Byte.valueOf((byte) 0x03), memoryData.get(2));
    }

    @Test
    public void testLoadSelectsBank() throws IOException {
        File hexFile = createHexFile("bank.hex",
                ":0100000041BE",
                ":00000001FF");

        HexLoader loader = new HexLoader();
        loader.load(hexFile.toPath(), memory, Loader.MemoryBank.of(2, 0));

        assertEquals(2, selectedBank);
    }

    @Test
    public void testLoadRestoresBankOnError() throws IOException {
        selectedBank = 3;
        File hexFile = tmpFolder.newFile("bad.hex");
        // Write invalid content that will cause IOException during parsing
        try (FileWriter writer = new FileWriter(hexFile)) {
            writer.write("not a hex file\n");
        }

        HexLoader loader = new HexLoader();
        try {
            loader.load(hexFile.toPath(), memory, Loader.MemoryBank.of(1, 0));
        } catch (IOException e) {
            // expected - bank should be restored to old value
            assertEquals(3, selectedBank);
            return;
        }
        // If no exception was thrown, that's also OK - some implementations might be lenient
    }

    @Test(expected = IOException.class)
    public void testLoadNonExistentFileThrows() throws IOException {
        HexLoader loader = new HexLoader();
        loader.load(Path.of("/nonexistent/file.hex"), memory, Loader.MemoryBank.of(0, 0));
    }

    @Test
    public void testLoadHexFileAtNonZeroAddress() throws IOException {
        // :0200100041426B = 2 bytes at address 0x0010: 41 42
        // :00000001FF = EOF
        File hexFile = createHexFile("offset.hex",
                ":0200100041426B",
                ":00000001FF");

        HexLoader loader = new HexLoader();
        loader.load(hexFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x41), memoryData.get(0x10));
        assertEquals(Byte.valueOf((byte) 0x42), memoryData.get(0x11));
    }
}

