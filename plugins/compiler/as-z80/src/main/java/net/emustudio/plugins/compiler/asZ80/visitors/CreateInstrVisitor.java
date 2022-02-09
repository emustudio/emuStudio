package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;

public class CreateInstrVisitor extends AsZ80ParserBaseVisitor<Node> {

    @Override
    public Node visitInstrRP_NN(InstrRP_NNContext ctx) {
        InstrRP_NN instr = new InstrRP_NN(ctx.opcode, ctx.rp.start);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn));
        return instr;
    }

    @Override
    public Node visitInstrR_N(InstrR_NContext ctx) {
        InstrR_N instr = new InstrR_N(ctx.opcode, ctx.r.r);
        instr.addChild(CreateVisitors.expr.visit(ctx.n));
        return instr;
    }

    @Override
    public Node visitInstr8bitN(Instr8bitNContext ctx) {
        InstrN instr = new InstrN(ctx.opcode);
        instr.addChild(CreateVisitors.expr.visit(ctx.n));
        return instr;
    }

    @Override
    public Node visitInstrRef_NN_R(InstrRef_NN_RContext ctx) {
        InstrRef_NN_R instr = new InstrRef_NN_R(ctx.opcode, ctx.r);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn));
        return instr;
    }

    @Override
    public Node visitInstrRP_Ref_NN(InstrRP_Ref_NNContext ctx) {
        InstrRP_Ref_NN instr = new InstrRP_Ref_NN(ctx.opcode, ctx.rp);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn));
        return instr;
    }

    @Override
    public Node visitInstrR_Ref_NN(InstrR_Ref_NNContext ctx) {
        InstrR_Ref_NN instr = new InstrR_Ref_NN(ctx.opcode, ctx.r);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn));
        return instr;
    }

    @Override
    public Node visitInstrN(InstrNContext ctx) {
        InstrN instr = new InstrN(ctx.opcode);
        instr.addChild(CreateVisitors.expr.visit(ctx.n));
        return instr;
    }

    @Override
    public Node visitInstrNN(InstrNNContext ctx) {
        InstrNN instr = new InstrNN(ctx.opcode);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn));
        return instr;
    }

    @Override
    public Node visitInstrC_N(InstrC_NContext ctx) {
        InstrC_N instr = new InstrC_N(ctx.opcode, ctx.c);
        instr.addChild(CreateVisitors.expr.visit(ctx.n));
        return instr;
    }

    @Override
    public Node visitInstr(InstrContext ctx) {
        return new Instr(ctx.opcode);
    }

    @Override
    public Node visitInstrRef_RP(InstrRef_RPContext ctx) {
        return new InstrRef_RP(ctx.opcode, ctx.rp);
    }

    @Override
    public Node visitInstrA_Ref_RP(InstrA_Ref_RPContext ctx) {
        return new InstrA_Ref_RP(ctx.opcode, ctx.rp);
    }

    @Override
    public Node visitInstrR_R(InstrR_RContext ctx) {
        return new InstrR_R(ctx.opcode, ctx.dst.r, ctx.src.r);
    }

    @Override
    public Node visitInstrRP_RP(InstrRP_RPContext ctx) {
        return new InstrRP_RP(ctx.opcode, ctx.src, ctx.dst);
    }

    @Override
    public Node visitInstrRP(InstrRPContext ctx) {
        return new InstrRP(ctx.opcode, ctx.rp.start);
    }

    @Override
    public Node visitInstrRP2(InstrRP2Context ctx) {
        return new InstrRP(ctx.opcode, ctx.rp2.start);
    }

    @Override
    public Node visitInstrR(InstrRContext ctx) {
        return new InstrR(ctx.opcode, ctx.r.r);
    }

    @Override
    public Node visitInstrRef_RP_RP(InstrRef_RP_RPContext ctx) {
        return new InstrRef_RP_RP(ctx.opcode, ctx.src, ctx.dst);
    }

    @Override
    public Node visitInstrC(InstrCContext ctx) {
        return new InstrC(ctx.opcode, ctx.c.start);
    }

    @Override
    public Node visitInstrC_NN(InstrC_NNContext ctx) {
        InstrC_NN instr = new InstrC_NN(ctx.opcode, ctx.c.start);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn));
        return instr;
    }
}
