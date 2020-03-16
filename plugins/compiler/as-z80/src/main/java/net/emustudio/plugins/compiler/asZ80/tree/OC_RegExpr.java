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
package net.emustudio.plugins.compiler.asZ80.tree;

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compiler.asZ80.Namespace;
import net.emustudio.plugins.compiler.asZ80.exceptions.ValueOutOfBoundsException;
import net.emustudio.plugins.compiler.asZ80.exceptions.ValueTooBigException;
import net.emustudio.plugins.compiler.asZ80.treeAbstract.Expression;
import net.emustudio.plugins.compiler.asZ80.treeAbstract.Instruction;

/**
 * opcode = (first_byte+reg) expr
 */
public class OC_RegExpr extends Instruction {
    public static final int CALL = 0xC40000; // CALL cc,NN
    public static final int JP = 0xC20000; // JP cc,NN
    public static final int JR = 0x2000; // JR cc,N
    public static final int LD_IIX_NN = 0xDD7000; // LD (IX+N),r
    public static final int LD_IIY_NN = 0xFD7000; // LD (IY+N),r
    public static final int LD_RR = 0x010000; // LD rr,NN
    public static final int BIT = 0xCB40; // BIT b,r
    public static final int RES = 0xCB80; // RES b,r
    public static final int SET = 0xCBC0; // SET b,r

    private final Expression expr;
    private final boolean oneByte;
    private final boolean bitInstr; // bit instruction? (BIT,SET,RES)
    private final boolean relativeAddress;

    /**
     * Creates a new instance of OC_RegExpr
     *
     * @param pos index of byte where add register value; e.g. DD 70+reg XX XX
     *            => pos = 1; C4+reg 00 00 => pos = 0;
     */

    public OC_RegExpr(int opcode, int reg, int pos, Expression expr, boolean oneByte, int line, int column) {
        super(opcode, line, column);
        relativeAddress = opcode == JR;
        this.opcode += (reg << ((getSize() - 1 - pos) * 8));
        this.oneByte = oneByte;
        this.expr = expr;
        this.bitInstr = false;
    }

    /**
     * Special constructor for BIT,RES and SET instructions
     */
    public OC_RegExpr(int opcode, Expression bit, int reg, int line, int column) {
        super(opcode, line, column);
        relativeAddress = false;
        oneByte = true;
        this.expr = bit;
        this.opcode += reg;
        bitInstr = true;
    }

    @Override
    public void pass1() {
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        int val = expr.getValue();
        if (bitInstr) {
            if ((val > 7) || (val < 0)) {
                throw new ValueOutOfBoundsException(line, column, 0, 7, val);
            }
            opcode += (8 * val);
        } else {
            if (oneByte) {
                if (relativeAddress) {
                    val = computeRelativeAddress(line, column, addr_start, val);
                } else if (Expression.getSize(val) > 1) {
                    throw new ValueTooBigException(line, column, val, 0xFF);
                }
                opcode += Expression.reverseBytes(val, 1);
            } else {
                opcode += Expression.reverseBytes(val, 2);
            }
        }
        return addr_start + getSize();
    }

    // this can be only mvi instr
    @Override
    public void generateCode(IntelHEX hex) {
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
