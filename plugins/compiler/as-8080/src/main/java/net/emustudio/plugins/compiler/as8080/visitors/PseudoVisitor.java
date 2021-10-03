package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;

public class PseudoVisitor extends As8080ParserBaseVisitor<Pseudo>  {

    @Override
    public Pseudo visitPseudoOrg(As8080Parser.PseudoOrgContext ctx) {
        return new PseudoOrg(AllVisitors.expr.visit(ctx.expr));
    }

    @Override
    public Pseudo visitPseudoEqu(As8080Parser.PseudoEquContext ctx) {
        return new PseudoEqu(ctx.id, AllVisitors.expr.visit(ctx.expr));
    }

    @Override
    public Pseudo visitPseudoSet(As8080Parser.PseudoSetContext ctx) {
        return new PseudoSet(ctx.id, AllVisitors.expr.visit(ctx.expr));
    }

    @Override
    public Pseudo visitPseudoIf(As8080Parser.PseudoIfContext ctx) {
        return new PseudoIf(AllVisitors.expr.visit(ctx.expr), AllVisitors.statement.visit(ctx.statement));
    }

    @Override
    public Pseudo visitPseudoMacroDef(As8080Parser.PseudoMacroDefContext ctx) {
        return new PseudoMacroDef();
    }

    @Override
    public Pseudo visitPseudoMacroCall(As8080Parser.PseudoMacroCallContext ctx) {
        return new PseudoMacroCall();
    }

    @Override
    public Pseudo visitPseudoInclude(As8080Parser.PseudoIncludeContext ctx) {
        return new PseudoInclude();
    }
}
