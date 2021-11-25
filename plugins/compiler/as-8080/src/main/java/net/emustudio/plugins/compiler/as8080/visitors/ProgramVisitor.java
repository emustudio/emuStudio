package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.visitors.InstrVisitor;

import java.util.Optional;

public class ProgramVisitor extends As8080ParserBaseVisitor<Program> {
    private final Program program = new Program();

    @Override
    public Program visitRLine(As8080Parser.RLineContext ctx) {
        Optional.ofNullable(ctx.label).ifPresent(program.env()::addLabel);
        return super.visitRLine(ctx);
    }

    @Override
    public Program visitRStatement(As8080Parser.RStatementContext ctx) {
        Optional.ofNullable(ctx.instr).ifPresent(i -> program.addIntruction(new InstrVisitor().visit(i)));
        //Optional.ofNullable(ctx.pseudo).ifPresent(p -> );
       // Optional.ofNullable(ctx.data).ifPresent(d -> );

        return program;
    }




}
