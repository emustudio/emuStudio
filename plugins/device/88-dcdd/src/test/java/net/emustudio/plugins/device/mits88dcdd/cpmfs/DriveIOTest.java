/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops.SectorOps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.*;

/**
 * Tests for DriveIO using DUMMY sector ops (sector == record, sectorSize == 128).
 */
public class DriveIOTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CpmFormat cpmFormat;
    private Path imageFile;

    @Before
    public void setup() throws IOException {
        // Small format: bsh=3, dsm=59, drm=31, spt=16, ofs=2, sectorSize=128
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(16, 16, 3, 59, 31, 0x80, 0, 2);
        cpmFormat = new CpmFormat("test", dpb, 128, 1, Collections.emptyList(),
                SectorOps.DUMMY, false, DateFormat.NOT_USED);

        imageFile = folder.newFile("test.dsk").toPath();
        java.nio.file.Files.delete(imageFile);
        DriveIO.format(imageFile, cpmFormat);
    }

    @Test
    public void testFormatCreatesFile() {
        assertTrue(java.nio.file.Files.exists(imageFile));
    }

    @Test
    public void testFormattedDiskFilledWithE5() throws Exception {
        try (DriveIO io = new DriveIO(imageFile, cpmFormat, READ)) {
            // Read block 0 (first directory block after ofs tracks)
            List<ByteBuffer> block = io.readBlock(0);
            assertFalse(block.isEmpty());
            ByteBuffer firstRecord = block.get(0);
            for (int i = 0; i < firstRecord.remaining(); i++) {
                assertEquals("byte " + i, (byte) 0xE5, firstRecord.get());
            }
        }
    }

    @Test
    public void testReadWriteBlockRoundTrip() throws Exception {
        try (DriveIO io = new DriveIO(imageFile, cpmFormat, READ, WRITE)) {
            // Write a block with known data
            int blockNumber = 1; // first data block after directory
            ByteBuffer record = ByteBuffer.allocate(CpmFormat.RECORD_SIZE);
            for (int i = 0; i < CpmFormat.RECORD_SIZE; i++) {
                record.put((byte) (i & 0xFF));
            }
            record.flip();

            // A block has recordsPerBlock records
            List<ByteBuffer> recordsToWrite = new java.util.ArrayList<>();
            recordsToWrite.add(record);
            // Pad with empty records for the rest
            for (int r = 1; r < cpmFormat.recordsPerBlock; r++) {
                ByteBuffer empty = ByteBuffer.allocate(CpmFormat.RECORD_SIZE);
                empty.flip();
                recordsToWrite.add(empty);
            }

            io.writeBlock(blockNumber, recordsToWrite);

            // Read it back
            List<ByteBuffer> readBack = io.readBlock(blockNumber);
            assertEquals(cpmFormat.recordsPerBlock, readBack.size());

            ByteBuffer first = readBack.get(0);
            for (int i = 0; i < CpmFormat.RECORD_SIZE; i++) {
                assertEquals("byte " + i, (byte) (i & 0xFF), first.get());
            }
        }
    }

    @Test
    public void testReadWriteRecordRoundTrip() throws Exception {
        try (DriveIO io = new DriveIO(imageFile, cpmFormat, READ, WRITE)) {
            Position pos = cpmFormat.blockToPosition(2);
            ByteBuffer record = ByteBuffer.allocate(CpmFormat.RECORD_SIZE);
            for (int i = 0; i < CpmFormat.RECORD_SIZE; i++) {
                record.put((byte) ((i + 42) & 0xFF));
            }
            record.flip();

            io.writeRecord(pos, record);

            Position pos2 = cpmFormat.blockToPosition(2);
            ByteBuffer readBack = io.readRecord(pos2);
            assertEquals(CpmFormat.RECORD_SIZE, readBack.remaining());
            for (int i = 0; i < CpmFormat.RECORD_SIZE; i++) {
                assertEquals((byte) ((i + 42) & 0xFF), readBack.get());
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatExistingFileThrows() throws IOException {
        // File already exists from setup
        DriveIO.format(imageFile, cpmFormat);
    }
}

