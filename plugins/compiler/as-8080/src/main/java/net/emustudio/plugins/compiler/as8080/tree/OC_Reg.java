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
package net.emustudio.plugins.compiler.as8080.tree;

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compiler.as8080.Namespace;
import net.emustudio.plugins.compiler.as8080.treeAbstract.OpCodeNode;

public class OC_Reg extends OpCodeNode {
    private final byte reg;

    public OC_Reg(String mnemo, byte reg, int line, int column) {
        super(mnemo, line, column);
        this.reg = reg;
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
        int opCode = 0;

        switch (mnemo) {
            case "inr":
                opCode = 4 | (reg << 3);
                break;
            case "dcr":
                opCode = 5 | (reg << 3);
                break;
            case "add":
                opCode = 128 | reg;
                break;
            case "adc":
                opCode = 136 | reg;
                break;
            case "sub":
                opCode = 144 | reg;
                break;
            case "sbb":
                opCode = 152 | reg;
                break;
            case "ana":
                opCode = 160 | reg;
                break;
            case "xra":
                opCode = 168 | reg;
                break;
            case "ora":
                opCode = 176 | reg;
                break;
            case "cmp":
                opCode = 184 | reg;
                break;
        }

        hex.putCode(String.format("%1$02X", opCode));
    }

    @Override
    public String toString() {
        return "OC_Reg{" +
            "reg=" + reg +
            '}';
    }
}
