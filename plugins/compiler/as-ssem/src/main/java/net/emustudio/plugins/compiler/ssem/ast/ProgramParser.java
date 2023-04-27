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

import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.compiler.ssem.CompilerChecks.checkedParseNumber;

public class ProgramParser extends SSEMParserBaseVisitor<Program> {
    private final Program program = new Program();
    private final String fileName;

    public ProgramParser(String fileName) {
        this.fileName = Objects.requireNonNull(fileName);
    }


    @Override
    public Program visitLine(SSEMParser.LineContext ctx) {
        if (ctx.linenumber != null) {
            int line = parsePositiveInteger(ctx.linenumber);

            if (ctx.command != null) {
                Token tokenInstr = ctx.command.instr;
                Optional<Token> tokenOperand = Optional.ofNullable(ctx.command.operand);

                long operand = 0;
                if (tokenOperand.isPresent()) {
                    if (tokenInstr.getType() == SSEMParser.BNUM) {
                        operand = parseBinary(tokenOperand.get());
                    } else {
                        operand = parseNumber(tokenOperand.get());
                    }
                }

                int instrType = tokenInstr.getType();
                if (instrType == SSEMParser.START) {
                    program.setStartLine(line, Position.of(fileName, ctx.linenumber));
                } else {
                    program.add(
                            line,
                            new Instruction(instrType, operand, Position.of(fileName, tokenInstr), tokenOperand.map(o -> Position.of(fileName, o))),
                            Position.of(fileName, ctx.linenumber)
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
        return checkedParseNumber(fileName, token, t -> Long.parseLong(t.getText(), 2));
    }

    private long parseNumber(Token token) {
        return checkedParseNumber(fileName, token, t -> {
            if (t.getType() == SSEMParser.HEXNUMBER) {
                return Long.decode(t.getText());
            } else {
                // Do not use decode because we don't support octal numbers
                return Long.parseLong(t.getText());
            }
        });
    }

    private int parsePositiveInteger(Token token) {
        return checkedParseNumber(fileName, token, t -> {
            if (t.getType() == SSEMParser.HEXNUMBER) {
                return Integer.decode(t.getText());
            } else {
                // Do not use decode because we don't support octal numbers
                return Integer.parseUnsignedInt(t.getText());
            }
        });
    }
}
