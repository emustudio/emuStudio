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
package net.emustudio.plugins.device.simh;

import net.emustudio.emulib.plugins.memory.MemoryContext;

public class CpmUtils {
    private final static int CPM_COMMAND_LINE_LENGTH = 128;
    public static final char[] cpmCommandLine = new char[CPM_COMMAND_LINE_LENGTH];

    public static void readCPMCommandLine(MemoryContext<Byte> memory) {
        int i;
        int len = memory.read(0x80) & 0x7F; // 0x80 contains length of command line, discard first char
        for (i = 0; i < len - 1; i++) {
            cpmCommandLine[i] = (char) memory.read(0x82 + i).byteValue(); // the first char, typically ' ', is discarded
        }
        cpmCommandLine[i] = 0; // make C string
    }


    /* The CP/M command line is used as the name of a file and UNIT* uptr is attached to it. */
//    public static void attachCPM(MemoryContext<Byte> memory, UNIT *uptr) {
//        createCPMCommandLine(memory);
//        if (uptr == &ptr_unit)
//            sim_switches = SWMASK('R') | SWMASK('Q');
//        else if (uptr == &ptp_unit)
//            sim_switches = SWMASK('W') | SWMASK('N') | SWMASK('Q');
//        /* 'N' option makes sure that file is properly truncated if it had existed before   */
//        sim_quiet = sim_switches & SWMASK ('Q');    /* -q means quiet                       */
//        lastCPMStatus = attach_unit(uptr, cpmCommandLine);
//        if (lastCPMStatus != SCPE_OK) {
//            sim_debug(VERBOSE_MSG, &simh_device, "SIMH: " ADDRESS_FORMAT
//            " Cannot open '%s' (%s).\n", PCX, cpmCommandLine,
//                sim_error_text(lastCPMStatus));
//        }
//    }
}
