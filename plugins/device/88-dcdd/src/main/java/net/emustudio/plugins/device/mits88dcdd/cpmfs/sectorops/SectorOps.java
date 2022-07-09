package net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.Position;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Special sector operations performed by the CP/M BIOS.
 */
public interface SectorOps {
    SectorOps DUMMY = new SectorOps() {
        @Override
        public ByteBuffer toSector(ByteBuffer record, Position position) {
            return record;
        }

        @Override
        public ByteBuffer toRecord(ByteBuffer sector, Position position) {
            return sector;
        }
    };

    /**
     * Converts record of max RECORD_SIZE bytes to raw sector.
     * the record size might be less than RECORD_SIZE.
     *
     * @param record   CP/M record
     * @param position position
     * @return raw sector with correct length
     */
    ByteBuffer toSector(ByteBuffer record, Position position);

    /**
     * Converts sector of sectorSize to RECORD_SIZE record.
     *
     * @param sector   sector
     * @param position position
     * @return record
     */
    ByteBuffer toRecord(ByteBuffer sector, Position position);


    static SectorOps fromString(String name) {
        switch (name.toLowerCase(Locale.ENGLISH)) {
            case "altair-floppy-mits":
                return Altair8mits.INSTANCE;
            case "altair-floppy-deramp":
                return Altair8deramp.INSTANCE;
            case "altair-minidisk-deramp":
                return AltairMinidiskDeramp.INSTANCE;
            default:
                return DUMMY;
        }

    }
}
