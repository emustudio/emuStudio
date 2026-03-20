/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.loaders;

import org.junit.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

public class LoaderTest {

    @Test
    public void testHasLoaderForTap() {
        assertTrue(Loader.hasLoader(Path.of("file.tap")));
    }

    @Test
    public void testHasLoaderForTzx() {
        assertTrue(Loader.hasLoader(Path.of("file.tzx")));
    }

    @Test
    public void testHasLoaderForTapUpperCase() {
        assertTrue(Loader.hasLoader(Path.of("FILE.TAP")));
    }

    @Test
    public void testHasLoaderForTzxUpperCase() {
        assertTrue(Loader.hasLoader(Path.of("FILE.TZX")));
    }

    @Test
    public void testHasLoaderForTapMixedCase() {
        assertTrue(Loader.hasLoader(Path.of("file.TaP")));
    }

    @Test
    public void testHasLoaderReturnsFalseForUnknownExtension() {
        assertFalse(Loader.hasLoader(Path.of("file.wav")));
    }

    @Test
    public void testHasLoaderReturnsFalseForNoExtension() {
        assertFalse(Loader.hasLoader(Path.of("filename")));
    }

    @Test
    public void testHasLoaderReturnsFalseForEmptyExtension() {
        assertFalse(Loader.hasLoader(Path.of("file.")));
    }

    @Test
    public void testCreateForTap() {
        Optional<Loader> loader = Loader.create(Path.of("file.tap"));
        assertTrue(loader.isPresent());
        assertTrue(loader.get() instanceof TapLoader);
    }

    @Test
    public void testCreateForTzx() {
        Optional<Loader> loader = Loader.create(Path.of("file.tzx"));
        assertTrue(loader.isPresent());
        assertTrue(loader.get() instanceof TzxLoader);
    }

    @Test
    public void testCreateForUnknownExtension() {
        Optional<Loader> loader = Loader.create(Path.of("file.wav"));
        assertFalse(loader.isPresent());
    }

    @Test
    public void testCreateForNoExtension() {
        Optional<Loader> loader = Loader.create(Path.of("noextension"));
        assertFalse(loader.isPresent());
    }

    @Test
    public void testCreateForUpperCaseTap() {
        Optional<Loader> loader = Loader.create(Path.of("FILE.TAP"));
        assertTrue(loader.isPresent());
        assertTrue(loader.get() instanceof TapLoader);
    }

    @Test
    public void testHasLoaderWithDirectoryPath() {
        assertTrue(Loader.hasLoader(Path.of("/some/path/to/file.tap")));
    }

    @Test
    public void testCreateWithDirectoryPath() {
        Optional<Loader> loader = Loader.create(Path.of("/some/path/to/file.tzx"));
        assertTrue(loader.isPresent());
    }
}

