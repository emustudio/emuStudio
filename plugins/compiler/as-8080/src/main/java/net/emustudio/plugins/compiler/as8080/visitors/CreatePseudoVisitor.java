package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class CreatePseudoVisitor extends As8080ParserBaseVisitor<Node>  {

    @Override
    public Node visitPseudoOrg(PseudoOrgContext ctx) {
        Token start = ctx.getStart();
        PseudoOrg pseudo = new PseudoOrg(start.getLine(), start.getCharPositionInLine());
        pseudo.addChild(Visitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoEqu(PseudoEquContext ctx) {
        PseudoEqu pseudo = new PseudoEqu(ctx.id);
        pseudo.addChild(Visitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoSet(PseudoSetContext ctx) {
        PseudoSet pseudo = new PseudoSet(ctx.id);
        pseudo.addChild(Visitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoIf(PseudoIfContext ctx) {
        Token start = ctx.getStart();

        PseudoIf pseudo = new PseudoIf(start.getLine(), start.getCharPositionInLine());
        pseudo.addChild(Visitors.expr.visit(ctx.expr));
        for (RLineContext line : ctx.rLine()) {
            pseudo.addChild(Visitors.line.visitRLine(line));
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoMacroDef(PseudoMacroDefContext ctx) {
        PseudoMacroDef pseudo = new PseudoMacroDef(ctx.id);

        if (ctx.params != null) {
            for (TerminalNode next : ctx.params.ID_IDENTIFIER()) {
                PseudoMacroParameter parameter = new PseudoMacroParameter();
                parameter.addChild(new ExprId(next.getSymbol()));
                pseudo.addChild(parameter);
            }
        }
        for (RLineContext line : ctx.rLine()) {
            pseudo.addChild(Visitors.line.visitRLine(line));
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoMacroCall(PseudoMacroCallContext ctx) {
        PseudoMacroCall pseudo = new PseudoMacroCall(ctx.id);

        if (ctx.args != null) {
            for (RExpressionContext next : ctx.args.rExpression()) {
                PseudoMacroArgument argument = new PseudoMacroArgument();
                pseudo.addChild(argument.addChild(Visitors.expr.visit(next)));
            }
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoInclude(PseudoIncludeContext ctx) {
        return new PseudoInclude(ctx.filename);
    }
}