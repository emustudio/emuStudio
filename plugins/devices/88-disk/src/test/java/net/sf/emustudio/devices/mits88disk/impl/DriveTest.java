/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.devices.mits88disk.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DriveTest {
    private final static short SECTOR_SIZE = 2;
    private final static short SECTORS_COUNT = 2;
    private final static short TRACKS_COUNT = 2;
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File testImageFile;
    
    @Before
    public void prepareTestImage() throws Exception {
        testImageFile = folder.newFile();
        try (RandomAccessFile raf = new RandomAccessFile(testImageFile, "rw")) {
            for (int i = 0; i < SECTOR_SIZE * SECTORS_COUNT * TRACKS_COUNT; i++) {
                raf.write(i);
            }
        }
    }
    
    private void assertImageContent(int dataStart) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(testImageFile, "r")) {
            for (int i = 0; i < SECTOR_SIZE * SECTORS_COUNT * TRACKS_COUNT; i++) {
                assertEquals(dataStart++, raf.read());
            }
        }
    }

    @Test
    public void testInitialDriveParameters() throws Exception {
        Drive drive = new Drive();
        
        Drive.DriveParameters params = drive.getDriveParameters();
        assertEquals(0, params.sector);
        assertEquals(0, params.sectorOffset);
        assertEquals(0, params.track);
        assertNull(params.mountedFloppy);
        assertEquals(0xE7, params.port1status);
        assertEquals(0xC1, params.port2status);
    }
    
    @Test
    public void testMountValidImage() throws IOException {
        new Drive().mount(testImageFile);
    }
    
    @Test(expected = NullPointerException.class)
    public void testMountImageNullArgument() throws IOException {
        new Drive().mount(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMountImageNonExistantFile() throws IOException {
         new Drive().mount(new File("nonexistant"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMountImageFolder() throws IOException {
         new Drive().mount(folder.getRoot());
    }
    
    @Test
    public void testUnmountWithoutMountHasNoEffect() {
        new Drive().umount();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testUnmountSelectedDriveThrows() throws IOException {
        Drive drive = new Drive();
        drive.mount(testImageFile);
        drive.select();
        
        drive.umount();
    }
    
    @Test
    public void testUmountClearsMountImageInDriveParams() throws IOException {
        Drive drive = new Drive();
        drive.mount(testImageFile);
        drive.umount();
        
        Drive.DriveParameters params = drive.getDriveParameters();
        assertNull(params.mountedFloppy);
    }

    @Test
    public void testDriveParametersAfterSelect() throws Exception {
        Drive drive = new Drive();
        drive.setSectorsCount(SECTORS_COUNT);
        drive.setSectorLength(SECTOR_SIZE);

        drive.mount(testImageFile);
        drive.select();
        
        Drive.DriveParameters params = drive.getDriveParameters();
        
        assertEquals(0xA5, params.port1status);
        assertEquals(0xC1, params.port2status);
        assertEquals(0, params.sector);
        assertEquals(0, params.sectorOffset);
        assertEquals(0, params.track);
    }
    
    @Test
    public void testDriveParametersAfterSelectThenDeselect() throws Exception {
        Drive drive = new Drive();
        drive.setSectorsCount(SECTORS_COUNT);
        drive.setSectorLength(SECTOR_SIZE);

        drive.mount(testImageFile);
        drive.select();
        drive.deselect();
        
        Drive.DriveParameters params = drive.getDriveParameters();
        assertEquals(0, params.sector);
        assertEquals(0, params.sectorOffset);
        assertEquals(0, params.track);
        assertSame(testImageFile, params.mountedFloppy);
        assertEquals(0xE7, params.port1status);
        assertEquals(0xC1, params.port2status);
    }

    @Test(expected = IllegalStateException.class)
    public void testSelectWithoutMount() {
        Drive drive = new Drive();
        drive.select();
    }
    
    @Test
    public void testDeselectWithoutSelectHasNoEffect() {
        new Drive().deselect();
    }
    
    @Test
    public void testReadAllData() throws Exception {
        Drive drive = new Drive();
        drive.setSectorsCount(SECTORS_COUNT);
        drive.setSectorLength(SECTOR_SIZE);

        drive.mount(testImageFile);
        drive.select();
        
        assertEquals(0, drive.getPort1status() & 0x40); // track 0

        int expectedData = 0;
        for (int track = 0; track < TRACKS_COUNT; track++) {
            assertEquals(track, drive.getTrack());
            
            // head load
            drive.writeToPort2((short)0x04);            
            for (int sector = 0; sector < SECTORS_COUNT; sector++) {
                assertEquals(sector, drive.getSector());
                
                for (int offset = 0; offset < SECTOR_SIZE; offset++) {
                    assertEquals(offset, drive.getOffset());
                    assertEquals(expectedData++, drive.readData());
                }
                drive.nextSectorIfHeadIsLoaded();
            }
            // head unload
            drive.writeToPort2((short)0x08);
            // assert head can be moved
            assertEquals(0, drive.getPort1status() & 0x02);
            // increment track number
            drive.writeToPort2((short)0x01);
        }
    }
    
    @Test
    public void testWriteAllData() throws IOException, Exception {
        Drive drive = new Drive();
        drive.setSectorsCount(SECTORS_COUNT);
        drive.setSectorLength(SECTOR_SIZE);

        drive.mount(testImageFile);
        drive.select();
        
        assertEquals(0, drive.getPort1status() & 0x40); // track 0

        int writtenData = 1;
        for (int track = 0; track < TRACKS_COUNT; track++) {
            assertEquals(track, drive.getTrack());
            
            // head load
            drive.writeToPort2((short)0x04);
            assertEquals(0, drive.getPort1status() & 0x04); // head loaded

            for (int sector = 0; sector < SECTORS_COUNT; sector++) {
                assertEquals(sector, drive.getSector());
                
                // write enable
                drive.writeToPort2((short)0x80);
                for (int offset = 0; offset < SECTOR_SIZE; offset++) {
                    assertEquals(offset, drive.getOffset());
                    
                    // enter new write data ?
                    assertEquals(0, drive.getPort1status() & 0x01);

                    drive.writeData(writtenData++);
                }
                drive.nextSectorIfHeadIsLoaded();
            }
            // head unload
            drive.writeToPort2((short)0x08);
            // assert head can be moved
            assertEquals(0, drive.getPort1status() & 0x02);
            // increment track number
            drive.writeToPort2((short)0x01);
        }
        assertImageContent(1);
    }
    
}
