/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
}
