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
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 88-DCDD
 *
 * Supported:
 *  - Altair 8" floppy disks: 77 tracks, 32 sectors, 137 bytes sector size; skew: 6
 *  - Altair Minidisk: 35 tracks, 16 sectors, 137 bytes sector size
 *
 * Raw sector size: 137 bytes
 * Sectors/Track: 32, numbered 0-31
 * Tracks/Diskette: 77, numbered 0-76
 *
 * Tracks 0-5 are formatted as "System Tracks" (regardless of how they are actually used). Sectors on these tracks are
 * formmatted as follows:
 *
 *      Byte    Value
 *       0      Track number + 80h
 *      1-2     Sixteen bit address in memory of the end of the bootloader (0x100). This same value is set in all
 *              sectors of tracks 0‐5.
 *     3-130    Data (128 bytes)
 *      131     0FFh (Stop Byte)
 *      132     Checksum of 3-130 (sum of the 128 byte payload)
 *     133-136  Not used
 *
 * Tracks 6-76 (except track 70) are "Data Tracks." Sectors on these tracks are formatted as follows:
 *
 *  Byte    Value
 *     0      Track number + 80h
 *     1      Skewed sector = (Sector number * 17) MOD 32
 *     2      File number in directory (or not used)
 *     3      Data byte count (or not used)
 *     4      Checksum of 2-3 & 5-134
 *    5-6     Pointer to next data group (or not used)
 *   7-134    Data (128 bytes)
 *    135     0FFh (Stop Byte)
 *    136     00h (Stop byte)
 *
 * Track 70 is the Altair Basic/DOS directory track. It is formatted the same as the Data Tracks, except that each Data
 * field is divided into 8 16-byte directory entries. The last 5 of these 16 bytes are written as 0 by most versions of Altair
 * Basic and DOS, but are used as a password by Multiuser Basic, where five 0's means "no password". Unfortunately, single-
 * user Basic does not always clear these bytes. If these bytes are not all 0 For a given directory entry, then multiuser
 * Basic will not be able to access the file. /P fixes this. The first directory entry that has FFh as its first byte is the
 * end-of-directory marker. (This FFh is called "the directory stopper byte.")
 */
public class DriveIO implements AutoCloseable {
    public final int RAW_SECTOR_SIZE = 137;

    public final CpmFormat cpmFormat;
    private final FileChannel channel;

    public DriveIO(Path imageFile, CpmFormat cpmFormat, OpenOption... openOptions) throws IOException {
        this.cpmFormat = Objects.requireNonNull(cpmFormat);
        this.channel = FileChannel.open(Objects.requireNonNull(imageFile), openOptions);
    }

    /**
     * Reads a CP/M "record".
     *
     * It is a raw sector stripped from prefix & suffix.
     * It does not check validity.
     *
     * @param position logical position
     * @return record data
     * @throws IOException on reading error
     */
    public ByteBuffer readRecord(Position position) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(RAW_SECTOR_SIZE);
        channel.position(cpmFormat.positionToOffset(position));
        if (channel.read(buffer) != RAW_SECTOR_SIZE) {
            throw new IOException("Could not read whole sector! (" + position + ")");
        }
        return cpmFormat.sectorOps.toRecord(buffer.flip());
    }

    /**
     * Writes a CP/M "record"
     *
     * @param position logical position
     * @param data     record data
     * @throws IOException              on writing error
     * @throws IllegalArgumentException if sector data does not have expected size (sector size)
     */
    public void writeRecord(Position position, ByteBuffer data) throws IOException {
        ByteBuffer sector = cpmFormat.sectorOps.toSector(data, position);
        channel.position(cpmFormat.positionToOffset(position));

        int expected = sector.remaining();
        if (channel.write(sector) != expected) {
            throw new IOException("Could not write whole sector! (" + position + ")");
        }
    }

    /**
     * Reads a block
     *
     * @param blockNumber block number
     * @return list of records in a block
     * @throws IOException on reading error
     */
    public List<ByteBuffer> readBlock(int blockNumber) throws IOException {
        Position position = cpmFormat.blockToPosition(blockNumber);
        List<ByteBuffer> block = new ArrayList<>();
        for (int counter = 0; counter < cpmFormat.recordsPerBlock; counter++) {
            block.add(readRecord(position));
            position.next(cpmFormat.dpb.spt);
        }
        return block;
    }

    /**
     * Writes a block
     *
     * @param blockNumber block number
     * @param records list of records
     * @throws IOException on writing error
     */
    public void writeBlock(int blockNumber, List<ByteBuffer> records) throws IOException {
        if (records.size() > cpmFormat.recordsPerBlock) {
            throw new IllegalArgumentException("Too many sectors per block");
        }

        Position position = cpmFormat.blockToPosition(blockNumber);
        for (ByteBuffer record : records) {
            writeRecord(position, record);
            position.next(cpmFormat.dpb.spt);
        }
    }

    @Override
    public void close() throws Exception {
        channel.close();
    }
}
