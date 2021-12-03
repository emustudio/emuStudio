package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Statement;
import net.emustudio.plugins.compiler.as8080.ast.expr.Expr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

public class PseudoVisitor extends As8080ParserBaseVisitor<Pseudo>  {

    @Override
    public Pseudo visitPseudoOrg(PseudoOrgContext ctx) {
        return new PseudoOrg(Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Pseudo visitPseudoEqu(PseudoEquContext ctx) {
        return new PseudoEqu(ctx.id, Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Pseudo visitPseudoSet(PseudoSetContext ctx) {
        return new PseudoSet(ctx.id, Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Pseudo visitPseudoIf(PseudoIfContext ctx) {
        return new PseudoIf(Visitors.expr.visit(ctx.expr), Visitors.statement.visit(ctx.statement));
    }

    @Override
    public Pseudo visitPseudoMacroDef(PseudoMacroDefContext ctx) {
        List<Token> params = new ArrayList<>();
        if (ctx.params != null) {
            params.add(ctx.params.id);
            for (TerminalNode next : ctx.params.ID_IDENTIFIER()) {
                params.add(next.getSymbol());
            }
        }

        List<Statement> statements = new ArrayList<>();
        for (RLineContext line : ctx.rLine()) {
            statements.add(AllVisitors.line.visitRLine(line));
        }

        return new PseudoMacroDef(ctx.id, params, statements);
    }

    @Override
    public Pseudo visitPseudoMacroCall(PseudoMacroCallContext ctx) {
        List<Expr> arguments = new ArrayList<>();
        if (ctx.args != null) {
            arguments.add(AllVisitors.expr.visit(ctx.args.expr));
            for (RExpressionContext next : ctx.args.rExpression()) {
                arguments.add(AllVisitors.expr.visit(next));
            }
        }
        return new PseudoMacroCall(ctx.id, arguments);
    }

    @Override
    public Pseudo visitPseudoInclude(PseudoIncludeContext ctx) {
        return new PseudoInclude(ctx.filename);
    }
}
