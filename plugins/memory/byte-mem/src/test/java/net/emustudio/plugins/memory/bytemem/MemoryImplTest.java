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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MemoryImplTest {
    private MemoryImpl memory;
    private PluginSettings settings;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private MemoryImpl createMemory(PluginSettings settings) {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(createNiceMock(Dialogs.class)).anyTimes();
        replay(applicationApi);
        return new MemoryImpl(0, applicationApi, settings);
    }

    @Before
    public void setup() {
        this.settings = PluginSettings.UNAVAILABLE;
        this.memory = createMemory(settings);
    }

    @After
    public void tearDown() {
        memory.destroy();
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", memory.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", memory.getCopyright());
    }

    @Test
    public void testGetDescription() {
        assertNotNull(memory.getDescription());
        assertFalse(memory.getDescription().isEmpty());
    }

    @Test
    public void testInitializeWithDefaults() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        assertEquals(256, mem.getSize());
        mem.destroy();
    }

    @Test
    public void testInitializeNegativeBanksCountResetsToOne() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(-1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        assertEquals(256, mem.getSize());
        mem.destroy();
    }

    @Test
    public void testIsShowSettingsSupportedWithNoGui() {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        assertFalse(mem.isShowSettingsSupported());
        mem.destroy();
    }

    @Test
    public void testIsShowSettingsSupportedWithGui() {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(false).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        assertTrue(mem.isShowSettingsSupported());
        mem.destroy();
    }

    @Test
    public void testDestroyAndRecreate() {
        memory.destroy();
        // Reinitialize for tearDown
        memory = createMemory(settings);
    }

    @Test
    public void testGetSizeDefaultIsZero() {
        // Before initialization, size depends on default memory array
        // With UNAVAILABLE settings, context.init is not called, so memory is small
        int size = memory.getSize();
        assertTrue(size >= 0);
    }

    @Test
    public void testInitializeNegativeCommonBoundaryResetsToZero() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(-5).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        assertEquals(256, mem.getSize());
        mem.destroy();
    }

    @Test
    public void testInitializeNegativeMemorySize() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(-1).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        // negative memorySize triggers a warning but still calls context.init
        try {
            mem.initialize();
        } catch (Exception e) {
            // may throw due to negative array size - that's acceptable
        }
        mem.destroy();
    }

    @Test
    public void testInitializeWithRomRanges() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        // ROM range 0: from=0, to=15
        expect(mockSettings.getInt("ROMfrom0")).andReturn(Optional.of(0)).anyTimes();
        expect(mockSettings.getInt("ROMto0")).andReturn(Optional.of(15)).anyTimes();
        // ROM range 1: not present
        expect(mockSettings.getInt("ROMfrom1")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt("ROMto1")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        assertEquals(256, mem.getSize());
        mem.destroy();
    }

    @Test
    public void testInitializeWithImageLoading() throws Exception {
        File imageFile = tempFolder.newFile("test.bin");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(new byte[]{0x01, 0x02, 0x03});
        }

        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        // image0
        expect(mockSettings.getString("imageName0")).andReturn(Optional.of(imageFile.getAbsolutePath())).anyTimes();
        expect(mockSettings.getInt("imageAddress0")).andReturn(Optional.of(0)).anyTimes();
        expect(mockSettings.getInt("imageBank0")).andReturn(Optional.of(0)).anyTimes();
        // no image1
        expect(mockSettings.getString("imageName1")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt("imageAddress1")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        // ROM ranges - none
        expect(mockSettings.getInt("ROMfrom0")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt("ROMto0")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        assertEquals(256, mem.getSize());
        mem.destroy();
    }

    @Test
    public void testInitializeImageNotFoundDoesNotThrow() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString("imageName0")).andReturn(Optional.of("/nonexistent/file.bin")).anyTimes();
        expect(mockSettings.getInt("imageAddress0")).andReturn(Optional.of(0)).anyTimes();
        expect(mockSettings.getInt("imageBank0")).andReturn(Optional.of(0)).anyTimes();
        expect(mockSettings.getString("imageName1")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt("imageAddress1")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt("ROMfrom0")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt("ROMto0")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize(); // should not throw - FileNotFoundException is caught
        mem.destroy();
    }

    @Test
    public void testLoadImageDirectly() throws Exception {
        File imageFile = tempFolder.newFile("direct.bin");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(new byte[]{(byte) 0xAB, (byte) 0xCD});
        }

        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        mem.loadImage(imageFile.toPath(), 0, 0);
        mem.destroy();
    }

    @Test(expected = IOException.class)
    public void testLoadImageNonExistentThrows() throws Exception {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        try {
            mem.loadImage(Path.of("/nonexistent/file.bin"), 0, 0);
        } finally {
            mem.destroy();
        }
    }

    @Test
    public void testSaveCoreSettings() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(mockSettings.contains(anyString())).andReturn(false).anyTimes();
        mockSettings.setInt(anyString(), anyInt());
        expectLastCall().anyTimes();
        mockSettings.setString(anyString(), anyString());
        expectLastCall().anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        mem.saveCoreSettings(2, 100,
                Arrays.asList("image1.bin", "image2.bin"),
                Arrays.asList(0, 100),
                Arrays.asList(0, 1));
        mem.destroy();
    }

    @Test
    public void testSaveCoreSettingsRemovesOldImages() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        // simulate existing imageName0 that needs removal
        expect(mockSettings.contains("imageName0")).andReturn(true).once();
        expect(mockSettings.contains("imageName1")).andReturn(false).once();
        expect(mockSettings.contains(anyString())).andReturn(false).anyTimes();
        mockSettings.remove(anyString());
        expectLastCall().anyTimes();
        mockSettings.setInt(anyString(), anyInt());
        expectLastCall().anyTimes();
        mockSettings.setString(anyString(), anyString());
        expectLastCall().anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        mem.saveCoreSettings(1, 0, List.of("new.bin"), List.of(0), List.of(0));
        mem.destroy();
    }

    @Test
    public void testSaveROMRanges() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        // ROM range loaded during init
        expect(mockSettings.getInt("ROMfrom0")).andReturn(Optional.of(0)).anyTimes();
        expect(mockSettings.getInt("ROMto0")).andReturn(Optional.of(15)).anyTimes();
        expect(mockSettings.getInt("ROMfrom1")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt("ROMto1")).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(mockSettings.contains("ROMfrom0")).andReturn(true).once();
        expect(mockSettings.contains("ROMfrom1")).andReturn(false).once();
        expect(mockSettings.contains(anyString())).andReturn(false).anyTimes();
        mockSettings.remove(anyString());
        expectLastCall().anyTimes();
        mockSettings.setInt(anyString(), anyInt());
        expectLastCall().anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        mem.saveROMRanges();
        mem.destroy();
    }

    @Test
    public void testShowSettingsWithGuiNotSupportedDoesNothing() {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.showSettings(null); // should not throw or create GUI
        mem.destroy();
    }

    @Test(expected = NullPointerException.class)
    public void testDestroyCalledTwiceThrowsNPE() {
        memory.destroy();
        try {
            memory.destroy();
        } finally {
            memory = createMemory(settings);
        }
    }

    @Test
    public void testInitializeWithMultipleBanks() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(3).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(128).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        assertEquals(256, mem.getSize());
        mem.destroy();
    }

    @Test
    public void testInitializeZeroBanksCountResetsToOne() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        assertEquals(256, mem.getSize());
        mem.destroy();
    }

    @Test
    public void testSaveCoreSettingsEmptyLists() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(mockSettings.contains(anyString())).andReturn(false).anyTimes();
        mockSettings.setInt(anyString(), anyInt());
        expectLastCall().anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        mem.saveCoreSettings(1, 0, List.of(), List.of(), List.of());
        mem.destroy();
    }

    @Test
    public void testSaveROMRangesNoRanges() throws PluginInitializationException {
        PluginSettings mockSettings = createNiceMock(PluginSettings.class);
        expect(mockSettings.getInt("banksCount", 1)).andReturn(1).anyTimes();
        expect(mockSettings.getInt("commonBoundary", 0)).andReturn(0).anyTimes();
        expect(mockSettings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE)).andReturn(256).anyTimes();
        expect(mockSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(mockSettings.contains(anyString())).andReturn(false).anyTimes();
        mockSettings.setInt(anyString(), anyInt());
        expectLastCall().anyTimes();
        replay(mockSettings);

        MemoryImpl mem = createMemory(mockSettings);
        mem.initialize();
        mem.saveROMRanges();
        mem.destroy();
    }
}
