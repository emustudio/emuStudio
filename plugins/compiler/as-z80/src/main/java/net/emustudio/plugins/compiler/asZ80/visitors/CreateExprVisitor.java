/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.compiler.antlr.ParsingUtils;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.expr.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

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
        Token token = ctx.unaryop;
        ExprUnary unary = new ExprUnary(
                new SourceCodePosition(token.getLine(), token.getCharPositionInLine(), sourceFileName),
                unaryOperator(token.getType())
        );
        unary.addChild(visit(ctx.expr));
        return unary;
    }

    @Override
    public Node visitExprInfix(ExprInfixContext ctx) {
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
    public Node visitExprParens(ExprParensContext ctx) {
        return visit(ctx.expr);
    }

    @Override
    public Node visitExprCurrentAddress(ExprCurrentAddressContext ctx) {
        Token start = ctx.getStart();
        return new ExprCurrentAddress(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));
    }

    private UnaryOperator unaryOperator(int tokenType) {
        switch (tokenType) {
            case OP_ADD:
                return UnaryOperator.ADD;
            case OP_SUBTRACT:
                return UnaryOperator.SUBTRACT;
            case OP_NOT:
            case OP_NOT_2:
                return UnaryOperator.NOT;
            default:
                throw new IllegalArgumentException("Unknown unary operation: " + tokenType);
        }
    }

    private InfixOperator infixOperator(int tokenType) {
        switch (tokenType) {
            case OP_ADD:
                return InfixOperator.ADD;
            case OP_SUBTRACT:
                return InfixOperator.SUBTRACT;
            case OP_DIVIDE:
                return InfixOperator.DIVIDE;
            case OP_MULTIPLY:
                return InfixOperator.MULTIPLY;
            case OP_MOD:
            case OP_MOD_2:
                return InfixOperator.MOD;
            case OP_AND:
                return InfixOperator.AND;
            case OP_OR:
                return InfixOperator.OR;
            case OP_XOR:
                return InfixOperator.XOR;
            case OP_SHL:
            case OP_SHL_2:
                return InfixOperator.SHL;
            case OP_SHR:
            case OP_SHR_2:
                return InfixOperator.SHR;
            case OP_EQUAL:
                return InfixOperator.EQUAL;
            case OP_LT:
                return InfixOperator.LT;
            case OP_LTE:
                return InfixOperator.LTE;
            case OP_GT:
                return InfixOperator.GT;
            case OP_GTE:
                return InfixOperator.GTE;
            default:
                throw new IllegalArgumentException("Unknown infix operation: " + tokenType);
        }
    }
}
