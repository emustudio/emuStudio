/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.plugins.compiler.ssem.Position;
import net.emustudio.plugins.compiler.ssem.SSEMParser;
import net.emustudio.plugins.compiler.ssem.SSEMParserBaseVisitor;
import org.antlr.v4.runtime.Token;

import static net.emustudio.plugins.compiler.ssem.CompilerChecks.checkedParseNumber;

public class ProgramParser extends SSEMParserBaseVisitor<Program> {
    private final Program program = new Program();

    @Override
    public Program visitLine(SSEMParser.LineContext ctx) {
        if (ctx.linenumber != null) {
            int line = parsePositiveInteger(ctx.linenumber);

            if (ctx.command != null) {
                Token tokenInstr = ctx.command.instr;
                Token tokenOperand = ctx.command.operand;

                long operand = 0;
                if (tokenOperand != null) {
                    if (tokenInstr.getType() == SSEMParser.BNUM) {
                        operand = parseBinary(tokenOperand);
                    } else {
                        operand = parseNumber(tokenOperand);
                    }
                }

                int instrType = tokenInstr.getType();
                if (instrType == SSEMParser.START) {
                    program.setStartLine(line, Position.of(ctx.linenumber));
                } else {
                    program.add(
                            line,
                            new Instruction(instrType, operand, Position.of(tokenInstr), Position.of(tokenOperand)),
                            Position.of(ctx.linenumber)
                    );
                }
            }
        }
        return program;
    }

    public Program getProgram() {
        return program;
    }

    private long parseBinary(Token token) {
        return checkedParseNumber(token, t -> Long.parseLong(t.getText(), 2));
    }

    private long parseNumber(Token token) {
        return checkedParseNumber(token, t -> {
            if (t.getType() == SSEMParser.HEXNUMBER) {
                return Long.decode(t.getText());
            } else {
                // Do not use decode because we don't support octal numbers
                return Long.parseLong(t.getText());
            }
        });
    }

    private int parsePositiveInteger(Token token) {
        return checkedParseNumber(token, t -> {
            if (t.getType() == SSEMParser.HEXNUMBER) {
                return Integer.decode(t.getText());
            } else {
                // Do not use decode because we don't support octal numbers
                return Integer.parseUnsignedInt(t.getText());
            }
        });
    }
}
