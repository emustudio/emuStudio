package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;

import static net.emustudio.plugins.compiler.as8080.CommonParsers.*;

public class CreateInstrVisitor extends As8080ParserBaseVisitor<Instr> {

    @Override
    public Instr visitInstrNoArgs(As8080Parser.InstrNoArgsContext ctx) {
        return new InstrNoArgs(ctx.opcode.getType());
    }

    @Override
    public Instr visitInstrReg(As8080Parser.InstrRegContext ctx) {
        return new InstrReg(ctx.opcode.getType(), parseReg(ctx.reg.start));
    }

    @Override
    public Instr visitInstrRegReg(As8080Parser.InstrRegRegContext ctx) {
        return new InstrRegReg(ctx.opcode.getType(), parseReg(ctx.dst.start), parseReg(ctx.src.start));
    }

    @Override
    public Instr visitInstrRegPair(As8080Parser.InstrRegPairContext ctx) {
        return new InstrRegPair(ctx.opcode.getType(), parseRegPair(ctx.regpair));
    }

    @Override
    public Instr visitInstrRegPairExpr(As8080Parser.InstrRegPairExprContext ctx) {
        InstrRegPairExpr instr = new InstrRegPairExpr(ctx.opcode.getType(), parseRegPair(ctx.regpair));
        instr.addChild(Visitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Instr visitInstrRegExpr(As8080Parser.InstrRegExprContext ctx) {
        InstrRegExpr instr = new InstrRegExpr(ctx.opcode.getType(), parseReg(ctx.reg.start));
        instr.addChild(Visitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Instr visitInstrExpr(As8080Parser.InstrExprContext ctx) {
        InstrExpr instr = new InstrExpr(ctx.opcode.getType());
        instr.addChild(Visitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Instr visitInstr8bitExpr(As8080Parser.Instr8bitExprContext ctx) {
        InstrExpr instr = new InstrExpr(ctx.opcode.getType());
        instr.addChild(Visitors.expr.visit(ctx.expr));
        return instr;
    }
}
