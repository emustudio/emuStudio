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
import net.emustudio.plugins.compiler.as8080.exceptions.CompilerException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.OpCodeNode;

// only for mov instruction
public class OC_RegReg extends OpCodeNode {
    private final byte reg_src;
    private final byte reg_dst;

    public OC_RegReg(String mnemo, byte reg_dst, byte reg_src, int line,
                     int column) {
        super(mnemo, line, column);
        this.reg_dst = reg_dst;
        this.reg_src = reg_src;
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        if ((reg_src == reg_dst) && (reg_src == 6)) {
            throw new CompilerException(line, column, "Can't use M register on both src and dst");
        }
        return addr_start + 1;
    }

    @Override
    public void pass4(IntelHEX hex) {
        int opCode = 64;
        opCode |= (reg_dst << 3);
        opCode |= reg_src;
        hex.putCode(String.format("%1$02X", opCode));
    }
}
