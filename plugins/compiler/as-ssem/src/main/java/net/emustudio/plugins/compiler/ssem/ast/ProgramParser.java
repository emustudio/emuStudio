package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.plugins.compiler.ssem.CompileException;
import net.emustudio.plugins.compiler.ssem.Position;
import net.emustudio.plugins.compiler.ssem.SSEMParser;
import net.emustudio.plugins.compiler.ssem.SSEMParserBaseVisitor;
import org.antlr.v4.runtime.Token;

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

    private long parseBinary(Token token) {
        try {
            return Long.parseLong(token.getText(), 2);
        } catch (NumberFormatException e) {
            throw new CompileException(
                token.getLine(), token.getCharPositionInLine(), "Could not parse number: " + token.getText()
            );
        }
    }

    private long parseNumber(Token token) {
        try {
            if (token.getType() == SSEMParser.HEXNUMBER) {
                return Long.decode(token.getText());
            } else {
                // Do not use decode because we don't support octal numbers
                return Long.parseLong(token.getText());
            }
        } catch (NumberFormatException e) {
            throw new CompileException(
                token.getLine(), token.getCharPositionInLine(), "Could not parse number: " + token.getText()
            );
        }
    }

    private int parsePositiveInteger(Token token) {
        try {
            if (token.getType() == SSEMParser.HEXNUMBER) {
                return Integer.decode(token.getText());
            } else {
                // Do not use decode because we don't support octal numbers
                return Integer.parseUnsignedInt(token.getText());
            }
        } catch (NumberFormatException e) {
            throw new CompileException(
                token.getLine(), token.getCharPositionInLine(), "Could not parse number: " + token.getText()
            );
        }
    }

    public Program getProgram() {
        return program;
    }
}
