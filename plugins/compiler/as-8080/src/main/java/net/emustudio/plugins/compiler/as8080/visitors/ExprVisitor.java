package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.CommonParsers;
import net.emustudio.plugins.compiler.as8080.ast.expr.*;

public class ExprVisitor extends As8080ParserBaseVisitor<Expr> {

    @Override
    public Expr visitExprOct(As8080Parser.ExprOctContext ctx) {
        return new ExprNumber(CommonParsers.parseLitOct(ctx.num));
    }

    @Override
    public Expr visitExprHex1(As8080Parser.ExprHex1Context ctx) {
        return new ExprNumber(CommonParsers.parseLitHex1(ctx.num));
    }

    @Override
    public Expr visitExprHex2(As8080Parser.ExprHex2Context ctx) {
        return new ExprNumber(CommonParsers.parseLitHex2(ctx.num));
    }

    @Override
    public Expr visitExprDec(As8080Parser.ExprDecContext ctx) {
        return new ExprNumber(CommonParsers.parseLitDec(ctx.num));
    }

    @Override
    public Expr visitExprBin(As8080Parser.ExprBinContext ctx) {
        return new ExprNumber(CommonParsers.parseLitBin(ctx.num));
    }

    @Override
    public Expr visitExprId(As8080Parser.ExprIdContext ctx) {
        return new ExprId(ctx.id.getText());
    }

    @Override
    public Expr visitExprUnary(As8080Parser.ExprUnaryContext ctx) {
        ExprUnary unary = new ExprUnary(ctx.unaryop.getType());
        unary.addChild(visit(ctx.expr));
        return unary;
    }

    @Override
    public Expr visitExprInfix(As8080Parser.ExprInfixContext ctx) {
        ExprInfix infix = new ExprInfix(ctx.op.getType());
        infix.addChild(visit(ctx.expr1));
        infix.addChild(visit(ctx.expr2));
        return infix;
    }

    @Override
    public Expr visitExprCurrentAddress(As8080Parser.ExprCurrentAddressContext ctx) {
        return new ExprCurrentAddress();
    }
}
