/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
