package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ParsingUtils;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.expr.*;
import org.antlr.v4.runtime.Token;

public class CreateExprVisitor extends AsZ80ParserBaseVisitor<Node> {

    @Override
    public Node visitExprOct(ExprOctContext ctx) {
        return new ExprNumber(ctx.num, ParsingUtils::parseLitOct);
    }

    @Override
    public Node visitExprHex1(ExprHex1Context ctx) {
        return new ExprNumber(ctx.num, ParsingUtils::parseLitHex1);
    }

    @Override
    public Node visitExprHex2(ExprHex2Context ctx) {
        return new ExprNumber(ctx.num, ParsingUtils::parseLitHex2);
    }

    @Override
    public Node visitExprDec(ExprDecContext ctx) {
        return new ExprNumber(ctx.num, ParsingUtils::parseLitDec);
    }

    @Override
    public Node visitExprBin(ExprBinContext ctx) {
        return new ExprNumber(ctx.num, ParsingUtils::parseLitBin);
    }

    @Override
    public Node visitExprId(ExprIdContext ctx) {
        return new ExprId(ctx.id);
    }

    @Override
    public Node visitExprString(ExprStringContext ctx) {
        return new ExprString(ctx.str);
    }

    @Override
    public Node visitExprUnary(ExprUnaryContext ctx) {
        ExprUnary unary = new ExprUnary(ctx.unaryop);
        unary.addChild(visit(ctx.expr));
        return unary;
    }

    @Override
    public Node visitExprInfix(ExprInfixContext ctx) {
        ExprInfix infix = new ExprInfix(ctx.op);
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
        return new ExprCurrentAddress(start.getLine(), start.getCharPositionInLine());
    }
}
