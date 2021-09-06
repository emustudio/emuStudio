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

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.as8080.Namespace;
import net.emustudio.plugins.compiler.as8080.exceptions.ValueTooBigException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.OpCodeNode;

import java.util.HashMap;
import java.util.Map;

public class OC_Expr extends OpCodeNode {
    private final static Map<String, Integer> OPCODES_BASE_198 = new HashMap<>();
    private final static Map<String, Integer> OPCODES_BASE_34 = new HashMap<>();
    private final static Map<String, Integer> OPCODES_BASE_194 = new HashMap<>();
    private final static Map<String, Integer> OPCODES_BASE_196 = new HashMap<>();

    private final static  Map<String, Integer> INSTRUCTION_SIZE = new HashMap<>();

    static {
        INSTRUCTION_SIZE.put("sta", 3);
        INSTRUCTION_SIZE.put("lda", 3);
        INSTRUCTION_SIZE.put("shld", 3);
        INSTRUCTION_SIZE.put("lhld", 3);
        INSTRUCTION_SIZE.put("jmp", 3);
        INSTRUCTION_SIZE.put("jc", 3);
        INSTRUCTION_SIZE.put("jnc", 3);
        INSTRUCTION_SIZE.put("jz", 3);
        INSTRUCTION_SIZE.put("jnz", 3);
        INSTRUCTION_SIZE.put("jm", 3);
        INSTRUCTION_SIZE.put("jp", 3);
        INSTRUCTION_SIZE.put("jpe", 3);
        INSTRUCTION_SIZE.put("jpo", 3);
        INSTRUCTION_SIZE.put("call", 3);
        INSTRUCTION_SIZE.put("cc", 3);
        INSTRUCTION_SIZE.put("cnc", 3);
        INSTRUCTION_SIZE.put("cz", 3);
        INSTRUCTION_SIZE.put("cnz", 3);
        INSTRUCTION_SIZE.put("cm", 3);
        INSTRUCTION_SIZE.put("cp", 3);
        INSTRUCTION_SIZE.put("cpe", 3);
        INSTRUCTION_SIZE.put("cpo", 3);
        INSTRUCTION_SIZE.put("rst", 1);
        INSTRUCTION_SIZE.put("adi", 2);
        INSTRUCTION_SIZE.put("aci", 2);
        INSTRUCTION_SIZE.put("sui", 2);
        INSTRUCTION_SIZE.put("sbi", 2);
        INSTRUCTION_SIZE.put("ani", 2);
        INSTRUCTION_SIZE.put("xri", 2);
        INSTRUCTION_SIZE.put("ori", 2);
        INSTRUCTION_SIZE.put("cpi", 2);
        INSTRUCTION_SIZE.put("in", 2);
        INSTRUCTION_SIZE.put("out", 2);

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

    }

    private final ExprNode expr;

    public OC_Expr(String mnemo, ExprNode expr, int line, int column) {
        super(mnemo, line, column);
        this.expr = expr;
    }

    @Override
    public int getSize() {
        return INSTRUCTION_SIZE.get(mnemo);
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        return (addr_start + this.getSize());
    }

    @Override
    public void pass4(IntelHEX hex) throws Exception {
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
        hex.add(code);
    }

    @Override
    public String toString() {
        return "OC_Expr{" +
            "expr=" + expr +
            '}';
    }
}
