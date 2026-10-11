/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.compiler.antlr.ParsingUtils;
import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
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
        Token token = ctx.unaryop;
        ExprUnary unary = new ExprUnary(
                new SourceCodePosition(token.getLine(), token.getCharPositionInLine(), sourceFileName),
                unaryOperator(token.getType())
        );
        unary.addChild(visit(ctx.expr));
        return unary;
    }

    @Override
    public Node visitExprInfix(As8080Parser.ExprInfixContext ctx) {
        Token token = ctx.op;
        ExprInfix infix = new ExprInfix(
                new SourceCodePosition(token.getLine(), token.getCharPositionInLine(), sourceFileName),
                infixOperator(token.getType())
        );
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

    private UnaryOperator unaryOperator(int tokenType) {
        switch (tokenType) {
            case As8080Parser.OP_ADD:
                return UnaryOperator.ADD;
            case As8080Parser.OP_SUBTRACT:
                return UnaryOperator.SUBTRACT;
            case As8080Parser.OP_NOT:
            case As8080Parser.OP_NOT_2:
                return UnaryOperator.NOT;
            default:
                throw new IllegalArgumentException("Unknown unary operation: " + tokenType);
        }
    }

    private InfixOperator infixOperator(int tokenType) {
        switch (tokenType) {
            case As8080Parser.OP_ADD:
                return InfixOperator.ADD;
            case As8080Parser.OP_SUBTRACT:
                return InfixOperator.SUBTRACT;
            case As8080Parser.OP_DIVIDE:
                return InfixOperator.DIVIDE;
            case As8080Parser.OP_MULTIPLY:
                return InfixOperator.MULTIPLY;
            case As8080Parser.OP_MOD:
            case As8080Parser.OP_MOD_2:
                return InfixOperator.MOD;
            case As8080Parser.OP_AND:
            case As8080Parser.OP_AND_2:
                return InfixOperator.AND;
            case As8080Parser.OP_OR:
            case As8080Parser.OP_OR_2:
                return InfixOperator.OR;
            case As8080Parser.OP_XOR:
            case As8080Parser.OP_XOR_2:
                return InfixOperator.XOR;
            case As8080Parser.OP_SHL:
            case As8080Parser.OP_SHL_2:
                return InfixOperator.SHL;
            case As8080Parser.OP_SHR:
            case As8080Parser.OP_SHR_2:
                return InfixOperator.SHR;
            case As8080Parser.OP_EQUAL:
                return InfixOperator.EQUAL;
            case As8080Parser.OP_LT:
                return InfixOperator.LT;
            case As8080Parser.OP_LTE:
                return InfixOperator.LTE;
            case As8080Parser.OP_GT:
                return InfixOperator.GT;
            case As8080Parser.OP_GTE:
                return InfixOperator.GTE;
            default:
                throw new IllegalArgumentException("Unknown infix operation: " + tokenType);
        }
    }
}
