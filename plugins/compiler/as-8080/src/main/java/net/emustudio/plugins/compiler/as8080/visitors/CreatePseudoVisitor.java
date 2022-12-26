/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class CreatePseudoVisitor extends As8080ParserBaseVisitor<Node> {

    @Override
    public Node visitPseudoOrg(PseudoOrgContext ctx) {
        Token start = ctx.getStart();
        PseudoOrg pseudo = new PseudoOrg(start.getLine(), start.getCharPositionInLine());
        pseudo.addChild(CreateVisitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoEqu(PseudoEquContext ctx) {
        PseudoEqu pseudo = new PseudoEqu(ctx.id);
        pseudo.addChild(CreateVisitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoVar(PseudoVarContext ctx) {
        PseudoSet pseudo = new PseudoSet(ctx.id);
        pseudo.addChild(CreateVisitors.expr.visit(ctx.expr));
        return pseudo;
    }

    @Override
    public Node visitPseudoIf(PseudoIfContext ctx) {
        Token start = ctx.getStart();

        PseudoIf pseudo = new PseudoIf(start.getLine(), start.getCharPositionInLine());
        Node expr = CreateVisitors.expr.visit(ctx.expr);
        PseudoIfExpression ifExpr = new PseudoIfExpression(expr.line, expr.column);
        ifExpr.addChild(expr);
        pseudo.addChild(ifExpr);
        for (RLineContext line : ctx.rLine()) {
            Node rLine = CreateVisitors.line.visitRLine(line);
            if (rLine != null) {
                pseudo.addChild(rLine);
            }
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoMacroDef(PseudoMacroDefContext ctx) {
        PseudoMacroDef pseudo = new PseudoMacroDef(ctx.id);

        if (ctx.params != null) {
            for (TerminalNode next : ctx.params.ID_IDENTIFIER()) {
                Token symbol = next.getSymbol();
                PseudoMacroParameter parameter = new PseudoMacroParameter(symbol.getLine(), symbol.getCharPositionInLine());
                parameter.addChild(new ExprId(next.getSymbol()));
                pseudo.addChild(parameter);
            }
        }
        for (RLineContext line : ctx.rLine()) {
            Node rLine = CreateVisitors.line.visitRLine(line);
            if (rLine != null) {
                pseudo.addChild(rLine);
            }
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoMacroCall(PseudoMacroCallContext ctx) {
        PseudoMacroCall pseudo = new PseudoMacroCall(ctx.id);

        if (ctx.args != null) {
            for (RExpressionContext next : ctx.args.rExpression()) {
                Token start = next.getStart();
                PseudoMacroArgument argument = new PseudoMacroArgument(start.getLine(), start.getCharPositionInLine());
                pseudo.addChild(argument.addChild(CreateVisitors.expr.visit(next)));
            }
        }
        return pseudo;
    }

    @Override
    public Node visitPseudoInclude(PseudoIncludeContext ctx) {
        return new PseudoInclude(ctx.filename);
    }
}
