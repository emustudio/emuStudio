/*
 * OC_Expr.java
 *
 * Created on Sobota, 2007, september 29, 20:54
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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

import net.sf.emustudio.intel8080.assembler.treeAbstract.ExprNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.OpCodeNode;
import emulib.plugins.compiler.HEXFileHandler;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;

/**
 *
 * @author vbmacher
 */
public class OC_Expr extends OpCodeNode {

    private ExprNode expr;

    /** Creates a new instance of OC_Expr */
    public OC_Expr(String mnemo, ExprNode expr, int line, int column) {
        super(mnemo, line, column);
        this.mnemo = mnemo;
        this.expr = expr;
    }

    /// compile time ///
    @Override
    public int getSize() {
        int rval;
        if (mnemo.equals("sta")) {
            rval = 3;
        } else if (mnemo.equals("lda")) {
            rval = 3;
        } else if (mnemo.equals("shld")) {
            rval = 3;
        } else if (mnemo.equals("lhld")) {
            rval = 3;
        } else if (mnemo.equals("jmp")) {
            rval = 3;
        } else if (mnemo.equals("jc")) {
            rval = 3;
        } else if (mnemo.equals("jnc")) {
            rval = 3;
        } else if (mnemo.equals("jz")) {
            rval = 3;
        } else if (mnemo.equals("jnz")) {
            rval = 3;
        } else if (mnemo.equals("jm")) {
            rval = 3;
        } else if (mnemo.equals("jp")) {
            rval = 3;
        } else if (mnemo.equals("jpe")) {
            rval = 3;
        } else if (mnemo.equals("jpo")) {
            rval = 3;
        } else if (mnemo.equals("call")) {
            rval = 3;
        } else if (mnemo.equals("cc")) {
            rval = 3;
        } else if (mnemo.equals("cnc")) {
            rval = 3;
        } else if (mnemo.equals("cz")) {
            rval = 3;
        } else if (mnemo.equals("cnz")) {
            rval = 3;
        } else if (mnemo.equals("cm")) {
            rval = 3;
        } else if (mnemo.equals("cp")) {
            rval = 3;
        } else if (mnemo.equals("cpe")) {
            rval = 3;
        } else if (mnemo.equals("cpo")) {
            rval = 3;
        } else if (mnemo.equals("cpe")) {
            rval = 3;
        } else if (mnemo.equals("rst")) {
            rval = 1;
        } else {
            rval = 2;
        }
        return rval;
    }

    @Override
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        return (addr_start + this.getSize());
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        short opCode = 198; // opcode for adi: 11 (000adi) 110
        boolean oneDataByte = true; // how many data bytes
        boolean insertAfter = true; // if expression have to be written after opcode
        boolean found = false;
        String code;

        if (mnemo.equals("adi")) {
            found = true;
        } else if (mnemo.equals("aci")) {
            opCode |= 8;
            found = true;
        } else if (mnemo.equals("sui")) {
            opCode |= 16;
            found = true;
        } else if (mnemo.equals("sbi")) {
            opCode |= 24;
            found = true;
        } else if (mnemo.equals("ani")) {
            opCode |= 32;
            found = true;
        } else if (mnemo.equals("xri")) {
            opCode |= 40;
            found = true;
        } else if (mnemo.equals("ori")) {
            opCode |= 48;
            found = true;
        } else if (mnemo.equals("cpi")) {
            opCode |= 56;
            found = true;
        } else {
            opCode = 34;
            oneDataByte = false;
        }

        if (found == false) {
            if (mnemo.equals("shld")) {
                found = true;
            } else if (mnemo.equals("lhld")) {
                opCode |= 8;
                found = true;
            } else if (mnemo.equals("sta")) {
                opCode |= 16;
                found = true;
            } else if (mnemo.equals("lda")) {
                opCode |= 24;
                found = true;
            } else {
                opCode = 194;
            }
        }

        if (found == false) {
            if (mnemo.equals("jmp")) {
                opCode |= 1;
                found = true;
            } else if (mnemo.equals("jnz")) {
                found = true;
            } else if (mnemo.equals("jz")) {
                opCode |= 8;
                found = true;
            } else if (mnemo.equals("jnc")) {
                opCode |= 16;
                found = true;
            } else if (mnemo.equals("jc")) {
                opCode |= 24;
                found = true;
            } else if (mnemo.equals("jpo")) {
                opCode |= 32;
                found = true;
            } else if (mnemo.equals("jpe")) {
                opCode |= 40;
                found = true;
            } else if (mnemo.equals("jp")) {
                opCode |= 48;
                found = true;
            } else if (mnemo.equals("jm")) {
                opCode |= 56;
                found = true;
            } else {
                opCode = 196;
            }
        }

        if (found == false) {
            if (mnemo.equals("call")) {
                opCode |= 9;
                found = true;
            } else if (mnemo.equals("cnz")) {
                found = true;
            } else if (mnemo.equals("cz")) {
                opCode |= 8;
                found = true;
            } else if (mnemo.equals("cnc")) {
                opCode |= 16;
                found = true;
            } else if (mnemo.equals("cc")) {
                opCode |= 24;
                found = true;
            } else if (mnemo.equals("cpo")) {
                opCode |= 32;
                found = true;
            } else if (mnemo.equals("cpe")) {
                opCode |= 40;
                found = true;
            } else if (mnemo.equals("cp")) {
                opCode |= 48;
                found = true;
            } else if (mnemo.equals("cm")) {
                opCode |= 56;
                found = true;
            } else {
                opCode = 199;
            }
        }

        if ((found == false) && mnemo.equals("rst")) {
            int v = expr.getValue();
            if (v > 7) {
                throw new Exception("[" + line + "," + column + "] value too large (maximum is 7)");
            }
            opCode |= (expr.getValue() << 3);
            insertAfter = false;
            found = true;
        }
        if ((found == false) && mnemo.equals("in")) {
            opCode = 219;
            oneDataByte = true;
            found = true;
        }
        if ((found == false) && mnemo.equals("out")) {
            opCode = 211;
            oneDataByte = true;
            found = true;
        }

        code = String.format("%02X", opCode);
        if (insertAfter) {
            if (oneDataByte) {
                if (expr.getValue() > 0xFF) {
                    throw new Exception("[" + line + "," + column + "] value too large (maximum is 0FFh)");
                }
                code += expr.getEncValue(true);
            } else {
                code += expr.getEncValue(false);
            }
        }
        hex.putCode(code);
    }
}
