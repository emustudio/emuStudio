package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import static net.emustudio.plugins.compiler.as8080.CommonParsers.parseLitString;

public class CreatePseudoVisitor extends As8080ParserBaseVisitor<Pseudo>  {

    @Override
    public Pseudo visitPseudoOrg(PseudoOrgContext ctx) {
        PseudoOrg pseudo = new PseudoOrg();
        pseudo.addChild(Visitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Pseudo visitPseudoEqu(PseudoEquContext ctx) {
        PseudoEqu pseudo = new PseudoEqu(ctx.id.getText());
        pseudo.addChild(Visitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Pseudo visitPseudoSet(PseudoSetContext ctx) {
        PseudoSet pseudo = new PseudoSet(ctx.id.getText());
        pseudo.addChild(Visitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Pseudo visitPseudoIf(PseudoIfContext ctx) {
        PseudoIf pseudo = new PseudoIf();
        pseudo.addChild(Visitors.expr.visit(ctx.expr));
        for (RLineContext line : ctx.rLine()) {
            pseudo.addChild(Visitors.line.visitRLine(line));
        }
        return pseudo;
    }

    @Override
    public Pseudo visitPseudoMacroDef(PseudoMacroDefContext ctx) {
        PseudoMacroDef pseudo = new PseudoMacroDef(ctx.id.getText());

        if (ctx.params != null) {
            for (TerminalNode next : ctx.params.ID_IDENTIFIER()) {
                pseudo.addChild(new ExprId(next.getSymbol().getText()));
            }
        }
        for (RLineContext line : ctx.rLine()) {
            pseudo.addChild(Visitors.line.visitRLine(line));
        }
        return pseudo;
    }

    @Override
    public Pseudo visitPseudoMacroCall(PseudoMacroCallContext ctx) {
        PseudoMacroCall pseudo = new PseudoMacroCall(ctx.id.getText());

        if (ctx.args != null) {
            for (RExpressionContext next : ctx.args.rExpression()) {
                pseudo.addChild(Visitors.expr.visit(next));
            }
        }
        return pseudo;
    }

    @Override
    public Pseudo visitPseudoInclude(PseudoIncludeContext ctx) {
        return new PseudoInclude(parseLitString(ctx.filename));
    }
}
