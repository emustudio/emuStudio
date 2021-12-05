package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Statement;

import java.util.Optional;

import static net.emustudio.plugins.compiler.as8080.CommonParsers.parseLabel;

public class LineVisitor extends As8080ParserBaseVisitor<Node> {

    @Override
    public Statement visitRLine(As8080Parser.RLineContext ctx) {
        Statement statement = new Statement();
        Optional.ofNullable(ctx.label).ifPresent(label -> statement.addChild(new Label(parseLabel(label))));
        if (ctx.statement != null) {
            statement.addChild(visit(ctx.statement));
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
