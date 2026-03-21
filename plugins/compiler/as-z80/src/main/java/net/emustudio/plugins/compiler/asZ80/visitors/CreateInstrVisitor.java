/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.CompilerTables;
import net.emustudio.plugins.compiler.asZ80.Pair;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprUnary;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class CreateInstrVisitor extends AsZ80ParserBaseVisitor<Node> {
    private static final Map<Integer, int[]> INSTR_XYZ = new HashMap<>();

    static {
        INSTR_XYZ.put(OPCODE_NOP, new int[]{0, 0, 0});
        INSTR_XYZ.put(OPCODE_RLCA, new int[]{0, 0, 7});
        INSTR_XYZ.put(OPCODE_RRCA, new int[]{0, 1, 7});
        INSTR_XYZ.put(OPCODE_RLA, new int[]{0, 2, 7});
        INSTR_XYZ.put(OPCODE_RRA, new int[]{0, 3, 7});
        INSTR_XYZ.put(OPCODE_DAA, new int[]{0, 4, 7});
        INSTR_XYZ.put(OPCODE_CPL, new int[]{0, 5, 7});
        INSTR_XYZ.put(OPCODE_SCF, new int[]{0, 6, 7});
        INSTR_XYZ.put(OPCODE_CCF, new int[]{0, 7, 7});
        INSTR_XYZ.put(OPCODE_HALT, new int[]{1, 6, 6});
        INSTR_XYZ.put(OPCODE_RET, new int[]{3, 1, 1});
        INSTR_XYZ.put(OPCODE_EXX, new int[]{3, 3, 1});
        INSTR_XYZ.put(OPCODE_DI, new int[]{3, 6, 3});
        INSTR_XYZ.put(OPCODE_EI, new int[]{3, 7, 3});
    }

    private final String sourceFileName;

    public CreateInstrVisitor(String sourceFileName) {
        this.sourceFileName = Objects.requireNonNull(sourceFileName);
    }

    /**
     * Wraps the displacement expression in a unary minus if the displacement operator is OP_SUBTRACT.
     */
    private Node visitDisplacementExpr(RDisplacementContext d) {
        Node expr = exprVisitor().visit(d.n);
        if (d.op.getType() == OP_SUBTRACT) {
            ExprUnary unary = new ExprUnary(sourceFileName, d.op);
            unary.addChild(expr);
            return unary.setSizeBytes(1);
        }
        return expr.setSizeBytes(1);
    }

    @Override
    public Node visitInstrXDCB_R(InstrXDCB_RContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int y = CompilerTables.rot.get(ctx.opcode.getType());
        int z = CompilerTables.registers.get(ctx.r.start.getType());
        Node instr = new InstrXDCB(sourceFileName, ctx.opcode, prefix, y, z).setSizeBytes(4);
        instr.addChild(visitDisplacementExpr(ctx.d));
        return instr;
    }

    @Override
    public Node visitInstrXDCB(InstrXDCBContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int y = CompilerTables.rot.get(ctx.opcode.getType());
        Node instr = new InstrXDCB(sourceFileName, ctx.opcode, prefix, y, 6).setSizeBytes(4);
        instr.addChild(visitDisplacementExpr(ctx.d));
        return instr;
    }

    @Override
    public Node visitInstrXDCB_N(InstrXDCB_NContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        Node instr = new InstrXDCB(sourceFileName, ctx.opcode, prefix, 0, 6).setSizeBytes(4);
        instr.addChild(exprVisitor().visit(ctx.n).setMaxValue(7));
        instr.addChild(visitDisplacementExpr(ctx.d));
        return instr;
    }

    @Override
    public Node visitInstrXDCB_N_R(InstrXDCB_N_RContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int z = CompilerTables.registers.get(ctx.r.start.getType());
        Node instr = new InstrXDCB(sourceFileName, ctx.opcode, prefix, 0, z).setSizeBytes(4);
        instr.addChild(exprVisitor().visit(ctx.n).setMaxValue(7));
        instr.addChild(visitDisplacementExpr(ctx.d));
        return instr;
    }

    @Override
    public Node visitInstrED_R2(InstrED_R2Context ctx) {
        int y = CompilerTables.registers.get(ctx.r.start.getType());
        int z = (ctx.opcode.getType() == OPCODE_IN) ? 0 : 1;
        return new InstrED(sourceFileName, ctx.opcode, y, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED_C(InstrED_CContext ctx) {
        int z = (ctx.opcode.getType() == OPCODE_IN) ? 0 : 1;
        return new InstrED(sourceFileName, ctx.opcode, 6, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED_RP(InstrED_RPContext ctx) {
        int q = (ctx.opcode.getType() == OPCODE_SBC) ? 0 : 1;
        int p = CompilerTables.regPairs.get(ctx.rp.start.getType());
        return new InstrED(sourceFileName, ctx.opcode, p, q, 2).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED_NN_RP(InstrED_NN_RPContext ctx) {
        int p = CompilerTables.regPairs.get(ctx.rp.getType());
        Node instr = new InstrED(sourceFileName, ctx.opcode, p, 0, 3).setSizeBytes(4);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrED_RP_NN(InstrED_RP_NNContext ctx) {
        int p = CompilerTables.regPairs.get(ctx.rp.getType());
        Node instr = new InstrED(sourceFileName, ctx.opcode, p, 1, 3).setSizeBytes(4);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrED_IM(InstrED_IMContext ctx) {
        int y = CompilerTables.im.get(ctx.im.getType());
        return new InstrED(sourceFileName, ctx.opcode, y, 6).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED_RIA_RIA(InstrED_RIA_RIAContext ctx) {
        int y = 0; // ctx.dst.getType() == REG_I
        if (ctx.dst.getType() == REG_R) {
            y = 1;
        } else if (ctx.src.getType() == REG_I) {
            y = 2;
        } else if (ctx.src.getType() == REG_R) {
            y = 3;
        }
        return new InstrED(sourceFileName, ctx.opcode, y, 7).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED(InstrEDContext ctx) {
        switch (ctx.opcode.getType()) {
            case OPCODE_NEG:
                return new InstrED(sourceFileName, ctx.opcode, 0, 4).setSizeBytes(2);
            case OPCODE_RETN:
                return new InstrED(sourceFileName, ctx.opcode, 0, 5).setSizeBytes(2);
            case OPCODE_RETI:
                return new InstrED(sourceFileName, ctx.opcode, 1, 5).setSizeBytes(2);
            case OPCODE_RRD:
                return new InstrED(sourceFileName, ctx.opcode, 4, 7).setSizeBytes(2);
            case OPCODE_RLD:
                return new InstrED(sourceFileName, ctx.opcode, 5, 7).setSizeBytes(2);
        }
        Pair<Integer, Integer> yz = CompilerTables.block.get(ctx.opcode.getType());
        return new InstrED(sourceFileName, ctx.opcode, yz.l, yz.r).setSizeBytes(2);
    }

    @Override
    public Node visitInstrCB(InstrCBContext ctx) {
        int y = CompilerTables.rot.get(ctx.opcode.getType());
        int z = CompilerTables.registers.get(ctx.r.r.getType());
        return new InstrCB(sourceFileName, ctx.opcode, y, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrCB_N_R(InstrCB_N_RContext ctx) {
        int z = CompilerTables.registers.get(ctx.r.r.getType());
        Node instr = new InstrCB(sourceFileName, ctx.opcode, 0, z).setSizeBytes(2);
        instr.addChild(exprVisitor().visit(ctx.n).setMaxValue(7));
        return instr;
    }

    @Override
    public Node visitInstrXD_II_NN(InstrXD_II_NNContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 0, 0, 2, 1).setSizeBytes(4);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrXD_II_RP(InstrXD_II_RPContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int p = CompilerTables.regPairsII.get(ctx.rp.getType());
        return new InstrXD(sourceFileName, ctx.opcode, prefix, 0, 1, p, 1).setSizeBytes(2);
    }

    @Override
    public Node visitInstrXD_Ref_NN_II(InstrXD_Ref_NN_IIContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 0, 0, 2, 2).setSizeBytes(4);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrXD_II_Ref_NN(InstrXD_II_Ref_NNContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 0, 1, 2, 2).setSizeBytes(4);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrXD_IIHL_N(InstrXD_IIHL_NContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int y = CompilerTables.registers.get(ctx.ii.start.getType());
        Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 0, y, 6).setSizeBytes(3);
        instr.addChild(exprVisitor().visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXD_Ref_II_N_N(InstrXD_Ref_II_N_NContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 0, 6, 6).setSizeBytes(4);
        instr.addChild(visitDisplacementExpr(ctx.d));
        instr.addChild(exprVisitor().visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXD_IIHL_R(InstrXD_IIHL_RContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int y = CompilerTables.registers.get(ctx.ii.start.getType());
        int z = CompilerTables.registers.get(ctx.r.start.getType());
        return new InstrXD(sourceFileName, ctx.opcode, prefix, 1, y, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrXD_R_IIHL(InstrXD_R_IIHLContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int y = CompilerTables.registers.get(ctx.r.start.getType());
        int z = CompilerTables.registers.get(ctx.ii.start.getType());
        return new InstrXD(sourceFileName, ctx.opcode, prefix, 1, y, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrXD_Ref_II_N_R(InstrXD_Ref_II_N_RContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int z = CompilerTables.registers.get(ctx.r.start.getType());
        Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 1, 6, z).setSizeBytes(3);
        instr.addChild(visitDisplacementExpr(ctx.d));
        return instr;
    }

    @Override
    public Node visitInstrXD_R_Ref_II_N(InstrXD_R_Ref_II_NContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int y = CompilerTables.registers.get(ctx.r.start.getType());
        Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 1, y, 6).setSizeBytes(3);
        instr.addChild(visitDisplacementExpr(ctx.d));
        return instr;
    }

    @Override
    public Node visitInstrXD_Ref_II_N(InstrXD_Ref_II_NContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        if (ctx.opcode.getType() == OPCODE_INC) {
            Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 0, 6, 4).setSizeBytes(3);
            instr.addChild(visitDisplacementExpr(ctx.d));
            return instr;
        } else if (ctx.opcode.getType() == OPCODE_DEC) {
            Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 0, 6, 5).setSizeBytes(3);
            instr.addChild(visitDisplacementExpr(ctx.d));
            return instr;
        }
        int y = CompilerTables.alu.get(ctx.opcode.getType());
        Node instr = new InstrXD(sourceFileName, ctx.opcode, prefix, 2, y, 6).setSizeBytes(3);
        instr.addChild(visitDisplacementExpr(ctx.d));
        return instr;
    }

    @Override
    public Node visitInstrXD_IIHL(InstrXD_IIHLContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int r = CompilerTables.registers.get(ctx.ii.start.getType());
        if (ctx.opcode.getType() == OPCODE_INC) {
            return new InstrXD(sourceFileName, ctx.opcode, prefix, 0, r, 4).setSizeBytes(2);
        } else if (ctx.opcode.getType() == OPCODE_DEC) {
            return new InstrXD(sourceFileName, ctx.opcode, prefix, 0, r, 5).setSizeBytes(2);
        }
        int y = CompilerTables.alu.get(ctx.opcode.getType());
        return new InstrXD(sourceFileName, ctx.opcode, prefix, 2, y, r).setSizeBytes(2);
    }

    @Override
    public Node visitInstrXD_II(InstrXD_IIContext ctx) {
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int opcode = ctx.opcode.getType();
        int x = (opcode == OPCODE_INC || opcode == OPCODE_DEC) ? 0 : 3;
        int z = (opcode == OPCODE_PUSH) ? 5 : ((opcode == OPCODE_POP || opcode == OPCODE_JP || opcode == OPCODE_LD) ? 1 : 3);
        int q = 0;
        int p = (opcode == OPCODE_LD) ? 3 : 2;

        switch (opcode) {
            case OPCODE_DEC:
            case OPCODE_JP:
            case OPCODE_LD:
                q = 1;
                break;
        }

        return new InstrXD(sourceFileName, ctx.opcode, prefix, x, q, p, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrRef_NN_R(InstrRef_NN_RContext ctx) {
        int p = (ctx.r.getType() == REG_HL) ? 2 : 3;
        Node instr = new Instr(sourceFileName, ctx.opcode, 0, 0, p, 2).setSizeBytes(3);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrRP_Ref_NN(InstrRP_Ref_NNContext ctx) {
        Node instr = new Instr(sourceFileName, ctx.opcode, 0, 1, 2, 2).setSizeBytes(3);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrR_Ref_NN(InstrR_Ref_NNContext ctx) {
        Node instr = new Instr(sourceFileName, ctx.opcode, 0, 1, 3, 2).setSizeBytes(3);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrA_Ref_RP(InstrA_Ref_RPContext ctx) {
        int p = CompilerTables.regPairs.get(ctx.rp.getType());
        return new Instr(sourceFileName, ctx.opcode, 0, 1, p, 2).setSizeBytes(1);
    }

    @Override
    public Node visitInstrRef_RP(InstrRef_RPContext ctx) {
        int p = CompilerTables.regPairs.get(ctx.rp.getType());
        int x = (ctx.opcode.getType() == OPCODE_LD) ? 0 : 3;
        int z = (ctx.opcode.getType() == OPCODE_LD) ? 2 : 1;
        int q = (ctx.opcode.getType() == OPCODE_LD) ? 0 : 1;
        return new Instr(sourceFileName, ctx.opcode, x, q, p, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrRef_RP_RP(InstrRef_RP_RPContext ctx) {
        return new Instr(sourceFileName, ctx.opcode, 3, 4, 3).setSizeBytes(1);
    }

    @Override
    public Node visitInstrR_R(InstrR_RContext ctx) {
        int y = CompilerTables.registers.get(ctx.dst.r.getType());
        int z = CompilerTables.registers.get(ctx.src.r.getType());
        return new Instr(sourceFileName, ctx.opcode, 1, y, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrR_N(InstrR_NContext ctx) {
        int y = CompilerTables.registers.get(ctx.r.r.getType());
        Node instr = new Instr(sourceFileName, ctx.opcode, 0, y, 6).setSizeBytes(2);
        instr.addChild(exprVisitor().visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrC_N(InstrC_NContext ctx) {
        int y = CompilerTables.conditions.get(ctx.c.getType()) + 4;
        Node instr = new Instr(sourceFileName, ctx.opcode, 0, y, 0).setSizeBytes(2);
        instr.addChild(exprVisitor().visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrC(InstrCContext ctx) {
        int y = CompilerTables.conditions.get(ctx.c.start.getType());
        return new Instr(sourceFileName, ctx.opcode, 3, y, 0).setSizeBytes(1);
    }

    @Override
    public Node visitInstrC_NN(InstrC_NNContext ctx) {
        int y = CompilerTables.conditions.get(ctx.c.start.getType());
        int z = (ctx.opcode.getType() == OPCODE_JP) ? 2 : 4;
        Node instr = new Instr(sourceFileName, ctx.opcode, 3, y, z).setSizeBytes(3);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrRP_NN(InstrRP_NNContext ctx) {
        int p = CompilerTables.regPairs.get(ctx.rp.start.getType());
        Node instr = new Instr(sourceFileName, ctx.opcode, 0, 0, p, 1).setSizeBytes(3);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrRP_RP(InstrRP_RPContext ctx) {
        int x = (ctx.opcode.getType() == OPCODE_EX && ctx.dst.getType() == REG_AF) ? 0 : 3;
        int z = (ctx.opcode.getType() == OPCODE_LD) ? 1 : ((ctx.dst.getType() == REG_AF) ? 0 : 3);
        int p = (ctx.opcode.getType() == OPCODE_LD) ? 3 : ((ctx.dst.getType() == REG_AF) ? 0 : 2);
        return new Instr(sourceFileName, ctx.opcode, x, 1, p, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrNN(InstrNNContext ctx) {
        int z = (ctx.opcode.getType() == OPCODE_JP) ? 3 : 5;
        int y = (ctx.opcode.getType() == OPCODE_JP) ? 0 : 1;
        Node instr = new Instr(sourceFileName, ctx.opcode, 3, y, z).setSizeBytes(3);
        instr.addChild(exprVisitor().visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrN(InstrNContext ctx) {
        int opcode = ctx.opcode.getType();

        boolean djnzOrJr = opcode == OPCODE_DJNZ || opcode == OPCODE_JR;
        boolean djnzOrOut = opcode == OPCODE_DJNZ || opcode == OPCODE_OUT;
        boolean rst = opcode == OPCODE_RST;

        int x = (djnzOrJr) ? 0 : 3;
        int z = (djnzOrJr) ? 0 : ((opcode == OPCODE_OUT || opcode == OPCODE_IN) ? 3 : (rst ? 7 : 6));
        int y = djnzOrOut ? 2 : ((opcode == OPCODE_JR || opcode == OPCODE_IN) ? 3 : (rst ? 0 : CompilerTables.alu.get(opcode)));

        Node instr = new Instr(sourceFileName, ctx.opcode, x, y, z).setSizeBytes(rst ? 1 : 2);
        instr.addChild(exprVisitor().visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrRP(InstrRPContext ctx) {
        int q = (ctx.opcode.getType() == OPCODE_INC) ? 0 : 1;
        int p = CompilerTables.regPairs.get(ctx.rp.start.getType());
        int z = (ctx.opcode.getType() == OPCODE_ADD) ? 1 : 3;
        return new Instr(sourceFileName, ctx.opcode, 0, q, p, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrRP2(InstrRP2Context ctx) {
        int p = CompilerTables.regPairs2.get(ctx.rp2.start.getType());
        int z = (ctx.opcode.getType() == OPCODE_POP) ? 1 : 5;
        return new Instr(sourceFileName, ctx.opcode, 3, 0, p, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrR(InstrRContext ctx) {
        int opcode = ctx.opcode.getType();
        boolean incDec = (opcode == OPCODE_INC || opcode == OPCODE_DEC);
        int reg = CompilerTables.registers.get(ctx.r.r.getType());

        int x = incDec ? 0 : 2;
        int y = incDec ? reg : CompilerTables.alu.get(ctx.opcode.getType());
        int z = (opcode == OPCODE_INC) ? 4 : ((opcode == OPCODE_DEC) ? 5 : reg);
        return new Instr(sourceFileName, ctx.opcode, x, y, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstr(InstrContext ctx) {
        int[] xyzValues = INSTR_XYZ.get(ctx.opcode.getType());
        return new Instr(sourceFileName, ctx.opcode, xyzValues[0], xyzValues[1], xyzValues[2]).setSizeBytes(1);
    }

    private CreateExprVisitor exprVisitor() {
        return CreateVisitors.expr(sourceFileName);
    }
}
