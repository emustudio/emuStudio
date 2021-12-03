package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.Statement;

import java.util.Optional;

public class LineVisitor extends As8080ParserBaseVisitor<Statement> {
    private Statement statement;

    @Override
    public Statement visitRLine(As8080Parser.RLineContext ctx) {
        statement = new Statement();
        Optional.ofNullable(ctx.label).ifPresent(label -> statement.addChild(new Label(label)));
        return visitChildren(ctx);
    }

    @Override
    public Statement visitRStatement(As8080Parser.RStatementContext ctx) {
        statement.addChild(Visitors.statement.visitRStatement(ctx));
        return statement;
    }
}
