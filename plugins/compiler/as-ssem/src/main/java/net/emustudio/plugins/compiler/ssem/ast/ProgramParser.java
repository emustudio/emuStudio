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
