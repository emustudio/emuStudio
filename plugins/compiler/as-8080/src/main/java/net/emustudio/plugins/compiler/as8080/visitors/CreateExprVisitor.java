/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.emulib.plugins.compiler.antlr.ParsingUtils;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.expr.*;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class CreateExprVisitor extends As8080ParserBaseVisitor<Node> {
    private final String sourceFileName;

    public CreateExprVisitor(String sourceFileName) {
        this.sourceFileName = Objects.requireNonNull(sourceFileName);
    }


    @Override
    public Node visitExprOct(As8080Parser.ExprOctContext ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitOct);
    }

    @Override
    public Node visitExprHex1(As8080Parser.ExprHex1Context ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitHex1);
    }

    @Override
    public Node visitExprHex2(As8080Parser.ExprHex2Context ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitHex2);
    }

    @Override
    public Node visitExprDec(As8080Parser.ExprDecContext ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitDec);
    }

    @Override
    public Node visitExprBin(As8080Parser.ExprBinContext ctx) {
        return new ExprNumber(sourceFileName, ctx.num, ParsingUtils::parseLitBin);
    }

    @Override
    public Node visitExprId(As8080Parser.ExprIdContext ctx) {
        return new ExprId(sourceFileName, ctx.id);
    }

    @Override
    public Node visitExprString(As8080Parser.ExprStringContext ctx) {
        return new ExprString(sourceFileName, ctx.str);
    }

    @Override
    public Node visitExprUnary(As8080Parser.ExprUnaryContext ctx) {
        ExprUnary unary = new ExprUnary(sourceFileName, ctx.unaryop);
        unary.addChild(visit(ctx.expr));
        return unary;
    }

    @Override
    public Node visitExprInfix(As8080Parser.ExprInfixContext ctx) {
        ExprInfix infix = new ExprInfix(sourceFileName, ctx.op);
        infix.addChild(visit(ctx.expr1));
        infix.addChild(visit(ctx.expr2));
        return infix;
    }

    @Override
    public Node visitExprParens(As8080Parser.ExprParensContext ctx) {
        return visit(ctx.expr);
    }

    @Override
    public Node visitExprCurrentAddress(As8080Parser.ExprCurrentAddressContext ctx) {
        Token start = ctx.getStart();
        return new ExprCurrentAddress(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));
    }
}
