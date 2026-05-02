/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class MemoryImplTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private MemoryImpl memory;

    @Before
    public void setUp() {
        memory = createMemory(new PluginSettingsMock().mock());
    }

    @After
    public void tearDown() {
        if (memory != null) {
            memory.destroy();
        }
    }

    @Test
    public void metadataIsAvailable() {
        assertNotEquals("(unknown)", memory.getVersion());
        assertNotEquals("(unknown)", memory.getCopyright());
        assertFalse(memory.getDescription().isEmpty());
    }

    @Test
    public void initializeNormalizesCoreSettings() throws PluginInitializationException {
        assertInitialization(settings(), 1, 0);
        assertInitialization(settings().set("banksCount", -1), 1, 0);
        assertInitialization(settings().set("banksCount", 0), 1, 0);
        assertInitialization(settings().set("banksCount", 3).set("commonBoundary", 128), 3, 128);
        assertInitialization(settings().set("commonBoundary", -5), 1, 0);
    }

    @Test
    public void initializeLoadsImagesAndRomRanges() throws Exception {
        Path image = image("init.bin", (byte) 1, (byte) 2, (byte) 3);
        MemoryImpl mem = createMemory(settings()
                .set("imageName0", image.toString()).set("imageAddress0", 0).set("imageBank0", 0)
                .set("ROMfrom0", 0).set("ROMto0", 1).mock());
        try {
            mem.initialize();
            MemoryContextImpl context = context(mem);
            assertEquals(Byte.valueOf((byte) 1), context.read(0));
            assertEquals(Byte.valueOf((byte) 3), context.read(2));
            assertTrue(context.isReadOnly(0));
            assertTrue(context.isReadOnly(1));
            assertFalse(context.isReadOnly(2));
        } finally {
            mem.destroy();
        }
    }

    @Test
    public void initializeIgnoresMissingImage() throws PluginInitializationException {
        MemoryImpl mem = createMemory(settings()
                .set("imageName0", "/nonexistent/file.bin").set("imageAddress0", 0).set("imageBank0", 0).mock());
        try {
            mem.initialize();
            assertEquals(256, mem.getSize());
        } finally {
            mem.destroy();
        }
    }

    @Test
    public void initializeFailsForNegativeMemorySize() {
        MemoryImpl mem = createMemory(settings().set("memorySize", -1).mock());
        try {
            mem.initialize();
            fail("Expected NegativeArraySizeException");
        } catch (NegativeArraySizeException | PluginInitializationException ignored) {
        } finally {
            mem.destroy();
        }
    }

    @Test
    public void loadImageLoadsBytesAndMissingFileThrows() throws Exception {
        MemoryImpl mem = createMemory(settings().mock());
        Path image = image("direct.bin", (byte) 0x12, (byte) 0x34);
        try {
            mem.initialize();
            mem.loadImage(image, 0, 0);
            assertEquals(Byte.valueOf((byte) 0x12), context(mem).read(0));
            try {
                mem.loadImage(Path.of("/nonexistent/file.bin"), 0, 0);
                fail("Expected IOException");
            } catch (IOException ignored) {
            }
        } finally {
            mem.destroy();
        }
    }

    @Test
    public void saveCoreSettingsRewritesAndRemovesImages() {
        PluginSettingsMock settings = settings()
                .set("imageName0", "old0.bin").set("imageAddress0", 7)
                .set("imageName1", "old1.bin").set("imageAddress1", 8);
        MemoryImpl mem = createMemory(settings.mock());
        try {
            mem.saveCoreSettings(2, 100, List.of("a.bin", "b.bin"), List.of(1, 2), List.of(0, 1));
            assertEquals(2, settings.getInt("banksCount", 0));
            assertEquals(100, settings.getInt("commonBoundary", 0));
            assertEquals(Optional.of("b.bin"), settings.getString("imageName1"));
            assertEquals(Optional.of(1), settings.getInt("imageBank1"));
            mem.saveCoreSettings(1, 0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            assertFalse(settings.contains("imageName0"));
            assertFalse(settings.contains("imageName1"));
        } finally {
            mem.destroy();
        }
    }

    @Test
    public void saveRomRangesRewritesAndRemovesRanges() throws PluginInitializationException {
        PluginSettingsMock settings = settings();
        MemoryImpl mem = createMemory(settings.mock());
        try {
            mem.initialize();
            MemoryContextImpl context = context(mem);
            context.setReadOnly(new RangeTree.Range(10, 15));
            mem.saveROMRanges();
            assertEquals(Optional.of(10), settings.getInt("ROMfrom0"));
            assertEquals(Optional.of(15), settings.getInt("ROMto0"));
            context.setReadWrite(new RangeTree.Range(10, 15));
            mem.saveROMRanges();
            assertFalse(settings.contains("ROMfrom0"));
        } finally {
            mem.destroy();
        }
    }

    @Test
    public void guiSupportTracksNoGuiFlag() {
        MemoryImpl supported = createMemory(settings().mock());
        assertTrue(supported.isShowSettingsSupported());
        supported.destroy();
        MemoryImpl mem = createMemory(new PluginSettingsMock().set(PluginSettings.EMUSTUDIO_NO_GUI, true).mock());
        try {
            assertFalse(mem.isShowSettingsSupported());
            mem.showSettings(null);
        } finally {
            mem.destroy();
        }
    }

    private void assertInitialization(PluginSettingsMock settings, int banks, int commonBoundary)
            throws PluginInitializationException {
        MemoryImpl mem = createMemory(settings.mock());
        try {
            mem.initialize();
            assertEquals(256, mem.getSize());
            assertEquals(banks, context(mem).getBanksCount());
            assertEquals(commonBoundary, context(mem).getCommonBoundary());
        } finally {
            mem.destroy();
        }
    }

    private Path image(String name, byte... bytes) throws IOException {
        return Files.write(tempFolder.newFile(name).toPath(), bytes);
    }

    static MemoryImpl createMemory(PluginSettings settings) {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(createNiceMock(Dialogs.class)).anyTimes();
        replay(applicationApi);
        return new MemoryImpl(0, applicationApi, settings);
    }

    static MemoryContextImpl context(MemoryImpl memory) {
        try {
            Field field = MemoryImpl.class.getDeclaredField("context");
            field.setAccessible(true);
            return (MemoryContextImpl) field.get(memory);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static PluginSettingsMock settings() {
        return new PluginSettingsMock().set("banksCount", 1).set("commonBoundary", 0).set("memorySize", 256);
    }
}
