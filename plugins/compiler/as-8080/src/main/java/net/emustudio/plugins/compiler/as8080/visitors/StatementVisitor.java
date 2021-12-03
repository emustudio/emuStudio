package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;

public class StatementVisitor extends As8080ParserBaseVisitor<Node>  {

    @Override
    public Node visitRStatement(As8080Parser.RStatementContext ctx) {
        if (ctx.instr != null) {
            return Visitors.instr.visit(ctx.instr);
        }
        if (ctx.data != null) {
            return Visitors.data.visit(ctx.data);
        }
        if (ctx.pseudo != null) {
            return Visitors.pseudo.visit(ctx.pseudo);
        }
        throw new IllegalStateException("No statement defined!");
    }
}
