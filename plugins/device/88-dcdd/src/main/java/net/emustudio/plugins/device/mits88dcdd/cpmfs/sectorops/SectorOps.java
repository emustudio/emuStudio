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
}
