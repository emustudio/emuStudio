package net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.Position;

import java.nio.ByteBuffer;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.RECORD_SIZE;

/**
 * https://deramp.com/downloads/altair/software/8_inch_floppy/CPM/CPM%203.0/BIOS.ASM
 *
 * Altair 8" floppy disk (from deramp.com)
 *
 * Tracks 0-5 are formatted as "System Tracks" (regardless of how they are actually used). Sectors on these tracks are
 * formatted as follows:
 *
 *    Byte   Value
 *       0   0
 *       1   1
 *   2-130   Data (128 bytes)
 *     131   0FFh (Stop Byte)
 *     132   Checksum of 2-130 (sum of the 128 byte payload)
 * 133-136   Not used
 *
 * Tracks 6-76 (except track 70) are "Data Tracks." Sectors on these tracks are formatted as follows:
 *
 *  Byte   Value
 *     0   Logical sector number (not skewed)
 *   1-5   0
 * 6-134   Data (128 bytes)
 *   135   0FFh (Stop Byte)
 *   136   Checksum of 6-134 (sum of the 128 byte payload)
 */
public class Altair8deramp implements SectorOps {
    public static final int SECTOR_SIZE = 137;
    public static final SectorOps INSTANCE = new Altair8deramp();

    //; Create Altair sector for system tracks 0-5 (mini disk 0-3)
    //; wDatTrk- Create Altair sector for tracks 6-76 (mindisk 4-34)


    @Override
    public ByteBuffer toSector(ByteBuffer record, Position position) {
        record.limit(RECORD_SIZE);
        record.position(0);

        int checksum = 0;
        for (int i = 0; i < record.remaining(); i++) {
            checksum = (checksum + record.get()) & 0xFF;
        }
        record.flip();

        ByteBuffer sector = ByteBuffer.allocate(SECTOR_SIZE); // sector length >= RECORD_SIZE

        if (position.track <= 5) {
            sector.put((byte) 0); // put 0100h (16 bit) at offset 1,2
            sector.put((byte) 1);

            sector.put(record); // 128 byte CPM sector
            sector.put((byte) 0xFF); // offset 131 is stop byte (0FFh)
            sector.put((byte) checksum); // offset 132 is checksum
        } else {
            sector.put((byte) position.sector); // store logical sector number (before skew)
            sector.put((byte) 0); // store zero at offset 2
            sector.put((byte) 0); // store zero at offset 3
            sector.put((byte) 0); // store zero at offset 4
            sector.put((byte) 0); // store zero at offset 5
            sector.put((byte) 0); // store zero at offset 6
            sector.put(record); // 128 byte CPM sector
            sector.put((byte) 0xFF); // offset 135 is stop byte (0FFh)
            sector.put((byte) checksum); // offset 136 is checksum
        }

        record.position(0);
        sector.position(0);
        sector.limit(sector.capacity());
        return sector;
    }

    @Override
    public ByteBuffer toRecord(ByteBuffer sector, Position position) {
        byte[] record = new byte[RECORD_SIZE];

        if (position.track <= 5) {
            sector.position(2);
        } else {
            sector.position(6);
        }

        sector.get(record);
        sector.position(0);
        return ByteBuffer.wrap(record);
    }
}
