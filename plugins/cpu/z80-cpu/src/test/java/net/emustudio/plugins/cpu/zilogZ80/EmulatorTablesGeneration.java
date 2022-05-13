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
package net.emustudio.plugins.cpu.zilogZ80;

import org.junit.Ignore;
import org.junit.Test;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;

@Ignore
public class EmulatorTablesGeneration {

    @Test
    public void generateTableSub() {
        StringBuilder table = new StringBuilder("public final static int[] TABLE_SUB = new int[] {\n        ");

        for (int sum = 0; sum < 256; sum++) {
            int flagZ = (sum == 0 ? FLAG_Z : 0);
            int flags = flagZ | (sum & 0x80) | FLAG_N;
            table.append("0x")
                .append(Integer.toHexString(flags))
                .append(", ");
        }
        table.append("};\n");
        System.out.println(table);
    }

    @Test
    public void generateTableCHP() {
        StringBuilder table = new StringBuilder("public final static int[] TABLE_CHP = new int[] {\n        ");

        for (int cbits = 0; cbits <= 0x1FF; cbits++) {
            int flagC = (cbits & 0x100) == 0x100 ? FLAG_C : 0;
            int flagH = cbits & FLAG_H;
            int carryIns = ((cbits & 0xFF) >>> 7) ^ flagC;
            int flagP = (carryIns == 0) ? 0 : FLAG_PV;
            int flags = flagC | flagH | flagP;
            table.append("0x")
                .append(Integer.toHexString(flags))
                .append(", ");
        }
        table.append("};\n");
        System.out.println(table);
    }

    @Test
    public void generateTableSZ() {
        StringBuilder table = new StringBuilder("public final static int[] TABLE_SZ = new int[] {\n        ");

        int i = 0;
        for (int sum = 0; sum < 256; sum++) {
            int flagZ = (sum == 0 ? FLAG_Z : 0);
            int flags = flagZ | (sum & 0x80);
            table.append("0x")
                .append(Integer.toHexString(flags))
                .append(", ");
            if (i++ == 15) {
                table.append("\n        ");
                i = 0;
            }
        }
        table.append("};\n");
        System.out.println(table);
    }

    @Test
    public void generateTableHP() {
        StringBuilder table = new StringBuilder("public final static int[] TABLE_HP = new int[] {\n        ");

        int i = 0;
        for (int cbits = 0; cbits <= 0x1FF; cbits++) {
            int flagC = (cbits & 0x100) == 0x100 ? FLAG_C : 0;
            int flagH = cbits & FLAG_H;
            int carryIns = ((cbits & 0xFF) >>> 7) ^ flagC;
            int flagP = (carryIns == 0) ? 0 : FLAG_PV;
            int flags = flagH | flagP;
            table.append("0x")
                .append(Integer.toHexString(flags))
                .append(", ");
            if (i++ == 15) {
                table.append("\n        ");
                i = 0;
            }
        }
        table.append("};\n");
        System.out.println(table);
    }

}
