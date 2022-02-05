package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoLabel;
import net.emustudio.plugins.compiler.asZ80.ast.Node;

public class CreateLineVisitor extends AsZ80ParserBaseVisitor<Node> {

    @Override
    public Node visitRLine(RLineContext ctx) {
        Node label = null;
        if (ctx.label != null) {
            label = new PseudoLabel(ctx.label);
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
    public Node visitRStatement(RStatementContext ctx) {
        if (ctx.instr != null) {
            return CreateVisitors.instr.visit(ctx.instr);
        } else if (ctx.data != null) {
            return CreateVisitors.data.visit(ctx.data);
        } else if (ctx.pseudo != null) {
            return CreateVisitors.pseudo.visit(ctx.pseudo);
        }
        throw new IllegalStateException("No statement defined!");
    }
}
