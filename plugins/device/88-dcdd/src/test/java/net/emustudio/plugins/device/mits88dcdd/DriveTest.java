/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.mits88dcdd;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88dcdd.drive.Drive;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveListener;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveParameters;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DriveTest {
    private final static short SECTOR_SIZE = 2;
    private final static short SECTORS_PER_TRACK = 2;
    private final static short TRACKS_COUNT = 2;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private Path testImageFile;

    @Before
    public void prepareTestImage() throws Exception {
        testImageFile = folder.newFile().toPath();
        try (RandomAccessFile raf = new RandomAccessFile(testImageFile.toFile(), "rw")) {
            for (int i = 0; i < SECTOR_SIZE * SECTORS_PER_TRACK * TRACKS_COUNT; i++) {
                raf.write(i);
            }
        }
    }

    private void assertImageContent(int dataStart) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(testImageFile.toFile(), "r")) {
            for (int i = 0; i < SECTOR_SIZE * SECTORS_PER_TRACK * TRACKS_COUNT; i++) {
                assertEquals(dataStart++, raf.read());
            }
        }
    }

    @Test
    public void testInitialDriveParameters() {
        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);

        DriveParameters params = drive.getDriveParameters();
        assertEquals(0, params.sector);
        assertEquals(0, params.sectorOffset);
        assertEquals(0, params.track);
        assertNull(params.mountedFloppy);
        assertEquals(0xE7, params.port1status & 0xFF);
        assertEquals(0xC1, params.port2status & 0xFF);
    }

    @Test
    public void testMountValidImage() throws IOException {
        new Drive(0, mock(Context8080.class), () -> 0).mount(testImageFile);
    }

    @Test(expected = NullPointerException.class)
    public void testMountImageNullArgument() throws IOException {
        new Drive(0, mock(Context8080.class), () -> 0).mount(null);
    }

    @Test
    public void testUnmountWithoutMountHasNoEffect() {
        new Drive(0, mock(Context8080.class), () -> 0).umount();
    }

    @Test
    public void testUnmountSelectedDriveDeselects() throws IOException {
        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.mount(testImageFile);
        drive.select();

        drive.umount();
        assertFalse(drive.isSelected());
    }

    @Test
    public void testUmountClearsMountImageInDriveParams() throws IOException {
        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.mount(testImageFile);
        drive.umount();

        DriveParameters params = drive.getDriveParameters();
        assertNull(params.mountedFloppy);
    }

    @Test
    public void testDriveParametersAfterSelect() throws Exception {
        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.setDriveSettings(new DiskSettings.DriveSettings(SECTOR_SIZE, SECTORS_PER_TRACK, null));

        drive.mount(testImageFile);
        drive.select();

        DriveParameters params = drive.getDriveParameters();

        assertEquals(0xA5, params.port1status & 0xFF);
        assertEquals(0xC1, params.port2status & 0xFF);
        assertEquals(0, params.sector);
        assertEquals(0, params.sectorOffset);
        assertEquals(0, params.track);
    }

    @Test
    public void testDriveParametersAfterSelectThenDeselect() throws Exception {
        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.setDriveSettings(new DiskSettings.DriveSettings(SECTOR_SIZE, SECTORS_PER_TRACK, null));

        drive.mount(testImageFile);
        drive.select();
        drive.deselect();

        DriveParameters params = drive.getDriveParameters();
        assertEquals(0, params.sector);
        assertEquals(0, params.sectorOffset);
        assertEquals(0, params.track);
        assertSame(testImageFile, params.mountedFloppy);
        assertEquals(0xE7, params.port1status & 0xFF);
        assertEquals(0xC1, params.port2status & 0xFF);
    }

    @Test
    public void testSelectWithoutMount() {
        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.select();
        assertFalse(drive.isSelected());
    }

    @Test
    public void testDeselectWithoutSelectHasNoEffect() {
        new Drive(0, mock(Context8080.class), () -> 0).deselect();
    }

    @Test
    public void testReadAllData() throws Exception {
        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.setDriveSettings(new DiskSettings.DriveSettings(SECTOR_SIZE, SECTORS_PER_TRACK, null));

        drive.mount(testImageFile);
        drive.select();

        assertEquals(0, drive.getPort1status() & 0x40); // track 0

        int expectedData = 0;
        for (int track = 0; track < TRACKS_COUNT; track++) {
            assertEquals(track, drive.getTrack());

            // head load
            drive.writeToPort2((short) 0x04);
            for (int sector = 0; sector < SECTORS_PER_TRACK; sector++) {
                assertEquals(sector, drive.getSector());

                for (int offset = 0; offset < SECTOR_SIZE; offset++) {
                    assertEquals(offset, drive.getOffset());
                    assertEquals(expectedData++, drive.readData());
                }
                drive.nextSectorIfHeadIsLoaded();
            }
            // head unload
            drive.writeToPort2((short) 0x08);
            // assert head can be moved
            assertEquals(0, drive.getPort1status() & 0x02);
            // increment track number
            drive.writeToPort2((short) 0x01);
        }
    }

    @Test
    public void testWriteAllData() throws Exception {
        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.setDriveSettings(new DiskSettings.DriveSettings(SECTOR_SIZE, SECTORS_PER_TRACK, null));

        drive.mount(testImageFile);
        drive.select();

        assertEquals(0, drive.getPort1status() & 0x40); // track 0

        int writtenData = 1;
        for (int track = 0; track < TRACKS_COUNT; track++) {
            assertEquals(track, drive.getTrack());

            // head load
            drive.writeToPort2((short) 0x04);
            assertEquals(0, drive.getPort1status() & 0x04); // head loaded

            for (int sector = 0; sector < SECTORS_PER_TRACK; sector++) {
                assertEquals(sector, drive.getSector());

                // write enable
                drive.writeToPort2((short) 0x80);
                for (int offset = 0; offset < SECTOR_SIZE; offset++) {
                    assertEquals(offset, drive.getOffset());

                    // enter new write data ?
                    assertEquals(0, drive.getPort1status() & 0x01);

                    drive.writeData(writtenData++);
                }
                drive.nextSectorIfHeadIsLoaded();
            }
            // head unload
            drive.writeToPort2((short) 0x08);
            // assert head can be moved
            assertEquals(0, drive.getPort1status() & 0x02);
            // increment track number
            drive.writeToPort2((short) 0x01);
        }
        assertImageContent(1);
    }

    @Test
    public void testDriveListenerIsCalledWhenDiskIsSelected() throws Exception {
        DriveListener listener = EasyMock.createMock(DriveListener.class);
        listener.driveSelect(true);
        expectLastCall().once();
        listener.driveParamsChanged(anyObject(DriveParameters.class));
        expectLastCall().once();
        replay(listener);

        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.addDriveListener(listener);
        drive.mount(testImageFile);
        drive.select();

        verify(listener);
    }

    @Test
    public void testDriveListenerIsCalledWhenDiskIsDeselected() throws Exception {
        DriveListener listener = EasyMock.createMock(DriveListener.class);
        listener.driveSelect(false);
        expectLastCall().once();
        listener.driveParamsChanged(anyObject(DriveParameters.class));
        expectLastCall().once();
        replay(listener);

        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.mount(testImageFile);
        drive.select();
        drive.addDriveListener(listener);
        drive.deselect();

        verify(listener);
    }

    @Test
    public void testDriveListenerIsCalledWhenSectorNumberIsChanged() throws Exception {
        DriveListener listener = EasyMock.createMock(DriveListener.class);
        listener.driveParamsChanged(anyObject(DriveParameters.class));
        expectLastCall().once();
        replay(listener);

        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.mount(testImageFile);
        drive.select();
        drive.addDriveListener(listener);
        drive.nextSectorIfHeadIsLoaded();

        verify(listener);
    }

    @Test
    public void testDriveListenerIsCalledWhenDataAreRead() throws Exception {
        DriveListener listener = EasyMock.createMock(DriveListener.class);
        listener.driveParamsChanged(anyObject(DriveParameters.class));
        expectLastCall().once();
        replay(listener);

        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.mount(testImageFile);
        drive.select();
        drive.writeToPort2((short) 0x04);
        drive.addDriveListener(listener);
        drive.readData();

        verify(listener);
    }

    @Test
    public void testDriveListenerIsCalledWhenDataAreWritten() throws Exception {
        DriveListener listener = EasyMock.createMock(DriveListener.class);
        listener.driveParamsChanged(anyObject(DriveParameters.class));
        expectLastCall().once();
        replay(listener);

        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.mount(testImageFile);
        drive.select();
        drive.writeToPort2((short) 0x04);
        drive.writeToPort2((short) 0x80);
        drive.addDriveListener(listener);
        drive.writeData(1);

        verify(listener);
    }

    @Test
    public void testDriveListenerIsCalledWhenFlagsAreSet() throws Exception {
        DriveListener listener = EasyMock.createMock(DriveListener.class);
        listener.driveParamsChanged(anyObject(DriveParameters.class));
        expectLastCall().once();
        replay(listener);

        Drive drive = new Drive(0, mock(Context8080.class), () -> 0);
        drive.mount(testImageFile);
        drive.select();
        drive.addDriveListener(listener);
        drive.writeToPort2((short) 0x04);

        verify(listener);
    }
}
