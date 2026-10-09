/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.emulib.runtime.io.FileLoader;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoaderTest {

    @Test
    public void testCreateLoaderRecognizesKnownExtensions() {
        assertCreateLoader("file.hex", FileLoader.Format.HEX);
        assertCreateLoader("file.tap", FileLoader.Format.TAP);
        assertCreateLoader("file.tzx", FileLoader.Format.TZX);
        assertCreateLoader("file.bin", FileLoader.Format.BIN);
        assertCreateLoader("file.com", FileLoader.Format.BIN);
        assertCreateLoader("file.out", FileLoader.Format.BIN);
        assertCreateLoader("file.rom", FileLoader.Format.BIN);
        assertCreateLoader("file.HEX", FileLoader.Format.HEX);
    }

    @Test
    public void testCreateLoaderFallsBackToBinary() {
        assertCreateLoader("file.xyz", FileLoader.Format.BIN);
        assertCreateLoader("somefile", FileLoader.Format.BIN);
    }

    @Test
    public void testMemoryAddressAwareness() {
        assertFalse(MemoryImageLoader.requiresLoadAddress(MemoryImageLoader.createLoader(Path.of("file.hex"))));
        assertFalse(MemoryImageLoader.requiresLoadAddress(MemoryImageLoader.createLoader(Path.of("file.tap"))));
        assertFalse(MemoryImageLoader.requiresLoadAddress(MemoryImageLoader.createLoader(Path.of("file.tzx"))));
        assertTrue(MemoryImageLoader.requiresLoadAddress(MemoryImageLoader.createLoader(Path.of("file.bin"))));
    }

    @Test
    public void testMemoryBankOf() {
        MemoryImageLoader.MemoryBank memoryBank = MemoryImageLoader.MemoryBank.of(2, 100);

        assertEquals(2, memoryBank.bank);
        assertEquals(100, memoryBank.address);
    }

    private void assertCreateLoader(String fileName, FileLoader.Format format) {
        assertEquals(format, MemoryImageLoader.createLoader(Path.of(fileName)).getFormat());
    }
}
