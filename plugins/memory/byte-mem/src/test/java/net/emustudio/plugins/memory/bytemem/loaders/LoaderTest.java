/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoaderTest {

    @Test
    public void testCreateLoaderRecognizesKnownExtensions() {
        assertCreateLoader("file.hex", HexLoader.class);
        assertCreateLoader("file.tap", TapLoader.class);
        assertCreateLoader("file.tzx", TzxLoader.class);
        assertCreateLoader("file.bin", BinaryLoader.class);
        assertCreateLoader("file.com", BinaryLoader.class);
        assertCreateLoader("file.HEX", HexLoader.class);
    }

    @Test
    public void testCreateLoaderFallsBackToBinary() {
        assertCreateLoader("file.xyz", BinaryLoader.class);
        assertCreateLoader("somefile", BinaryLoader.class);
    }

    @Test
    public void testMemoryAddressAwareness() {
        assertTrue(new HexLoader().isMemoryAddressAware());
        assertTrue(new TapLoader().isMemoryAddressAware());
        assertTrue(new TzxLoader().isMemoryAddressAware());
        assertFalse(new BinaryLoader().isMemoryAddressAware());
    }

    @Test
    public void testMemoryBankOf() {
        Loader.MemoryBank memoryBank = Loader.MemoryBank.of(2, 100);

        assertEquals(2, memoryBank.bank);
        assertEquals(100, memoryBank.address);
    }

    private void assertCreateLoader(String fileName, Class<? extends Loader> loaderClass) {
        assertTrue(loaderClass.isInstance(Loader.createLoader(Path.of(fileName))));
    }
}
