/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.tree;

import emulib.runtime.HEXFileManager;
import net.sf.emustudio.zilogZ80.assembler.exceptions.CompilerException;
import net.sf.emustudio.zilogZ80.assembler.exceptions.ValueTooBigException;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Instruction;

public class OC_ExprExpr extends Instruction {
    public static final int LD_IIX_NN = 0xDD360000; // LD (IX+N),N
    public static final int LD_IIY_NN = 0xFD360000; // LD (IY+N),N
    public static final int BIT_IIX_NN = 0xDDCB0046; // BIT b,(IIX+N)
    public static final int BIT_IIY_NN = 0xFDCB0046; // BIT b,(IIY+N)
    public static final int RES_IIX_NN = 0xDDCB0086; // RES b,(IIX+N)
    public static final int RES_IIY_NN = 0xFDCB0086; // RES b,(IIY+N)
    public static final int SET_IIX_NN = 0xDDCB00C6; // SET b,(IIX+N)
    public static final int SET_IIY_NN = 0xFDCB00C6; // SET b,(IIY+N)
    private Expression e1;
    private Expression e2;
    private boolean bitInstr;
    private int old_opcode;

    public OC_ExprExpr(int opcode, Expression e1, Expression e2, boolean bitInstr, int line, int column) {
        super(opcode, line, column);
        this.e1 = e1;
        this.e2 = e2;
        this.bitInstr = bitInstr;
        this.old_opcode = opcode;
    }

    @Override
    public void pass1() throws Exception {
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        e1.eval(parentEnv, addr_start);
        e2.eval(parentEnv, addr_start);

        int val1 = e1.getValue();
        int val2 = e2.getValue();
        if (Expression.getSize(val1) > 1) {
            throw new ValueTooBigException(line, column, val1, 0xFF);
        }
        if (Expression.getSize(val2) > 1) {
            throw new ValueTooBigException(line, column, val2, 0xFF);
        }
        opcode = old_opcode;
        if (bitInstr) {
            if ((val1 > 7) || (val1 < 0)) {
                throw new CompilerException(line, column, "Error: value(1) can be only in range 0-7");
            }
            opcode += (val2 << 8) + (8 * val1);
        } else {
            opcode += ((val1 << 8) + val2);
        }
        return (addr_start + getSize());
    }

    @Override
    public void generateCode(HEXFileManager hex) throws Exception {
        String s;
        if (getSize() == 1) {
            s = "%1$02X";
        } else if (getSize() == 2) {
            s = "%1$04X";
        } else if (getSize() == 3) {
            s = "%1$06X";
        } else {
            s = "%1$08X";
        }
        hex.putCode(String.format(s, opcode));
    }
}
