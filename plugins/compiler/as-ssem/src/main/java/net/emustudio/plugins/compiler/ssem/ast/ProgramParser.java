package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.plugins.compiler.ssem.SSEMParser;
import net.emustudio.plugins.compiler.ssem.SSEMParserBaseVisitor;
import org.antlr.v4.runtime.Token;

public class ProgramParser extends SSEMParserBaseVisitor<Program> {
    private final Program program = new Program();

    @Override
    public Program visitLine(SSEMParser.LineContext ctx) {
        if (ctx.linenumber != null) {
            int line = parseNumber(ctx.linenumber);

            if (ctx.command != null) {
                int operand = 0;
                if (ctx.command.operand != null) {
                    if (ctx.command.instr.getType() == SSEMParser.BNUM) {
                        operand = Integer.parseInt(ctx.command.operand.getText(), 2);
                    } else {
                        operand = parseNumber(ctx.command.operand);
                    }
                }

                int instrType = ctx.command.instr.getType();
                if (instrType == SSEMParser.START) {
                    program.setStartLine(line);
                } else {
                    program.add(line, new Instruction(instrType, operand));
                }
            }
        }
        return program;
    }

    private int parseNumber(Token token) {
        if (token.getType() == SSEMParser.HEXNUMBER) {
            return Integer.decode(token.getText());
        } else {
            // Do not use decode because we don't support octal numbers
            return Integer.parseUnsignedInt(token.getText());
        }
    }

    public Program getProgram() {
        return program;
    }
}
