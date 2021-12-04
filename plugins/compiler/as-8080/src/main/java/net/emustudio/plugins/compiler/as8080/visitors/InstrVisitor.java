package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;

import static net.emustudio.plugins.compiler.as8080.CommonParsers.*;

public class InstrVisitor extends As8080ParserBaseVisitor<Instr> {

    @Override
    public Instr visitInstrNoArgs(As8080Parser.InstrNoArgsContext ctx) {
        return new InstrNoArgs(parseOpcode(ctx.opcode));
    }

    @Override
    public Instr visitInstrReg(As8080Parser.InstrRegContext ctx) {
        return new InstrReg(parseOpcode(ctx.opcode), parseReg(ctx.reg.start));
    }

    @Override
    public Instr visitInstrRegReg(As8080Parser.InstrRegRegContext ctx) {
        return new InstrRegReg(parseOpcode(ctx.opcode), parseReg(ctx.dst.start), parseReg(ctx.src.start));
    }

    @Override
    public Instr visitInstrRegPair(As8080Parser.InstrRegPairContext ctx) {
        return new InstrRegPair(parseOpcode(ctx.opcode), parseRegPair(ctx.regpair));
    }

    @Override
    public Instr visitInstrRegPairExpr(As8080Parser.InstrRegPairExprContext ctx) {
        return new InstrRegPairExpr(parseOpcode(ctx.opcode), parseRegPair(ctx.regpair), Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Instr visitInstrRegExpr(As8080Parser.InstrRegExprContext ctx) {
        return new InstrRegExpr(parseOpcode(ctx.opcode), parseReg(ctx.reg.start), Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Instr visitInstrExpr(As8080Parser.InstrExprContext ctx) {
        return new InstrExpr(parseOpcode(ctx.opcode), Visitors.expr.visit(ctx.expr));
    }

    @Override
    public Instr visitInstr8bitExpr(As8080Parser.Instr8bitExprContext ctx) {
        return new InstrExpr(parseOpcode(ctx.opcode), Visitors.expr.visit(ctx.expr));
    }
}
