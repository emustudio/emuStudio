package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.CommonParsers;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.expr.*;
import org.antlr.v4.runtime.Token;

public class CreateExprVisitor extends As8080ParserBaseVisitor<Node> {

    @Override
    public Node visitExprOct(As8080Parser.ExprOctContext ctx) {
        return new ExprNumber(ctx.num, CommonParsers::parseLitOct);
    }

    @Override
    public Node visitExprHex1(As8080Parser.ExprHex1Context ctx) {
        return new ExprNumber(ctx.num, CommonParsers::parseLitHex1);
    }

    @Override
    public Node visitExprHex2(As8080Parser.ExprHex2Context ctx) {
        return new ExprNumber(ctx.num, CommonParsers::parseLitHex2);
    }

    @Override
    public Node visitExprDec(As8080Parser.ExprDecContext ctx) {
        return new ExprNumber(ctx.num, CommonParsers::parseLitDec);
    }

    @Override
    public Node visitExprBin(As8080Parser.ExprBinContext ctx) {
        return new ExprNumber(ctx.num, CommonParsers::parseLitBin);
    }

    @Override
    public Node visitExprId(As8080Parser.ExprIdContext ctx) {
        return new ExprId(ctx.id);
    }

    @Override
    public Node visitExprUnary(As8080Parser.ExprUnaryContext ctx) {
        ExprUnary unary = new ExprUnary(ctx.unaryop);
        unary.addChild(visit(ctx.expr));
        return unary;
    }

    @Override
    public Node visitExprInfix(As8080Parser.ExprInfixContext ctx) {
        ExprInfix infix = new ExprInfix(ctx.op);
        infix.addChild(visit(ctx.expr1));
        infix.addChild(visit(ctx.expr2));
        return infix;
    }

    @Override
    public Node visitExprCurrentAddress(As8080Parser.ExprCurrentAddressContext ctx) {
        Token start = ctx.getStart();
        return new ExprCurrentAddress(start.getLine(), start.getCharPositionInLine());
    }
}
