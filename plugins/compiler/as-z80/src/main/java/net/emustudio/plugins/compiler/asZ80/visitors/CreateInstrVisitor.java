package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;

public class CreateInstrVisitor extends AsZ80ParserBaseVisitor<Node> {

    @Override
    public Node visitInstrNoArgs(InstrNoArgsContext ctx) {
        return new InstrNoArgs(ctx.opcode);
    }

    @Override
    public Node visitInstrReg(InstrRegContext ctx) {
        return new InstrReg(ctx.opcode, ctx.reg.getStart());
    }

    @Override
    public Node visitInstrRegReg(InstrRegRegContext ctx) {
        return new InstrRegReg(ctx.opcode, ctx.dst.getStart(), ctx.src.getStart());
    }

    @Override
    public Node visitInstrRegPair(InstrRegPairContext ctx) {
        return new InstrRegPair(ctx.opcode, ctx.regpair);
    }

    @Override
    public Node visitInstrRegPairExpr(InstrRegPairExprContext ctx) {
        InstrRegPairExpr instr = new InstrRegPairExpr(ctx.opcode, ctx.regpair);
        instr.addChild(CreateVisitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstrRegExpr(InstrRegExprContext ctx) {
        InstrRegExpr instr = new InstrRegExpr(ctx.opcode, ctx.reg.getStart());
        instr.addChild(CreateVisitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstrExpr(InstrExprContext ctx) {
        InstrExpr instr = new InstrExpr(ctx.opcode);
        instr.addChild(CreateVisitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstr8bitExpr(Instr8bitExprContext ctx) {
        InstrExpr instr = new InstrExpr(ctx.opcode);
        instr.addChild(CreateVisitors.expr.visit(ctx.expr));
        return instr;
    }
}
