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
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.plugins.compiler.rasp.ast.Instruction;
import net.emustudio.plugins.compiler.rasp.ast.Label;
import net.emustudio.plugins.compiler.rasp.ast.Program;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.compiler.rasp.ParsingUtils.*;
import static net.emustudio.plugins.compiler.rasp.RASPParser.*;


public class ProgramParser extends RASPParserBaseVisitor<Program> {
    private final static Map<Integer, Integer> registerOps = new HashMap<>();
    private final static Map<Integer, Integer> constantOps = new HashMap<>();
    private final static Map<Integer, Integer> jumpOps = new HashMap<>();

    static {
        registerOps.put(OPCODE_READ, 1);
        registerOps.put(OPCODE_WRITE, 3);
        registerOps.put(OPCODE_LOAD, 5);
        registerOps.put(OPCODE_STORE, 6);
        registerOps.put(OPCODE_ADD, 8);
        registerOps.put(OPCODE_SUB, 10);
        registerOps.put(OPCODE_MUL, 12);
        registerOps.put(OPCODE_DIV, 14);

        constantOps.put(OPCODE_WRITE, 2);
        constantOps.put(OPCODE_LOAD, 4);
        constantOps.put(OPCODE_ADD, 7);
        constantOps.put(OPCODE_SUB, 9);
        constantOps.put(OPCODE_MUL, 11);
        constantOps.put(OPCODE_DIV, 13);

        jumpOps.put(OPCODE_JMP, 15);
        jumpOps.put(OPCODE_JZ, 16);
        jumpOps.put(OPCODE_JGTZ, 17);
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
    public Program visitRLine(RASPParser.RLineContext ctx) {
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
    public Program visitROrg(ROrgContext ctx) {
        this.currentAddress = parseNumber(ctx.n.n);
        return program;
    }

    @Override
    public Program visitInstrRegister(InstrRegisterContext ctx) {
        Token op = ctx.op;
        int opcode = registerOps.get(op.getType());
        int operand = parseNumber(ctx.n.n);

        Instruction instruction = new Instruction(
            op.getLine(), op.getCharPositionInLine(), opcode, currentAddress++, Optional.of(operand)
        );
        currentAddress++;  // operand
        program.add(instruction);
        return program;
    }

    @Override
    public Program visitInstrConstant(InstrConstantContext ctx) {
        Token op = ctx.op;
        int opcode = constantOps.get(op.getType());
        int operand = parseNumber(ctx.n.n);

        Instruction instruction = new Instruction(
            op.getLine(), op.getCharPositionInLine(), opcode, currentAddress++, Optional.of(operand)
        );
        currentAddress++;  // operand
        program.add(instruction);
        return program;
    }

    @Override
    public Program visitInstrJump(InstrJumpContext ctx) {
        Token op = ctx.op;
        int opcode = jumpOps.get(op.getType());
        String id = ctx.id.getText();

        Instruction instruction = new Instruction(
            op.getLine(), op.getCharPositionInLine(), opcode, currentAddress++, id
        );
        currentAddress++;  // operand
        program.add(instruction);
        return program;
    }

    @Override
    public Program visitInstrNoOperand(InstrNoOperandContext ctx) {
        Instruction instruction = new Instruction(
            ctx.op.getLine(), ctx.op.getCharPositionInLine(), 18, currentAddress++, Optional.empty()
        );
        program.add(instruction);
        return program;
    }


    @Override
    public Program visitRInput(RASPParser.RInputContext ctx) {
        for (RNumberContext n : ctx.rNumber()) {
            if (n.n != null) {
                program.add(parseNumber(n.n));
            }
        }
        return program;
    }

    public int parseNumber(Token number) {
        switch (number.getType()) {
            case LIT_BINNUMBER:
                return parseLitBin(number);
            case LIT_HEXNUMBER_1:
                return parseLitHex1(number);
            case LIT_HEXNUMBER_2:
                return parseLitHex2(number);
            case LIT_NUMBER:
                return parseLitDec(number);
            case LIT_OCTNUMBER:
                return parseLitOct(number);
        }
        throw new IllegalStateException("unexpected number token type: " + number.getType());
    }
}
