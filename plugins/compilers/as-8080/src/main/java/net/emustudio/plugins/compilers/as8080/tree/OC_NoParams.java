/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compilers.as8080.tree;

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compilers.as8080.Namespace;
import net.emustudio.plugins.compilers.as8080.treeAbstract.OpCodeNode;

public class OC_NoParams extends OpCodeNode {

    public OC_NoParams(String mnemo, int line, int column) {
        super(mnemo, line, column);
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        return addr_start + 1;
    }

    @Override
    public void pass4(IntelHEX hex) {
        short opCode = 0; // nop

        switch (mnemo) {
            case "stc":
                opCode = 55;
                break;
            case "cmc":
                opCode = 63;
                break;
            case "cma":
                opCode = 47;
                break;
            case "daa":
                opCode = 39;
                break;
            case "nop":
                break;
            case "rlc":
                opCode = 7;
                break;
            case "rrc":
                opCode = 15;
                break;
            case "ral":
                opCode = 23;
                break;
            case "rar":
                opCode = 31;
                break;
            case "xchg":
                opCode = 235;
                break;
            case "xthl":
                opCode = 227;
                break;
            case "sphl":
                opCode = 249;
                break;
            case "pchl":
                opCode = 233;
                break;
            case "ret":
                opCode = 201;
                break;
            case "rc":
                opCode = 216;
                break;
            case "rnc":
                opCode = 208;
                break;
            case "rz":
                opCode = 200;
                break;
            case "rnz":
                opCode = 192;
                break;
            case "rm":
                opCode = 248;
                break;
            case "rp":
                opCode = 240;
                break;
            case "rpe":
                opCode = 232;
                break;
            case "rpo":
                opCode = 224;
                break;
            case "ei":
                opCode = 251;
                break;
            case "di":
                opCode = 243;
                break;
            case "hlt":
                opCode = 118;
                break;
        }
        hex.putCode(String.format("%1$02X", opCode));
    }
}
