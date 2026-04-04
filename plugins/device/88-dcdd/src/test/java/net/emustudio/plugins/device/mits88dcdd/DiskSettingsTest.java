/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd;

import net.emustudio.emulib.runtime.settings.BasicSettings;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DiskSettingsTest {
    private DiskSettings diskSettings;

    @Before
    public void setUp() {
        BasicSettings basicSettings = createNiceMock(BasicSettings.class);
        // NiceMock returns 0 for int, false for boolean, null for String by default
        // DiskSettings constructor reads default values when settings return defaults
        expect(basicSettings.getInt(anyString(), anyInt())).andAnswer(() -> (int) getCurrentArguments()[1]).anyTimes();
        expect(basicSettings.getBoolean(anyString(), anyBoolean())).andAnswer(() -> (boolean) getCurrentArguments()[1]).anyTimes();
        expect(basicSettings.getString(anyString(), isNull())).andReturn(null).anyTimes();
        replay(basicSettings);

        diskSettings = new DiskSettings(basicSettings);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullSettings() {
        new DiskSettings(null);
    }

    @Test
    public void testDefaultPort1CPU() {
        assertEquals(DiskSettings.DEFAULT_CPU_PORT1, diskSettings.getPort1CPU());
    }

    @Test
    public void testDefaultPort2CPU() {
        assertEquals(DiskSettings.DEFAULT_CPU_PORT2, diskSettings.getPort2CPU());
    }

    @Test
    public void testDefaultPort3CPU() {
        assertEquals(DiskSettings.DEFAULT_CPU_PORT3, diskSettings.getPort3CPU());
    }

    @Test
    public void testDefaultInterruptVector() {
        assertEquals(DiskSettings.DEFAULT_INTERRUPT_VECTOR, diskSettings.getInterruptVector());
    }

    @Test
    public void testDefaultInterruptsSupported() {
        assertEquals(DiskSettings.DEFAULT_INTERRUPTS_SUPPORTED, diskSettings.getInterruptsSupported());
    }

    @Test
    public void testSetPort1CPU() {
        diskSettings.setPort1CPU(0x10);
        assertEquals(0x10, diskSettings.getPort1CPU());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPort1CPUNegativeThrows() {
        diskSettings.setPort1CPU(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPort1CPUTooLargeThrows() {
        diskSettings.setPort1CPU(0x100);
    }

    @Test
    public void testSetPort1CPUBoundaryValues() {
        diskSettings.setPort1CPU(0);
        assertEquals(0, diskSettings.getPort1CPU());
        diskSettings.setPort1CPU(0xFF);
        assertEquals(0xFF, diskSettings.getPort1CPU());
    }

    @Test
    public void testSetPort2CPU() {
        diskSettings.setPort2CPU(0x20);
        assertEquals(0x20, diskSettings.getPort2CPU());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPort2CPUNegativeThrows() {
        diskSettings.setPort2CPU(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPort2CPUTooLargeThrows() {
        diskSettings.setPort2CPU(0x100);
    }

    @Test
    public void testSetPort3CPU() {
        diskSettings.setPort3CPU(0x30);
        assertEquals(0x30, diskSettings.getPort3CPU());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPort3CPUNegativeThrows() {
        diskSettings.setPort3CPU(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPort3CPUTooLargeThrows() {
        diskSettings.setPort3CPU(0x100);
    }

    @Test
    public void testSetInterruptVector() {
        diskSettings.setInterruptVector(3);
        assertEquals(3, diskSettings.getInterruptVector());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInterruptVectorNegativeThrows() {
        diskSettings.setInterruptVector(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInterruptVectorTooLargeThrows() {
        diskSettings.setInterruptVector(8);
    }

    @Test
    public void testSetInterruptVectorBoundaryValues() {
        diskSettings.setInterruptVector(0);
        assertEquals(0, diskSettings.getInterruptVector());
        diskSettings.setInterruptVector(7);
        assertEquals(7, diskSettings.getInterruptVector());
    }

    @Test
    public void testSetInterruptsSupported() {
        diskSettings.setInterruptsSupported(false);
        assertFalse(diskSettings.getInterruptsSupported());
        diskSettings.setInterruptsSupported(true);
        assertTrue(diskSettings.getInterruptsSupported());
    }

    @Test
    public void testGetDriveSettings() {
        DiskSettings.DriveSettings ds = diskSettings.getDriveSettings(0);
        assertNotNull(ds);
        assertEquals(DiskSettings.DEFAULT_SECTOR_SIZE, ds.sectorSize);
        assertEquals(DiskSettings.DEFAULT_SECTORS_PER_TRACK, ds.sectorsPerTrack);
        assertNull(ds.imagePath);
        assertFalse(ds.mounted);
    }

    @Test
    public void testSetDriveSettings() {
        DiskSettings.DriveSettings ds = new DiskSettings.DriveSettings(256, 64, "/path/to/image.dsk", true);
        diskSettings.setDriveSettings(0, ds);

        DiskSettings.DriveSettings result = diskSettings.getDriveSettings(0);
        assertEquals(256, result.sectorSize);
        assertEquals(64, result.sectorsPerTrack);
        assertEquals("/path/to/image.dsk", result.imagePath);
        assertTrue(result.mounted);
    }

    @Test
    public void testSetDriveSettingsWithNullImagePath() {
        DiskSettings.DriveSettings ds = new DiskSettings.DriveSettings(128, 32, null, false);
        diskSettings.setDriveSettings(0, ds);

        DiskSettings.DriveSettings result = diskSettings.getDriveSettings(0);
        assertNull(result.imagePath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDriveSettingsNegativeDriveThrows() {
        diskSettings.setDriveSettings(-1, DiskSettings.DriveSettings.DEFAULT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDriveSettingsDrive16Throws() {
        diskSettings.setDriveSettings(16, DiskSettings.DriveSettings.DEFAULT);
    }

    @Test
    public void testSetDriveSettingsBoundaryDrives() {
        diskSettings.setDriveSettings(0, DiskSettings.DriveSettings.DEFAULT);
        diskSettings.setDriveSettings(15, DiskSettings.DriveSettings.DEFAULT);
    }

    @Test
    public void testObserverNotifiedOnPortChange() {
        AtomicBoolean notified = new AtomicBoolean(false);
        diskSettings.addObserver(() -> notified.set(true));

        diskSettings.setPort1CPU(0x10);
        assertTrue(notified.get());
    }

    @Test
    public void testObserverNotifiedOnPort2Change() {
        AtomicBoolean notified = new AtomicBoolean(false);
        diskSettings.addObserver(() -> notified.set(true));

        diskSettings.setPort2CPU(0x20);
        assertTrue(notified.get());
    }

    @Test
    public void testObserverNotifiedOnPort3Change() {
        AtomicBoolean notified = new AtomicBoolean(false);
        diskSettings.addObserver(() -> notified.set(true));

        diskSettings.setPort3CPU(0x30);
        assertTrue(notified.get());
    }

    @Test
    public void testObserverNotifiedOnInterruptVectorChange() {
        AtomicBoolean notified = new AtomicBoolean(false);
        diskSettings.addObserver(() -> notified.set(true));

        diskSettings.setInterruptVector(5);
        assertTrue(notified.get());
    }

    @Test
    public void testObserverNotifiedOnInterruptsSupportedChange() {
        AtomicBoolean notified = new AtomicBoolean(false);
        diskSettings.addObserver(() -> notified.set(true));

        diskSettings.setInterruptsSupported(false);
        assertTrue(notified.get());
    }

    @Test
    public void testObserverNotifiedOnDriveSettingsChange() {
        AtomicBoolean notified = new AtomicBoolean(false);
        diskSettings.addObserver(() -> notified.set(true));

        diskSettings.setDriveSettings(0, DiskSettings.DriveSettings.DEFAULT);
        assertTrue(notified.get());
    }

    @Test
    public void testClearObservers() {
        AtomicBoolean notified = new AtomicBoolean(false);
        diskSettings.addObserver(() -> notified.set(true));
        diskSettings.clearObservers();

        diskSettings.setPort1CPU(0x10);
        assertFalse(notified.get());
    }

    @Test
    public void testDriveSettingsDefaultConstant() {
        DiskSettings.DriveSettings def = DiskSettings.DriveSettings.DEFAULT;
        assertEquals(DiskSettings.DEFAULT_SECTOR_SIZE, def.sectorSize);
        assertEquals(DiskSettings.DEFAULT_SECTORS_PER_TRACK, def.sectorsPerTrack);
        assertNull(def.imagePath);
        assertFalse(def.mounted);
    }
}
