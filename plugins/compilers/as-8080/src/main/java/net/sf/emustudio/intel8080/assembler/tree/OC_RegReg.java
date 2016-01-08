/*
 * Copyright (C) 2007-2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.intel8080.assembler.tree;

import emulib.runtime.HEXFileManager;
import net.sf.emustudio.intel8080.assembler.exceptions.CompilerException;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.treeAbstract.OpCodeNode;

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
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        if ((reg_src == reg_dst) && (reg_src == 6)) {
            throw new CompilerException(line, column, "Can't use M register on both src and dst");
        }
        return addr_start + 1;
    }

    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        int opCode = 64;
        opCode |= (reg_dst << 3);
        opCode |= reg_src;
        hex.putCode(String.format("%1$02X", opCode));
    }
}
