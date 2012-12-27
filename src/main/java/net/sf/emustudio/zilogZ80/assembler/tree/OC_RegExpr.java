/*
 * OC_RegExpr.java
 *
 * Created on Pondelok, 2008, august 18, 8:55
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.tree;

import emulib.plugins.compiler.HEXFileHandler;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Instruction;

/**
 *
 * opcode = (first_byte+reg) expr
 *
 * @author Peter Jakubčo
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
    private Expression expr;
    private boolean oneByte;
    private boolean bitInstr; // bit instruction? (BIT,SET,RES)
    private int old_opcode;

    /**
     * *
     * Creates a new instance of OC_RegExpr
     *
     * @param pos index of byte where add register value; e.g. DD 70+reg XX XX
     * => pos = 1; C4+reg 00 00 => pos = 0;
     */
    public OC_RegExpr(int opcode, int reg, int pos, Expression expr,
            boolean oneByte, int line, int column) {
        super(opcode, line, column);
        this.opcode += (reg << ((getSize() - 1 - pos) * 8));
        old_opcode = opcode; //this.opcode;
        this.oneByte = oneByte;
        this.expr = expr;
        bitInstr = false;
    }

    /**
     * Special constructor for BIT,RES and SET instructions
     */
    public OC_RegExpr(int opcode, Expression bit, int reg,
            int line, int column) {
        super(opcode, line, column);
        oneByte = true;
        this.expr = bit;
        this.opcode += reg;
        old_opcode = opcode; //this.opcode;
        bitInstr = true;
    }

    @Override
    public void pass1() {
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        int val = expr.getValue();
        if (oneByte && (Expression.getSize(val) > 1)) {
            throw new Exception("[" + line + "," + column + "] Error:"
                    + " value too large");
        }
        //   opcode = old_opcode;
        if (old_opcode == JR) {
            val = (val - 2) & 0xff;
        }
        if (bitInstr) {
            if ((val > 7) || (val < 0)) {
                throw new Exception("[" + line + "," + column + "] Error:"
                        + " value can be only in range 0-7");
            }
            opcode += (8 * val);
        } else {
            if (oneByte) {
                opcode += Expression.reverseBytes(val, 1);
            } else {
                opcode += Expression.reverseBytes(val, 2);
            }
        }
        return addr_start + getSize();
    }

    // this can be only mvi instr
    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
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
