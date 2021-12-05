package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.Statement;

import java.util.Objects;

/**
 * The visitor creates internal AST (= "Program") of the parse tree.
 */
public class CreateProgramVisitor extends As8080ParserBaseVisitor<Program> {
    private final Program program;

    public CreateProgramVisitor(Program program) {
        this.program = Objects.requireNonNull(program);
    }

    @Override
    public Program visitRLine(As8080Parser.RLineContext ctx) {
        Statement statement = Visitors.line.visitRLine(ctx);
        if (statement != null) {
            program.addChild(statement);
        }
        return program;
    }
}
