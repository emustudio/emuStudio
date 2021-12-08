package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.Node;

public class CreateLineVisitor extends As8080ParserBaseVisitor<Node> {

    @Override
    public Node visitRLine(As8080Parser.RLineContext ctx) {
        Node label = null;
        if (ctx.label != null) {
            label = new Label(ctx.label);
        }
        Node statement = null;
        if (ctx.statement != null) {
            statement = visit(ctx.statement);
        }
        if (label != null) {
            if (statement != null) {
                label.addChild(statement);
            }
            return label;
        }
        return statement;
    }

    @Override
    public Node visitRStatement(As8080Parser.RStatementContext ctx) {
        if (ctx.instr != null) {
            return Visitors.instr.visit(ctx.instr);
        } else if (ctx.data != null) {
            return Visitors.data.visit(ctx.data);
        } else if (ctx.pseudo != null) {
            return Visitors.pseudo.visit(ctx.pseudo);
        }
        throw new IllegalStateException("No statement defined!");
    }
}
