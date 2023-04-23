/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ParsingUtils;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.expr.*;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class CreateExprVisitor extends AsZ80ParserBaseVisitor<Node> {
    private final String sourceFileName;

    public CreateExprVisitor(String sourceFileName) {
        this.sourceFileName = Objects.requireNonNull(sourceFileName);
    }

    @Override
    public Node visitExprOct(ExprOctContext ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitOct);
    }

    @Override
    public Node visitExprHex1(ExprHex1Context ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitHex1);
    }

    @Override
    public Node visitExprHex2(ExprHex2Context ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitHex2);
    }

    @Override
    public Node visitExprDec(ExprDecContext ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitDec);
    }

    @Override
    public Node visitExprBin(ExprBinContext ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitBin);
    }

    @Override
    public Node visitExprId(ExprIdContext ctx) {
        return new ExprId(sourceFileName, ctx.id);
    }

    @Override
    public Node visitExprString(ExprStringContext ctx) {
        return new ExprString(sourceFileName, ctx.str);
    }

    @Override
    public Node visitExprUnary(ExprUnaryContext ctx) {
        ExprUnary unary = new ExprUnary(sourceFileName, ctx.unaryop);
        unary.addChild(visit(ctx.expr));
        return unary;
    }

    @Override
    public Node visitExprInfix(ExprInfixContext ctx) {
        ExprInfix infix = new ExprInfix(sourceFileName, ctx.op);
        infix.addChild(visit(ctx.expr1));
        infix.addChild(visit(ctx.expr2));
        return infix;
    }

    @Override
    public Node visitExprParens(ExprParensContext ctx) {
        return visit(ctx.expr);
    }

    @Override
    public Node visitExprCurrentAddress(ExprCurrentAddressContext ctx) {
        Token start = ctx.getStart();
        return new ExprCurrentAddress(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));
    }
}
