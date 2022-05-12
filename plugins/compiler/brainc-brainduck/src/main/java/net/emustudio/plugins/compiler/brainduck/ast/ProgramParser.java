package net.emustudio.plugins.compiler.brainduck.ast;

import net.emustudio.plugins.compiler.brainduck.BraincParser;
import net.emustudio.plugins.compiler.brainduck.BraincParserBaseVisitor;

public class ProgramParser extends BraincParserBaseVisitor<Program> {
    private final Program program = new Program();

    public Program getProgram() {
        return program;
    }

    @Override
    public Program visitStatement(BraincParser.StatementContext ctx) {
        if (ctx.instr != null) {
            Instruction instruction = new Instruction(ctx.instr.getType());
            program.add(instruction);
        }
        return program;
    }
}
