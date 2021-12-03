package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;

public class InstrVisitor extends As8080ParserBaseVisitor<Instr> {

    @Override
    public Instr visitInstrNoArgs(As8080Parser.InstrNoArgsContext ctx) {
        return new InstrNoArgs(ctx.opcode);
    }

    @Override
    public Instr visitInstrReg(As8080Parser.InstrRegContext ctx) {
        return new InstrReg(ctx.opcode, ctx.reg.start);
    }

    @Override
    public Instr visitInstrRegReg(As8080Parser.InstrRegRegContext ctx) {
        return new InstrRegReg(ctx.opcode, ctx.dst.start, ctx.src.start);
    }

    @Override
    public Instr visitInstrRegPair(As8080Parser.InstrRegPairContext ctx) {
        return new InstrRegPair(ctx.opcode, ctx.regpair);
    }

    @Override
    public Instr visitInstrRegPairExpr(As8080Parser.InstrRegPairExprContext ctx) {
        return new InstrRegPairExpr(ctx.opcode, ctx.regpair, Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Instr visitInstrRegExpr(As8080Parser.InstrRegExprContext ctx) {
        return new InstrRegExpr(ctx.opcode, ctx.reg.start, Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Instr visitInstrExpr(As8080Parser.InstrExprContext ctx) {
        return new InstrExpr(ctx.opcode, Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Instr visitInstr8bitExpr(As8080Parser.Instr8bitExprContext ctx) {
        return new InstrExpr(ctx.opcode, Visitors.expr.visit(ctx.expr));
    }
}
