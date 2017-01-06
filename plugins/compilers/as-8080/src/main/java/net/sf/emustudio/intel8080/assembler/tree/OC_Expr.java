/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
import net.sf.emustudio.intel8080.assembler.exceptions.ValueTooBigException;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.treeAbstract.ExprNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.OpCodeNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OC_Expr extends OpCodeNode {
    private final static Set<String> INSTRUCTION_WITH_SIZE_3 = new HashSet<>();
    private final static Map<String, Integer> OPCODES_BASE_198 = new HashMap<>();
    private final static Map<String, Integer> OPCODES_BASE_34 = new HashMap<>();
    private final static Map<String, Integer> OPCODES_BASE_194 = new HashMap<>();
    private final static Map<String, Integer> OPCODES_BASE_196 = new HashMap<>();

    static {
        INSTRUCTION_WITH_SIZE_3.add("sta");
        INSTRUCTION_WITH_SIZE_3.add("lda");
        INSTRUCTION_WITH_SIZE_3.add("shld");
        INSTRUCTION_WITH_SIZE_3.add("lhld");
        INSTRUCTION_WITH_SIZE_3.add("jmp");
        INSTRUCTION_WITH_SIZE_3.add("jc");
        INSTRUCTION_WITH_SIZE_3.add("jnc");
        INSTRUCTION_WITH_SIZE_3.add("jz");
        INSTRUCTION_WITH_SIZE_3.add("jnz");
        INSTRUCTION_WITH_SIZE_3.add("jm");
        INSTRUCTION_WITH_SIZE_3.add("jp");
        INSTRUCTION_WITH_SIZE_3.add("jpe");
        INSTRUCTION_WITH_SIZE_3.add("jpo");
        INSTRUCTION_WITH_SIZE_3.add("call");
        INSTRUCTION_WITH_SIZE_3.add("cc");
        INSTRUCTION_WITH_SIZE_3.add("cnc");
        INSTRUCTION_WITH_SIZE_3.add("cz");
        INSTRUCTION_WITH_SIZE_3.add("cnz");
        INSTRUCTION_WITH_SIZE_3.add("cm");
        INSTRUCTION_WITH_SIZE_3.add("cp");
        INSTRUCTION_WITH_SIZE_3.add("cpe");
        INSTRUCTION_WITH_SIZE_3.add("cpo");
        INSTRUCTION_WITH_SIZE_3.add("cpe");
        INSTRUCTION_WITH_SIZE_3.add("rst");

        OPCODES_BASE_198.put("adi", 0);
        OPCODES_BASE_198.put("aci", 8);
        OPCODES_BASE_198.put("sui", 16);
        OPCODES_BASE_198.put("sbi", 24);
        OPCODES_BASE_198.put("ani", 32);
        OPCODES_BASE_198.put("xri", 40);
        OPCODES_BASE_198.put("ori", 48);
        OPCODES_BASE_198.put("cpi", 0x38);

        OPCODES_BASE_34.put("shld", 0);
        OPCODES_BASE_34.put("lhld", 8);
        OPCODES_BASE_34.put("sta", 16);
        OPCODES_BASE_34.put("lda", 24);

        OPCODES_BASE_194.put("jmp", 1);
        OPCODES_BASE_194.put("jnz", 0);
        OPCODES_BASE_194.put("jz", 8);
        OPCODES_BASE_194.put("jnc", 16);
        OPCODES_BASE_194.put("jc", 24);
        OPCODES_BASE_194.put("jpo", 32);
        OPCODES_BASE_194.put("jpe", 40);
        OPCODES_BASE_194.put("jp", 48);
        OPCODES_BASE_194.put("jm", 56);

        OPCODES_BASE_196.put("call", 9);
        OPCODES_BASE_196.put("cnz", 0);
        OPCODES_BASE_196.put("cz", 8);
        OPCODES_BASE_196.put("cnc", 16);
        OPCODES_BASE_196.put("cc", 24);
        OPCODES_BASE_196.put("cpo", 32);
        OPCODES_BASE_196.put("cpe", 40);
        OPCODES_BASE_196.put("cp", 48);
        OPCODES_BASE_196.put("cm", 56);
        OPCODES_BASE_196.put("cp", 48);

    }

    private final ExprNode expr;

    public OC_Expr(String mnemo, ExprNode expr, int line, int column) {
        super(mnemo, line, column);
        this.expr = expr;
    }

    @Override
    public int getSize() {
        if (INSTRUCTION_WITH_SIZE_3.contains(mnemo)) {
            return 3;
        }
        return 2;
    }

    @Override
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        return (addr_start + this.getSize());
    }

    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        short opCode = 0xC6; // opcode for adi: 11 (000adi) 110
        boolean oneDataByte = true; // how many data bytes
        boolean insertAfter = true; // if expression have to be written after opcode
        boolean found = false;
        String code;

        if (OPCODES_BASE_198.containsKey(mnemo)) {
            opCode |= OPCODES_BASE_198.get(mnemo);
            found = true;
        } else {
            opCode = 34;
            oneDataByte = false;
        }

        if (!found && OPCODES_BASE_34.containsKey(mnemo)) {
            opCode |= OPCODES_BASE_34.get(mnemo);
            found = true;
        } else if (!found) {
            opCode = 194;
        }

        if (!found && OPCODES_BASE_194.containsKey(mnemo)) {
            opCode |= OPCODES_BASE_194.get(mnemo);
            found = true;
        } else if (!found) {
            opCode = 196;
        }

        if (!found && OPCODES_BASE_196.containsKey(mnemo)) {
            opCode |= OPCODES_BASE_196.get(mnemo);
            found = true;
        } else if (!found) {
            opCode = 199;
        }

        if (!found && mnemo.equals("rst")) {
            int v = expr.getValue();
            if (v > 7) {
                throw new ValueTooBigException(line, column, v, 7);
            }
            opCode |= (expr.getValue() << 3);
            insertAfter = false;
            found = true;
        }
        if (!found && mnemo.equals("in")) {
            opCode = 219;
            oneDataByte = true;
            found = true;
        }
        if (!found && mnemo.equals("out")) {
            opCode = 211;
            oneDataByte = true;
        }

        code = String.format("%02X", opCode);
        if (insertAfter) {
            if (oneDataByte) {
                if (expr.getValue() > 0xFF) {
                    throw new ValueTooBigException(line, column, expr.getValue(), 0xFF);
                }
                code += expr.getEncValue(true);
            } else {
                code += expr.getEncValue(false);
            }
        }
        hex.putCode(code);
    }
}
