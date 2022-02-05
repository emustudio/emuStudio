package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.Program;

import java.util.Objects;

/**
 * The visitor creates internal AST (= "Program") of the parse tree.
 */
public class CreateProgramVisitor extends AsZ80ParserBaseVisitor<Program> {
    private final Program program;

    public CreateProgramVisitor(Program program) {
        this.program = Objects.requireNonNull(program);
    }

    @Override
    public Program visitRLine(RLineContext ctx) {
        Node statement = CreateVisitors.line.visitRLine(ctx);
        if (statement != null) {
            program.addChild(statement);
        }
        return program;
    }
}
