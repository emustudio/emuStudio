package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;

import java.util.Optional;

/**
 * The visitor creates internal AST (= "Program") of the parse tree.
 */
public class CreateProgramVisitor extends As8080ParserBaseVisitor<Program> {
    private final Program program = new Program();

    @Override
    public Program visitRLine(As8080Parser.RLineContext ctx) {
        Optional.ofNullable(ctx.label).ifPresent(program.env()::addLabel);
        program.addChild(AllVisitors.statement.visit(ctx.statement));
        return program;
    }
}
