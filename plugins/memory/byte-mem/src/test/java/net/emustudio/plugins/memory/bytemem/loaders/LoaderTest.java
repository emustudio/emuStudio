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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class LoaderTest {
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
            int addr = (int) getCurrentArguments()[0];
            Byte[] data = (Byte[]) getCurrentArguments()[1];
            for (int i = 0; i < data.length; i++) {
                memoryData.put(addr + i, data[i]);
            }
            return null;
        }).anyTimes();

        replay(memory);
    }

    // === Loader.createLoader tests ===

    @Test
    public void testCreateLoaderForHexExtension() {
        assertTrue(Loader.createLoader(Path.of("file.hex")) instanceof HexLoader);
    }

    @Test
    public void testCreateLoaderForTapExtension() {
        assertTrue(Loader.createLoader(Path.of("file.tap")) instanceof TapLoader);
    }

    @Test
    public void testCreateLoaderForTzxExtension() {
        assertTrue(Loader.createLoader(Path.of("file.tzx")) instanceof TzxLoader);
    }

    @Test
    public void testCreateLoaderForBinExtension() {
        assertTrue(Loader.createLoader(Path.of("file.bin")) instanceof BinaryLoader);
    }

    @Test
    public void testCreateLoaderForComExtension() {
        assertTrue(Loader.createLoader(Path.of("file.com")) instanceof BinaryLoader);
    }

    @Test
    public void testCreateLoaderForUnknownExtension() {
        assertTrue(Loader.createLoader(Path.of("file.xyz")) instanceof BinaryLoader);
    }

    @Test
    public void testCreateLoaderNoExtension() {
        assertTrue(Loader.createLoader(Path.of("somefile")) instanceof BinaryLoader);
    }

    @Test
    public void testHexLoaderIsMemoryAddressAware() {
        assertTrue(new HexLoader().isMemoryAddressAware());
    }

    @Test
    public void testBinaryLoaderIsNotMemoryAddressAware() {
        assertFalse(new BinaryLoader().isMemoryAddressAware());
    }

    @Test
    public void testTapLoaderIsMemoryAddressAware() {
        assertTrue(new TapLoader().isMemoryAddressAware());
    }

    @Test
    public void testTzxLoaderIsMemoryAddressAware() {
        assertTrue(new TzxLoader().isMemoryAddressAware());
    }

    @Test
    public void testMemoryBankOf() {
        Loader.MemoryBank mb = Loader.MemoryBank.of(2, 100);
        assertEquals(2, mb.bank);
        assertEquals(100, mb.address);
    }

    @Test
    public void testCreateLoaderUpperCaseExtension() {
        assertTrue(Loader.createLoader(Path.of("file.HEX")) instanceof HexLoader);
    }

    // === BinaryLoader tests ===

    @Test
    public void testBinaryLoaderLoadsContent() throws IOException {
        File binFile = tmpFolder.newFile("test.bin");
        try (FileOutputStream fos = new FileOutputStream(binFile)) {
            fos.write(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});
        }

        BinaryLoader loader = new BinaryLoader();
        loader.load(binFile.toPath(), memory, Loader.MemoryBank.of(0, 0x100));

        assertEquals(Byte.valueOf((byte) 0x01), memoryData.get(0x100));
        assertEquals(Byte.valueOf((byte) 0x02), memoryData.get(0x101));
        assertEquals(Byte.valueOf((byte) 0x05), memoryData.get(0x104));
    }

    @Test
    public void testBinaryLoaderSelectsBank() throws IOException {
        File binFile = tmpFolder.newFile("test2.bin");
        try (FileOutputStream fos = new FileOutputStream(binFile)) {
            fos.write(new byte[]{(byte) 0xAA});
        }

        BinaryLoader loader = new BinaryLoader();
        loader.load(binFile.toPath(), memory, Loader.MemoryBank.of(1, 0));

        assertEquals(1, selectedBank);
    }

    // === TapLoader tests ===

    @Test
    public void testTapLoaderLoadsMemoryBlock() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Header block
        buf.putShort((short) 19);
        buf.put((byte) 0); // flag = header
        buf.put((byte) 3); // headerType = 3 (memory block)
        buf.put(new byte[10]); // filename
        buf.putShort((short) 3); // data length
        buf.putShort((short) 0x1000); // start address
        buf.putShort((short) 0);
        buf.put((byte) 0); // checksum

        // Data block
        buf.putShort((short) 5);
        buf.put((byte) 0xFF); // flag = data
        buf.put(new byte[]{0x11, 0x22, 0x33});
        buf.put((byte) 0); // checksum

        buf.flip();
        byte[] content = new byte[buf.remaining()];
        buf.get(content);

        File tapFile = tmpFolder.newFile("test.tap");
        try (FileOutputStream fos = new FileOutputStream(tapFile)) {
            fos.write(content);
        }

        TapLoader loader = new TapLoader();
        loader.load(tapFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x11), memoryData.get(0x1000));
        assertEquals(Byte.valueOf((byte) 0x22), memoryData.get(0x1001));
        assertEquals(Byte.valueOf((byte) 0x33), memoryData.get(0x1002));
    }

    @Test
    public void testTapLoaderIgnoresNonMemoryBlocks() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Header block: type=0 (program, not memory block)
        buf.putShort((short) 19);
        buf.put((byte) 0);
        buf.put((byte) 0); // headerType = 0 (program)
        buf.put(new byte[10]);
        buf.putShort((short) 2);
        buf.putShort((short) 0x2000);
        buf.putShort((short) 0);
        buf.put((byte) 0);

        // Data block
        buf.putShort((short) 4);
        buf.put((byte) 0xFF);
        buf.put(new byte[]{(byte) 0xAA, (byte) 0xBB});
        buf.put((byte) 0);

        buf.flip();
        byte[] content = new byte[buf.remaining()];
        buf.get(content);

        File tapFile = tmpFolder.newFile("test_notype.tap");
        try (FileOutputStream fos = new FileOutputStream(tapFile)) {
            fos.write(content);
        }

        TapLoader loader = new TapLoader();
        loader.load(tapFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertNull(memoryData.get(0x2000));
    }

    // === TzxLoader tests ===

    @Test
    public void testTzxLoaderLoadsMemoryBlock() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Header block
        buf.put((byte) 0x10);
        buf.putShort((short) 1000);
        buf.putShort((short) 19);
        buf.put((byte) 0);
        buf.put((byte) 3); // memory block
        buf.put(new byte[10]);
        buf.putShort((short) 3);
        buf.putShort((short) 0x5000);
        buf.putShort((short) 0);
        buf.put((byte) 0);

        // Data block
        buf.put((byte) 0x10);
        buf.putShort((short) 1000);
        buf.putShort((short) 5);
        buf.put((byte) 0xFF);
        buf.put(new byte[]{0x44, 0x55, 0x66});
        buf.put((byte) 0);

        buf.flip();
        byte[] content = new byte[buf.remaining()];
        buf.get(content);

        File tzxFile = tmpFolder.newFile("test.tzx");
        try (FileOutputStream fos = new FileOutputStream(tzxFile)) {
            fos.write(content);
        }

        TzxLoader loader = new TzxLoader();
        loader.load(tzxFile.toPath(), memory, Loader.MemoryBank.of(0, 0));

        assertEquals(Byte.valueOf((byte) 0x44), memoryData.get(0x5000));
        assertEquals(Byte.valueOf((byte) 0x55), memoryData.get(0x5001));
        assertEquals(Byte.valueOf((byte) 0x66), memoryData.get(0x5002));
    }
}
