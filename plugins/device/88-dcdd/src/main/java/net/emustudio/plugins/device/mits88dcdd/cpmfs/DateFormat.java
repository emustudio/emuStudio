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
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

// https://www.seasip.info/Cpm/dosses.html
public enum DateFormat {
    NOT_USED,

    /**
     * 21 00 C1 C1 M1 M1 M1 M1 A1 A1 A1 A1 C2 C2 M2 M2
     * M2 M2 A2 A2 A2 A2 C3 C3 M3 M3 M3 M3 A3 A3 A3 A3
     * <p>
     * C1 = File 1 Create date
     * M1 = File 1 Modify date/time
     * A1 = File 1 Access date/time
     * C2 = File 2 Create date
     * M2 = File 2 Modify date/time
     * A2 = File 2 Access date/time
     * C3 = File 3 Create date
     * M3 = File 3 Modify date/time
     * A3 = File 3 Access date/time
     */
    NATIVE, // used in CP/M 2.2: Z80DOS, DOS+, P2DOS and CP/M Plus; every 4th entry

    /**
     * 21 C1 C1 C1 C1 M1 M1 M1 M1 00 00 C2 C2 C2 C2 M2
     * M2 M2 M2 00 00 C3 C3 C3 C3 M3 M3 M3 M3 00 00 00
     * <p>
     * https://manpages.debian.org/testing/cpmtools/cpm.5.en.html
     * <p>
     * A time stamp consists of two dates: Creation and modification date (the latter being recorded when the file is
     * closed). CP/M Plus further allows optionally to record the access instead of creation date as first time stamp.
     * - 2 bytes (little-endian) days starting with 1 at 01-01-1978
     * - 1 byte hour in BCD format
     * - 1 byte minute in BCD format
     * <p>
     * C1 = File 1 Create date
     * M1 = File 1 Modify date/time
     * C2 = File 2 Create date
     * M2 = File 2 Modify date/time
     * C3 = File 3 Create date
     * M3 = File 3 Modify date/time
     */
    NATIVE2, // P2DOS and CP/M Plus; every 4th entry

    /**
     * CP/M 3, sometimes ZSDOS
     */
    DATE_STAMPER // !!!TIME&.DAT; in 1st entry
}
