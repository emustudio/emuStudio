/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Objects;

public class CreatePseudoVisitor extends As8080ParserBaseVisitor<Node> {
    private final String sourceFileName;

    public CreatePseudoVisitor(String sourceFileName) {
        this.sourceFileName = Objects.requireNonNull(sourceFileName);
    }

    @Override
    public Node visitPseudoOrg(PseudoOrgContext ctx) {
        Token start = ctx.getStart();
        PseudoOrg pseudo = new PseudoOrg(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));
        pseudo.addChild(exprVisitor().visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoEqu(PseudoEquContext ctx) {
        PseudoEqu pseudo = new PseudoEqu(sourceFileName, ctx.id);
        pseudo.addChild(exprVisitor().visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoVar(PseudoVarContext ctx) {
        PseudoSet pseudo = new PseudoSet(sourceFileName, ctx.id);
        pseudo.addChild(exprVisitor().visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoIf(PseudoIfContext ctx) {
        Token start = ctx.getStart();

        PseudoIf pseudo = new PseudoIf(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));
        Node expr = exprVisitor().visit(ctx.expr);
        PseudoIfExpression ifExpr = new PseudoIfExpression(expr.position);
        ifExpr.addChild(expr);
        pseudo.addChild(ifExpr);
        for (RLineContext line : ctx.rLine()) {
            Node rLine = lineVisitor().visitRLine(line);
            if (rLine != null) {
                pseudo.addChild(rLine);
            }
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoMacroDef(PseudoMacroDefContext ctx) {
        PseudoMacroDef pseudo = new PseudoMacroDef(sourceFileName, ctx.id);

        if (ctx.params != null) {
            for (TerminalNode next : ctx.params.ID_IDENTIFIER()) {
                Token symbol = next.getSymbol();
                PseudoMacroParameter parameter = new PseudoMacroParameter(new SourceCodePosition(symbol.getLine(), symbol.getCharPositionInLine(), sourceFileName));
                parameter.addChild(new ExprId(sourceFileName, next.getSymbol()));
                pseudo.addChild(parameter);
            }
        }
        for (RLineContext line : ctx.rLine()) {
            Node rLine = lineVisitor().visitRLine(line);
            if (rLine != null) {
                pseudo.addChild(rLine);
            }
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoMacroCall(PseudoMacroCallContext ctx) {
        PseudoMacroCall pseudo = new PseudoMacroCall(sourceFileName, ctx.id);

        if (ctx.args != null) {
            for (RExpressionContext next : ctx.args.rExpression()) {
                Token start = next.getStart();
                PseudoMacroArgument argument = new PseudoMacroArgument(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));
                pseudo.addChild(argument.addChild(exprVisitor().visit(next)));
            }
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoInclude(PseudoIncludeContext ctx) {
        return new PseudoInclude(sourceFileName, ctx.filename);
    }

    private CreateExprVisitor exprVisitor() {
        return CreateVisitors.expr(sourceFileName);
    };
    private CreateLineVisitor lineVisitor() {
        return CreateVisitors.line(sourceFileName);
    }
}
