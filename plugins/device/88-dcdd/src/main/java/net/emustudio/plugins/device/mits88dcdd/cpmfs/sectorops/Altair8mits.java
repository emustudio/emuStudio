/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.Position;
import net.jcip.annotations.NotThreadSafe;

import java.nio.ByteBuffer;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.RECORD_SIZE;

/**
 * MITS 88-DCDD sector ops.
 * <p>
 * Sector has 137 bytes, and the record (128 bytes) was placed inside, possibly with some offset.
 * The rest of bytes (prefix and suffix of the record) had often a special meaning. However all this
 * prefixing/postfixing was performed in software; it depends on specific CP/M implementation.
 * <p>
 * <p>
 * <p>
 * Tracks 0-5 are formatted as "System Tracks" (regardless of how they are actually used). Sectors on these tracks are
 * formatted as follows:
 * <p>
 * Byte    Value
 * 0      Track number + 80h
 * 1-2     Sixteen bit address in memory of the end of the bootloader (0x100). This same value is set in all
 * sectors of tracks 0‐5.
 * 3-130    Data (128 bytes)
 * 131     0FFh (Stop Byte)
 * 132     Checksum of 3-130 (sum of the 128 byte payload)
 * 133-136  Not used
 * <p>
 * Tracks 6-76 (except track 70) are "Data Tracks." Sectors on these tracks are formatted as follows:
 * <p>
 * Byte    Value
 * 0      Track number + 80h
 * 1      Skewed sector = (Sector number * 17) MOD 32
 * 2      File number in directory (or not used)
 * 3      Data byte count (or not used)
 * 4      Checksum of 2-3 & 5-134
 * 5-6     Pointer to next data group (or not used)
 * 7-134    Data (128 bytes)
 * 135     0FFh (Stop Byte)
 * 136     00h (Stop byte)
 * <p>
 * Track 70 is the Altair Basic/DOS directory track. It is formatted the same as the Data Tracks, except that each Data
 * field is divided into 8 16-byte directory entries. The last 5 of these 16 bytes are written as 0 by most versions of Altair
 * Basic and DOS, but are used as a password by Multiuser Basic, where five 0's means "no password". Unfortunately, single-
 * user Basic does not always clear these bytes. If these bytes are not all 0 For a given directory entry, then multiuser
 * Basic will not be able to access the file. /P fixes this. The first directory entry that has FFh as its first byte is the
 * end-of-directory marker. (This FFh is called "the directory stopper byte.")
 */
@NotThreadSafe
public class Altair8mits implements SectorOps {
    public static final int SECTOR_SIZE = 137;
    public static final SectorOps INSTANCE = new Altair8mits();

    @Override
    public ByteBuffer toSector(ByteBuffer record, Position position) {
        // https://retrocmp.de/hardware/altair-8800/altair-floppy.htm
        record.limit(RECORD_SIZE);
        record.position(0);

        ByteBuffer sector = ByteBuffer.allocate(SECTOR_SIZE); // sector length >= RECORD_SIZE
        sector.put((byte) (position.track | 0x80)); // Track Number, with MSB set (the sync bit)
        sector.put((byte) ((position.sector * 17) % 32)); // sector number; or used/unused size...?
        sector.put((byte) 0);
        sector.put(record);

        record.flip();
        int checksum = 0;
        for (int i = 0; i < record.remaining(); i++) {
            checksum = (checksum + record.get()) & 0xFF;
        }
        sector.put((byte) 0xFF); // stop byte
        sector.put((byte) checksum);

        record.position(0);
        sector.position(0);
        sector.limit(sector.capacity());
        return sector;
    }

    @Override
    public ByteBuffer toRecord(ByteBuffer sector, Position position) {
        sector.position(3);
        byte[] record = new byte[RECORD_SIZE];
        sector.get(record);
        sector.position(0);
        return ByteBuffer.wrap(record);
    }
}
