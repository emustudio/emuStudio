package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.CompilerTables;
import net.emustudio.plugins.compiler.asZ80.Pair;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class CreateInstrVisitor extends AsZ80ParserBaseVisitor<Node> {

    @Override
    public Node visitInstrXDCB_R(InstrXDCB_RContext ctx) {
        //  opcode=OPCODE_RLC d=rDisplacement SEP_COMMA r=rRegister2                      # instrXDCB_R
        //  | opcode=OPCODE_RRC d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
        //  | opcode=OPCODE_RL d=rDisplacement SEP_COMMA r=rRegister2                     # instrXDCB_R
        //  | opcode=OPCODE_RR d=rDisplacement SEP_COMMA r=rRegister2                     # instrXDCB_R
        //  | opcode=OPCODE_SLA d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
        //  | opcode=OPCODE_SRA d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
        //  | opcode=OPCODE_SLL d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
        //  | opcode=OPCODE_SRL d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int y = CompilerTables.rot.get(ctx.opcode.getType());
        int z = CompilerTables.registers.get(ctx.r.start.getType());
        Node instr = new InstrXDCB(ctx.opcode, prefix, y, z).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXDCB(InstrXDCBContext ctx) {
        //  | opcode=OPCODE_SRL d=rDisplacement                                           # instrXDCB
        //  | opcode=OPCODE_RLC d=rDisplacement                                           # instrXDCB
        //  | opcode=OPCODE_RRC d=rDisplacement                                           # instrXDCB
        //  | opcode=OPCODE_RL d=rDisplacement                                            # instrXDCB
        //  | opcode=OPCODE_RR d=rDisplacement                                            # instrXDCB
        //  | opcode=OPCODE_SLA d=rDisplacement                                           # instrXDCB
        //  | opcode=OPCODE_SRA d=rDisplacement                                           # instrXDCB
        //  | opcode=OPCODE_SLL d=rDisplacement                                           # instrXDCB
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int y = CompilerTables.rot.get(ctx.opcode.getType());
        Node instr = new InstrXDCB(ctx.opcode, prefix, y, 6).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXDCB_N(InstrXDCB_NContext ctx) {
        // | opcode=OPCODE_BIT n=rExpression SEP_COMMA d=rDisplacement                   # instrXDCB_N
        // | opcode=OPCODE_RES n=rExpression SEP_COMMA d=rDisplacement                   # instrXDCB_N
        // | opcode=OPCODE_SET n=rExpression SEP_COMMA d=rDisplacement                   # instrXDCB_N
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        Node instr = new InstrXDCB(ctx.opcode, prefix, 0, 6).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
        instr.addChild(CreateVisitors.expr.visit(ctx.n).setMaxValue(7));
        return super.visitInstrXDCB_N(ctx);
    }

    @Override
    public Node visitInstrXDCB_N_R(InstrXDCB_N_RContext ctx) {
        //  | opcode=OPCODE_RES n=rExpression SEP_COMMA d=rDisplacement SEP_COMMA r=rRegister2  # instrXDCB_N_R
        //  | opcode=OPCODE_SET n=rExpression SEP_COMMA d=rDisplacement SEP_COMMA r=rRegister2  # instrXDCB_N_R
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int z = CompilerTables.registers.get(ctx.r.start.getType());
        Node instr = new InstrXDCB(ctx.opcode, prefix, 0, z).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
        instr.addChild(CreateVisitors.expr.visit(ctx.n).setMaxValue(7));
        return instr;
    }

    @Override
    public Node visitInstrED_R2(InstrED_R2Context ctx) {
        //  | opcode=OPCODE_IN r=rRegister2 SEP_COMMA SEP_LPAR REG_C SEP_RPAR             # instrED_R2
        //  | opcode=OPCODE_OUT SEP_LPAR REG_C SEP_RPAR SEP_COMMA r=rRegister2            # instrED_R2
        int y = CompilerTables.registers.get(ctx.r.start.getType());
        int z = (ctx.opcode.getType() == OPCODE_IN) ? 0 : 1;
        return new InstrED(ctx.opcode, y, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED_C(InstrED_CContext ctx) {
        //  | opcode=OPCODE_IN SEP_LPAR REG_C SEP_RPAR                                    # instrED_C
        //  | opcode=OPCODE_OUT SEP_LPAR REG_C SEP_RPAR SEP_COMMA n=LIT_NUMBER            # instrED_C
        int z = (ctx.opcode.getType() == OPCODE_IN) ? 0 : 1;
        return new InstrED(ctx.opcode, 6, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED_RP(InstrED_RPContext ctx) {
        //  | opcode=OPCODE_SBC REG_HL SEP_COMMA rp=rRegPair                              # instrED_RP
        //  | opcode=OPCODE_ADC REG_HL SEP_COMMA rp=rRegPair                              # instrED_RP
        int q = (ctx.opcode.getType() == OPCODE_SBC) ? 0 : 1;
        int p = CompilerTables.regPairs.get(ctx.rp.start.getType());
        return new InstrED(ctx.opcode, p, q, 2).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED_NN_RP(InstrED_NN_RPContext ctx) {
        //  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA rp=rRegPair     # instrED_NN_RP
        int q = 0;
        int p = CompilerTables.regPairs.get(ctx.rp.start.getType());
        Node instr = new InstrED(ctx.opcode, p, q, 3).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrED_RP_NN(InstrED_RP_NNContext ctx) {
        //  | opcode=OPCODE_LD rp=rRegPair SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR     # instrED_RP_NN
        int q = 1;
        int p = CompilerTables.regPairs.get(ctx.rp.start.getType());
        Node instr = new InstrED(ctx.opcode, p, q, 3).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrED_IM(InstrED_IMContext ctx) {
        //  | opcode=OPCODE_IM im=(IM_0|IM_1|IM_2|IM_01)                                  # instrED_IM
        int y = CompilerTables.im.get(ctx.im.getType());
        return new InstrED(ctx.opcode, y, 6).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED_RIA_RIA(InstrED_RIA_RIAContext ctx) {
        //  | opcode=OPCODE_LD dst=REG_I src=REG_A                                        # instrED_RIA_RIA
        //  | opcode=OPCODE_LD dst=REG_R src=REG_A                                        # instrED_RIA_RIA
        //  | opcode=OPCODE_LD dst=REG_A src=REG_I                                        # instrED_RIA_RIA
        //  | opcode=OPCODE_LD dst=REG_A src=REG_R                                        # instrED_RIA_RIA
        int y = 0; // ctx.dst.getType() == REG_I
        if (ctx.dst.getType() == REG_R) {
            y = 1;
        } else if (ctx.src.getType() == REG_I) {
            y = 2;
        } else if (ctx.src.getType() == REG_R) {
            y = 3;
        }
        return new InstrED(ctx.opcode, y, 7).setSizeBytes(2);
    }

    @Override
    public Node visitInstrED(InstrEDContext ctx) {
        //  | opcode=OPCODE_NEG                                                           # instrED
        //  | opcode=OPCODE_RETN                                                          # instrED
        //  | opcode=OPCODE_RETI                                                          # instrED
        //  | opcode=OPCODE_RLD                                                           # instrED
        //  | opcode=OPCODE_RRD                                                           # instrED
        //  | opcode=OPCODE_LDI                                                           # instrED
        //  | opcode=OPCODE_LDIR                                                          # instrED
        //  | opcode=OPCODE_CPI                                                           # instrED
        //  | opcode=OPCODE_CPIR                                                          # instrED
        //  | opcode=OPCODE_INI                                                           # instrED
        //  | opcode=OPCODE_INIR                                                          # instrED
        //  | opcode=OPCODE_OUTI                                                          # instrED
        //  | opcode=OPCODE_OTIR                                                          # instrED
        //  | opcode=OPCODE_LDD                                                           # instrED
        //  | opcode=OPCODE_LDDR                                                          # instrED
        //  | opcode=OPCODE_CPD                                                           # instrED
        //  | opcode=OPCODE_CPDR                                                          # instrED
        //  | opcode=OPCODE_IND                                                           # instrED
        //  | opcode=OPCODE_INDR                                                          # instrED
        //  | opcode=OPCODE_OUTD                                                          # instrED
        //  | opcode=OPCODE_OTDR                                                          # instrED
        switch (ctx.opcode.getType()) {
            case OPCODE_NEG:
                return new InstrED(ctx.opcode, 0, 4);
            case OPCODE_RETN:
                return new InstrED(ctx.opcode, 0, 5);
            case OPCODE_RETI:
                return new InstrED(ctx.opcode, 1, 5);
            case OPCODE_RRD:
                return new InstrED(ctx.opcode, 4, 7);
            case OPCODE_RLD:
                return new InstrED(ctx.opcode, 5, 7);
        }

        Pair<Integer, Integer> yz = CompilerTables.block.get(ctx.opcode.getType());
        return new InstrED(ctx.opcode, yz.l, yz.r).setSizeBytes(2);
    }

    @Override
    public Node visitInstrCB(InstrCBContext ctx) {
        //  | opcode=OPCODE_RLC r=rRegister                                               # instrCB
        //  | opcode=OPCODE_RRC r=rRegister                                               # instrCB
        //  | opcode=OPCODE_RL r=rRegister                                                # instrCB
        //  | opcode=OPCODE_RR r=rRegister                                                # instrCB
        //  | opcode=OPCODE_SLA r=rRegister                                               # instrCB
        //  | opcode=OPCODE_SRA r=rRegister                                               # instrCB
        //  | opcode=OPCODE_SLL r=rRegister                                               # instrCB
        //  | opcode=OPCODE_SRL r=rRegister                                               # instrCB
        int y = CompilerTables.rot.get(ctx.opcode.getType());
        int z = CompilerTables.registers.get(ctx.r.r.getType());
        return new InstrCB(ctx.opcode, y, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrCB_N_R(InstrCB_N_RContext ctx) {
        //  | opcode=OPCODE_BIT n=rExpression SEP_COMMA r=rRegister                       # instrCB_N_R
        //  | opcode=OPCODE_RES n=rExpression SEP_COMMA r=rRegister                       # instrCB_N_R
        //  | opcode=OPCODE_SET n=rExpression SEP_COMMA r=rRegister                       # instrCB_N_R
        int z = CompilerTables.registers.get(ctx.r.r.getType());
        // y needs to be computed from expr
        Node instr = new InstrCB(ctx.opcode, 0, z).setSizeBytes(2);
        instr.addChild(CreateVisitors.expr.visit(ctx.n).setMaxValue(7));
        return instr;
    }

    @Override
    public Node visitInstrXD_II_NN(InstrXD_II_NNContext ctx) {
        //  | opcode=OPCODE_LD ii=rII SEP_COMMA nn=rExpression                            # instrXD_II_NN
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        Node instr = new InstrXD(ctx.opcode, prefix, 0, 0, 2, 1).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrXD_II_RP(InstrXD_II_RPContext ctx) {
        //  | opcode=OPCODE_ADD ii=rII SEP_COMMA rp=(REG_BC|REG_DE|REG_IX|REG_IY|REG_SP)  # instrXD_II_RP
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int p = CompilerTables.regPairsII.get(ctx.rp.getType());
        return new InstrXD(ctx.opcode, prefix, 0, 1, p, 1).setSizeBytes(2);
    }

    @Override
    public Node visitInstrXD_Ref_NN_II(InstrXD_Ref_NN_IIContext ctx) {
        //  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA ii=rII          # instrXD_Ref_NN_II
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        Node instr = new InstrXD(ctx.opcode, prefix, 0, 0, 2, 2).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrXD_II_Ref_NN(InstrXD_II_Ref_NNContext ctx) {
        //  | opcode=OPCODE_LD ii=rII SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR          # instrXD_II_Ref_NN
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        Node instr = new InstrXD(ctx.opcode, prefix, 0, 1, 2, 2).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrXD_IIHL_N(InstrXD_IIHL_NContext ctx) {
        //  | opcode=OPCODE_LD ii=rII_HL SEP_COMMA n=rExpression                          # instrXD_IIHL_N
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int y = CompilerTables.registers.get(ctx.ii.start.getType());
        Node instr = new InstrXD(ctx.opcode, prefix, 0, y, 6).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXD_Ref_II_N_N(InstrXD_Ref_II_N_NContext ctx) {
        //  | opcode=OPCODE_LD d=rDisplacement SEP_COMMA n=rExpression                    # instrXD_Ref_II_N_N
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        Node instr = new InstrXD(ctx.opcode, prefix, 0, 6, 6).setSizeBytes(4);
        instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
        instr.addChild(CreateVisitors.expr.visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXD_IIHL_R(InstrXD_IIHL_RContext ctx) {
        //  | opcode=OPCODE_LD ii=rII_HL SEP_COMMA r=rRegisterII                          # instrXD_IIHL_R
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int y = CompilerTables.registers.get(ctx.ii.start.getType());
        int z = CompilerTables.registers.get(ctx.r.start.getType());
        return new InstrXD(ctx.opcode, prefix, 1, y, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrXD_R_IIHL(InstrXD_R_IIHLContext ctx) {
        //  | opcode=OPCODE_LD r=rRegisterII SEP_COMMA ii=rII_HL                          # instrXD_R_IIHL
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int y = CompilerTables.registers.get(ctx.r.start.getType());
        int z = CompilerTables.registers.get(ctx.ii.start.getType());
        return new InstrXD(ctx.opcode, prefix, 1, y, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrXD_Ref_II_N_R(InstrXD_Ref_II_N_RContext ctx) {
        //  | opcode=OPCODE_LD d=rDisplacement SEP_COMMA r=rRegister2                     # instrXD_Ref_II_N_R
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int z = CompilerTables.registers.get(ctx.r.start.getType());
        Node instr = new InstrXD(ctx.opcode, prefix, 1, 6, z).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXD_R_Ref_II_N(InstrXD_R_Ref_II_NContext ctx) {
        //  | opcode=OPCODE_LD r=rRegister2 SEP_COMMA d=rDisplacement                     # instrXD_R_Ref_II_N
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        int y = CompilerTables.registers.get(ctx.r.start.getType());
        Node instr = new InstrXD(ctx.opcode, prefix, 1, y, 6).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXD_Ref_II_N(InstrXD_Ref_II_NContext ctx) {
        //  | opcode=OPCODE_INC d=rDisplacement                                           # instrXD_Ref_II_N
        //  | opcode=OPCODE_DEC d=rDisplacement                                           # instrXD_Ref_II_N
        //  | opcode=OPCODE_ADD REG_A SEP_COMMA d=rDisplacement                           # instrXD_Ref_II_N
        //  | opcode=OPCODE_ADC REG_A SEP_COMMA d=rDisplacement                           # instrXD_Ref_II_N
        //  | opcode=OPCODE_SBC REG_A SEP_COMMA d=rDisplacement                           # instrXD_Ref_II_N
        //  | opcode=OPCODE_SUB d=rDisplacement                                           # instrXD_Ref_II_N
        //  | opcode=OPCODE_AND d=rDisplacement                                           # instrXD_Ref_II_N
        //  | opcode=OPCODE_XOR d=rDisplacement                                           # instrXD_Ref_II_N
        //  | opcode=OPCODE_OR d=rDisplacement                                            # instrXD_Ref_II_N
        //  | opcode=OPCODE_CP d=rDisplacement                                            # instrXD_Ref_II_N
        int prefix = CompilerTables.prefix.get(ctx.d.ii.start.getType());
        if (ctx.opcode.getType() == OPCODE_INC) {
            Node instr = new InstrXD(ctx.opcode, prefix, 0, 6, 4).setSizeBytes(3);
            instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
            return instr;
        } else if (ctx.opcode.getType() == OPCODE_DEC) {
            Node instr = new InstrXD(ctx.opcode, prefix, 0, 6, 5).setSizeBytes(3);
            instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
            return instr;
        }

        int y = CompilerTables.alu.get(ctx.opcode.getType());
        Node instr = new InstrXD(ctx.opcode, prefix, 2, y, 6).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.d.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrXD_IIHL(InstrXD_IIHLContext ctx) {
        //  | opcode=OPCODE_INC ii=rII_HL                                                 # instrXD_IIHL
        //  | opcode=OPCODE_DEC ii=rII_HL                                                 # instrXD_IIHL
        //  | opcode=OPCODE_ADD REG_A SEP_COMMA ii=rII_HL                                 # instrXD_IIHL
        //  | opcode=OPCODE_ADC REG_A SEP_COMMA ii=rII_HL                                 # instrXD_IIHL
        //  | opcode=OPCODE_SBC REG_A SEP_COMMA ii=rII_HL                                 # instrXD_IIHL
        //  | opcode=OPCODE_SUB ii=rII_HL                                                 # instrXD_IIHL
        //  | opcode=OPCODE_AND ii=rII_HL                                                 # instrXD_IIHL
        //  | opcode=OPCODE_XOR ii=rII_HL                                                 # instrXD_IIHL
        //  | opcode=OPCODE_OR ii=rII_HL                                                  # instrXD_IIHL
        //  | opcode=OPCODE_CP ii=rII_HL                                                  # instrXD_IIHL
        int prefix = CompilerTables.prefix.get(ctx.ii.start.getType());
        int r = CompilerTables.registers.get(ctx.ii.start.getType());

        if (ctx.opcode.getType() == OPCODE_INC) {
            return new InstrXD(ctx.opcode, prefix, 0, r, 4).setSizeBytes(2);
        } else if (ctx.opcode.getType() == OPCODE_DEC) {
            return new InstrXD(ctx.opcode, prefix, 0, r, 5).setSizeBytes(2);
        }
        int y = CompilerTables.alu.get(ctx.opcode.getType());
        return new InstrXD(ctx.opcode, prefix, 2, y, r).setSizeBytes(2);
    }

    @Override
    public Node visitInstrXD_II(InstrXD_IIContext ctx) {
        //  | opcode=OPCODE_INC ii=rII                                                    # instrXD_II
        //  | opcode=OPCODE_DEC ii=rII                                                    # instrXD_II
        //  | opcode=OPCODE_POP ii=rII                                                    # instrXD_II
        //  | opcode=OPCODE_JP SEP_LPAR ii=rII SEP_RPAR                                   # instrXD_II
        //  | opcode=OPCODE_LD REG_SP SEP_COMMA ii=rII                                    # instrXD_II
        //  | opcode=OPCODE_EX SEP_LPAR REG_SP SEP_RPAR SEP_COMMA ii=rII                  # instrXD_II
        //  | opcode=OPCODE_PUSH ii=rII                                                   # instrXD_II
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

        return new InstrXD(ctx.opcode, prefix, x, q, p, z).setSizeBytes(2);
    }

    @Override
    public Node visitInstrRef_NN_R(InstrRef_NN_RContext ctx) {
        //  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA r=REG_HL        # instrRef_NN_R // x=0, z=2, q=0, p=2
        //  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA r=REG_A         # instrRef_NN_R // x=0, z=2, q=0, p=3
        int p = (ctx.r.getType() == REG_HL) ? 2 : 3;
        Node instr = new Instr(ctx.opcode, 0, 0, p, 2).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrRP_Ref_NN(InstrRP_Ref_NNContext ctx) {
        //  | opcode=OPCODE_LD rp=REG_HL SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR       # instrRP_Ref_NN // x=0, z=2, q=1, p=2
        Node instr = new Instr(ctx.opcode, 0, 1, 2, 2).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrR_Ref_NN(InstrR_Ref_NNContext ctx) {
        //  | opcode=OPCODE_LD r=REG_A SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR         # instrR_Ref_NN  // x=0, z=2, q=1, p=3
        Node instr = new Instr(ctx.opcode, 0, 1, 3, 2).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrA_Ref_RP(InstrA_Ref_RPContext ctx) {
        //  | opcode=OPCODE_LD REG_A SEP_COMMA SEP_LPAR rp=(REG_BC|REG_DE) SEP_RPAR       # instrA_Ref_RP // x=0, z=2, q=1, p=rp
        int p = CompilerTables.regPairs.get(ctx.rp.getType());
        return new Instr(ctx.opcode, 0, 1, p, 2).setSizeBytes(1);
    }

    @Override
    public Node visitInstrRef_RP(InstrRef_RPContext ctx) {
        //  | opcode=OPCODE_LD SEP_LPAR rp=(REG_BC|REG_DE) SEP_RPAR SEP_COMMA REG_A       # instrRef_RP   // x=0, z=2, q=0, p=rp
        //  | opcode=OPCODE_JP SEP_LPAR rp=REG_HL SEP_RPAR                                # instrRef_RP   // x=3, z=1, q=1, p=2
        int p = CompilerTables.regPairs.get(ctx.rp.getType());
        int x = (ctx.opcode.getType() == OPCODE_LD) ? 0 : 3;
        int z = (ctx.opcode.getType() == OPCODE_LD) ? 2 : 1;
        int q = (ctx.opcode.getType() == OPCODE_LD) ? 0 : 1;
        return new Instr(ctx.opcode, x, q, p, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrRef_RP_RP(InstrRef_RP_RPContext ctx) {
        //  | opcode=OPCODE_EX SEP_LPAR dst=REG_SP SEP_RPAR SEP_COMMA src=REG_HL          # instrRef_RP_RP // x=3, z=3, y=4
        return new Instr(ctx.opcode, 3, 4, 3).setSizeBytes(1);
    }

    @Override
    public Node visitInstrR_R(InstrR_RContext ctx) {
        //  | opcode=OPCODE_LD dst=rRegister SEP_COMMA src=rRegister                      # instrR_R      // x=1, y=dst, z=src
        int y = CompilerTables.registers.get(ctx.dst.r.getType());
        int z = CompilerTables.registers.get(ctx.src.r.getType());
        return new Instr(ctx.opcode, 1, y, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrR_N(InstrR_NContext ctx) {
        //  | opcode=OPCODE_LD r=rRegister SEP_COMMA n=rExpression                        # instrR_N      // x=0, z=6, y=r
        int y = CompilerTables.registers.get(ctx.r.r.getType());
        Node instr = new Instr(ctx.opcode, 0, y, 6).setSizeBytes(2);
        instr.addChild(CreateVisitors.expr.visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrC_N(InstrC_NContext ctx) {
        //  | opcode=OPCODE_JR c=(COND_NZ|COND_Z|COND_NC|COND_C) SEP_COMMA n=rExpression  # instrC_N    // x=0, z=0, y=4..7
        int y = CompilerTables.conditions.get(ctx.c.getType());
        Node instr = new Instr(ctx.opcode, 0, y, 0).setSizeBytes(2);
        instr.addChild(CreateVisitors.expr.visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrC(InstrCContext ctx) {
        //  | opcode=OPCODE_RET c=cCondition                                              # instrC        // x=3, z=0, y=cc
        int y = CompilerTables.conditions.get(ctx.c.start.getType());
        return new Instr(ctx.opcode, 3, y, 0).setSizeBytes(1);
    }

    @Override
    public Node visitInstrC_NN(InstrC_NNContext ctx) {
        //  | opcode=OPCODE_JP c=cCondition SEP_COMMA nn=rExpression                      # instrC_NN     // x=3, z=2, y=cc
        //  | opcode=OPCODE_CALL c=cCondition SEP_COMMA nn=rExpression                    # instrC_NN     // x=3, z=4, y=cc
        int y = CompilerTables.conditions.get(ctx.c.start.getType());
        int z = (ctx.opcode.getType() == OPCODE_JP) ? 2 : 4;
        Node instr = new Instr(ctx.opcode, 3, y, z).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrRP_NN(InstrRP_NNContext ctx) {
        //  | opcode=OPCODE_LD rp=rRegPair SEP_COMMA nn=rExpression                       # instrRP_NN  // x=0, z=1, q=0, p=rp
        int p = CompilerTables.regPairs.get(ctx.rp.start.getType());
        Node instr = new Instr(ctx.opcode, 0, 0, p, 1).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrRP_RP(InstrRP_RPContext ctx) {
        //  | opcode=OPCODE_EX dst=REG_AF SEP_COMMA src=REG_AFF                           # instrRP_RP  // x=0, z=0, q=1, p=0
        //  | opcode=OPCODE_LD dst=REG_SP SEP_COMMA src=REG_HL                            # instrRP_RP    // x=3, z=1, q=1, p=3
        //  | opcode=OPCODE_EX dst=REG_DE SEP_COMMA src=REG_HL                            # instrRP_RP    // x=3, z=3, q=1, p=2
        int x = (ctx.opcode.getType() == OPCODE_EX && ctx.dst.getType() == REG_AF) ? 0 : 3;
        int z = (ctx.opcode.getType() == OPCODE_LD) ? 1 : ((ctx.dst.getType() == REG_AF) ? 0 : 3);
        int p = (ctx.opcode.getType() == OPCODE_LD) ? 3 : ((ctx.dst.getType() == REG_AF) ? 0 : 2);
        return new Instr(ctx.opcode, x, 1, p, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrNN(InstrNNContext ctx) {
        //  | opcode=OPCODE_JP nn=rExpression                                             # instrNN       // x=3, z=3, y=0
        //  | opcode=OPCODE_CALL nn=rExpression                                           # instrNN       // x=3, z=5, y=1
        int z = (ctx.opcode.getType() == OPCODE_JP) ? 3 : 5;
        int y = (ctx.opcode.getType() == OPCODE_JP) ? 0 : 1;
        Node instr = new Instr(ctx.opcode, 3, y, z).setSizeBytes(3);
        instr.addChild(CreateVisitors.expr.visit(ctx.nn).setSizeBytes(2));
        return instr;
    }

    @Override
    public Node visitInstrN(InstrNContext ctx) {
        //  | opcode=OPCODE_DJNZ n=rExpression                                            # instrN      // x=0, z=0, y=2
        //  | opcode=OPCODE_JR n=rExpression                                              # instrN      // x=0, z=0, y=3
        //  | opcode=OPCODE_OUT SEP_LPAR n=rExpression SEP_RPAR SEP_COMMA REG_A           # instrN      // x=3, z=3, y=2
        //  | opcode=OPCODE_IN REG_A SEP_COMMA SEP_LPAR n=rExpression SEP_RPAR            # instrN      // x=3, z=3, y=3
        //  | opcode=OPCODE_ADD REG_A SEP_COMMA n=rExpression                             # instrN      // x=3, z=6, y=alu
        //  | opcode=OPCODE_ADC REG_A SEP_COMMA n=rExpression                             # instrN      // x=3, z=6, y=alu
        //  | opcode=OPCODE_SUB n=rExpression                                             # instrN      // x=3, z=6, y=alu
        //  | opcode=OPCODE_SBC REG_A SEP_COMMA n=rExpression                             # instrN      // x=3, z=6, y=alu
        //  | opcode=OPCODE_AND n=rExpression                                             # instrN      // x=3, z=6, y=alu
        //  | opcode=OPCODE_XOR n=rExpression                                             # instrN      // x=3, z=6, y=alu
        //  | opcode=OPCODE_OR n=rExpression                                              # instrN      // x=3, z=6, y=alu
        //  | opcode=OPCODE_CP n=rExpression                                              # instrN      // x=3, z=6, y=alu
        //  | opcode=OPCODE_RST n=rExpression                                             # instrN      // x=3, z=7, y=N/8
        int opcode = ctx.opcode.getType();

        boolean djnzOrJr = opcode == OPCODE_DJNZ || opcode == OPCODE_JR;
        boolean djnzOrOut = opcode == OPCODE_DJNZ || opcode == OPCODE_OUT;
        boolean rst = opcode == OPCODE_RST;

        int x = (djnzOrJr) ? 0 : 3;
        int z = (djnzOrJr) ? 0 : ((opcode == OPCODE_OUT || opcode == OPCODE_IN) ? 3 : (rst ? 7 : 6));
        int y = djnzOrOut ? 2 : ((opcode == OPCODE_JR || opcode == OPCODE_IN) ? 3 : (rst ? 0 : CompilerTables.alu.get(opcode)));

        Node instr = new Instr(ctx.opcode, x, y, z).setSizeBytes(rst ? 1 : 2);
        instr.addChild(CreateVisitors.expr.visit(ctx.n).setSizeBytes(1));
        return instr;
    }

    @Override
    public Node visitInstrRP(InstrRPContext ctx) {
        //  | opcode=OPCODE_ADD REG_HL SEP_COMMA rp=rRegPair                              # instrRP     // x=0, z=1, q=1, p=rp
        //  | opcode=OPCODE_INC rp=rRegPair                                               # instrRP       // x=0, z=3, q=0, p=rp
        //  | opcode=OPCODE_DEC rp=rRegPair                                               # instrRP       // x=0, z=3, q=1, p=rp
        int q = (ctx.opcode.getType() == OPCODE_INC) ? 0 : 1;
        int p = CompilerTables.regPairs.get(ctx.rp.start.getType());
        int z = (ctx.opcode.getType() == OPCODE_ADD) ? 1 : 3;
        return new Instr(ctx.opcode, 0, q, p, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrRP2(InstrRP2Context ctx) {
        //  | opcode=OPCODE_POP rp2=rRegPair2                                             # instrRP2      // x=3, z=1, q=0, p=rp2
        //  | opcode=OPCODE_PUSH rp2=rRegPair2                                            # instrRP2      // x=3, z=5, q=0, p=rp2
        int p = CompilerTables.regPairs2.get(ctx.rp2.start.getType());
        int z = (ctx.opcode.getType() == OPCODE_POP) ? 1 : 5;
        return new Instr(ctx.opcode, 3, 0, p, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstrR(InstrRContext ctx) {
        //  | opcode=OPCODE_ADD REG_A SEP_COMMA r=rRegister                               # instrR        // x=2, y=alu, z=r
        //  | opcode=OPCODE_ADC REG_A SEP_COMMA r=rRegister                               # instrR        // x=2, y=alu, z=r
        //  | opcode=OPCODE_SUB r=rRegister                                               # instrR        // x=2, y=alu, z=r
        //  | opcode=OPCODE_SBC REG_A SEP_COMMA r=rRegister                               # instrR        // x=2, y=alu, z=r
        //  | opcode=OPCODE_AND r=rRegister                                               # instrR        // x=2, y=alu, z=r
        //  | opcode=OPCODE_XOR r=rRegister                                               # instrR        // x=2, y=alu, z=r
        //  | opcode=OPCODE_OR r=rRegister                                                # instrR        // x=2, y=alu, z=r
        //  | opcode=OPCODE_CP r=rRegister                                                # instrR        // x=2, y=alu, z=r
        //  | opcode=OPCODE_INC r=rRegister                                               # instrR        // x=0, z=4, y=r
        //  | opcode=OPCODE_DEC r=rRegister                                               # instrR        // x=0, z=5, y=r
        int opcode = ctx.opcode.getType();
        boolean incDec = (opcode == OPCODE_INC || opcode == OPCODE_DEC);
        int reg = CompilerTables.registers.get(ctx.r.r.getType());

        int x = incDec ? 0 : 2;
        int y = incDec ? reg : CompilerTables.alu.get(ctx.opcode.getType());
        int z = (opcode == OPCODE_INC) ? 4 : ((opcode == OPCODE_DEC) ? 5 : reg);
        return new Instr(ctx.opcode, x, y, z).setSizeBytes(1);
    }

    @Override
    public Node visitInstr(InstrContext ctx) {
        //  | opcode=OPCODE_NOP                                                           # instr         // x=0, z=0, y=0
        //  | opcode=OPCODE_RLCA                                                          # instr         // x=0, z=7, y=0
        //  | opcode=OPCODE_RRCA                                                          # instr         // x=0, z=7, y=1
        //  | opcode=OPCODE_RLA                                                           # instr         // x=0, z=7, y=2
        //  | opcode=OPCODE_RRA                                                           # instr         // x=0, z=7, y=3
        //  | opcode=OPCODE_DAA                                                           # instr         // x=0, z=7, y=4
        //  | opcode=OPCODE_CPL                                                           # instr         // x=0, z=7, y=5
        //  | opcode=OPCODE_SCF                                                           # instr         // x=0, z=7, y=6
        //  | opcode=OPCODE_CCF                                                           # instr         // x=0, z=7, y=7
        //  | opcode=OPCODE_HALT                                                          # instr         // x=1, z=6, y=6
        //  | opcode=OPCODE_RET                                                           # instr         // x=3, z=1, q=1, p=0
        //  | opcode=OPCODE_EXX                                                           # instr         // x=3, z=1, q=1, p=1
        //  | opcode=OPCODE_DI                                                            # instr         // x=3, z=3, y=6
        //  | opcode=OPCODE_EI                                                            # instr         // x=3, z=3, y=7

        Map<Integer, int[]> xyz = new HashMap<>();
        xyz.put(OPCODE_NOP, new int[] {0, 0, 0});
        xyz.put(OPCODE_RLCA, new int[] {0, 0, 7});
        xyz.put(OPCODE_RRCA, new int[] {0, 1, 7});
        xyz.put(OPCODE_RLA, new int[] {0, 2, 7});
        xyz.put(OPCODE_RRA, new int[] {0, 3, 7});
        xyz.put(OPCODE_DAA, new int[] {0, 4, 7});
        xyz.put(OPCODE_CPL, new int[] {0, 5, 7});
        xyz.put(OPCODE_SCF, new int[] {0, 6, 7});
        xyz.put(OPCODE_CCF, new int[] {0, 7, 7});
        xyz.put(OPCODE_HALT, new int[] {1, 6, 6});
        xyz.put(OPCODE_RET, new int[] {3, 1, 1});
        xyz.put(OPCODE_EXX, new int[] {3, 3, 1});
        xyz.put(OPCODE_DI, new int[] {3, 6, 3});
        xyz.put(OPCODE_EI, new int[] {3, 7, 3});

        int[] xyzValues = xyz.get(ctx.opcode.getType());
        return new Instr(ctx.opcode, xyzValues[0], xyzValues[1], xyzValues[2]).setSizeBytes(1);
    }
}
