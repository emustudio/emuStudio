/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ram;

import net.emustudio.plugins.compiler.ram.ast.Instruction;
import net.emustudio.plugins.compiler.ram.ast.Label;
import net.emustudio.plugins.compiler.ram.ast.Program;
import net.emustudio.plugins.compiler.ram.ast.Value;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.compiler.ram.ParsingUtils.*;
import static net.emustudio.plugins.compiler.ram.RAMParser.*;

public class ProgramParser extends RAMParserBaseVisitor<Program> {
    private final static Map<Integer, RAMInstruction.Opcode> tokenOpcodes = new HashMap<>();

    static {
        tokenOpcodes.put(OPCODE_READ, RAMInstruction.Opcode.READ);
        tokenOpcodes.put(OPCODE_WRITE, RAMInstruction.Opcode.WRITE);
        tokenOpcodes.put(OPCODE_LOAD, RAMInstruction.Opcode.LOAD);
        tokenOpcodes.put(OPCODE_STORE, RAMInstruction.Opcode.STORE);
        tokenOpcodes.put(OPCODE_ADD, RAMInstruction.Opcode.ADD);
        tokenOpcodes.put(OPCODE_SUB, RAMInstruction.Opcode.SUB);
        tokenOpcodes.put(OPCODE_MUL, RAMInstruction.Opcode.MUL);
        tokenOpcodes.put(OPCODE_DIV, RAMInstruction.Opcode.DIV);
        tokenOpcodes.put(OPCODE_JMP, RAMInstruction.Opcode.JMP);
        tokenOpcodes.put(OPCODE_JZ, RAMInstruction.Opcode.JZ);
        tokenOpcodes.put(OPCODE_JGTZ, RAMInstruction.Opcode.JGTZ);
        tokenOpcodes.put(OPCODE_HALT, RAMInstruction.Opcode.HALT);
    }

    private final Program program;
    private int currentAddress;

    public ProgramParser(Program program) {
        this.program = Objects.requireNonNull(program);
    }

    public Program getProgram() {
        return program;
    }

    @Override
    public Program visitRLine(RAMParser.RLineContext ctx) {
        if (ctx.label != null) {
            Token token = ctx.label;
            Label label = new Label(token.getLine(), token.getCharPositionInLine(), parseLabel(token), currentAddress);
            program.add(label);
        }
        if (ctx.statement != null) {
            visitRStatement(ctx.statement);
        }
        return program;
    }

    @Override
    public Program visitRInstruction(RAMParser.RInstructionContext ctx) {
        Token op = ctx.op;
        RAMInstruction.Opcode opcode = tokenOpcodes.get(op.getType());
        RAMInstruction.Direction direction = RAMInstruction.Direction.DIRECT;
        if (ctx.d != null) {
            if (ctx.d.getType() == OP_CONSTANT) {
                direction = RAMInstruction.Direction.CONSTANT;
            } else if (ctx.d.getType() == OP_INDIRECT) {
                direction = RAMInstruction.Direction.INDIRECT;
            }
        }
        Value operand = null;
        if (ctx.id != null) {
            operand = new Value(ctx.id.getText(), true);
        } else if (ctx.v != null) {
            operand = parseValue(ctx.v.v);
        } else if (ctx.n != null) {
            operand = parseValue(ctx.n.n);
        }

        Instruction instruction = new Instruction(
            op.getLine(), op.getCharPositionInLine(), opcode, direction, currentAddress++, Optional.ofNullable(operand)
        );
        program.add(instruction);
        return program;
    }

    @Override
    public Program visitRInput(RAMParser.RInputContext ctx) {
        for (RValueContext v : ctx.rValue()) {
            if (v.v != null) {
                program.add(parseValue(v.v));
            }
        }
        return program;
    }

    public Value parseValue(Token value) {
        switch (value.getType()) {
            case LIT_BINNUMBER:
                return new Value(parseLitBin(value));
            case LIT_HEXNUMBER_1:
                return new Value(parseLitHex1(value));
            case LIT_HEXNUMBER_2:
                return new Value(parseLitHex2(value));
            case LIT_NUMBER:
                return new Value(parseLitDec(value));
            case LIT_OCTNUMBER:
                return new Value(parseLitOct(value));
            case LIT_STRING_1:
            case LIT_STRING_2:
                return new Value(parseLitString(value), false);
        }
        throw new IllegalStateException("unexpected value token type: " + value.getType());
    }
}
