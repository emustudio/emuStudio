package net.emustudio.plugins.compiler.asZ80.parser;

import net.emustudio.plugins.compiler.asZ80.CompilerTables;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprCurrentAddress;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprId;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoLabel;
import org.junit.Test;

import java.util.Random;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.Utils.*;

public class ParseInstrTest {

    @Test
    public void testInstrNoArgs() {
        assertInstr("nop", OPCODE_NOP, 0, 0, 0);
        assertInstr("ex af, af'", OPCODE_EX, 0, 1, 0);
        assertInstr("ld (bc), a", OPCODE_LD, 0, 0, 2);
        assertInstr("ld (de), a", OPCODE_LD, 0, 2, 2);
        assertInstr("ld a, (bc)", OPCODE_LD, 0, 1, 2);
        assertInstr("ld a, (de)", OPCODE_LD, 0, 3, 2);
        assertInstr("rlca", OPCODE_RLCA, 0, 0, 7);
        assertInstr("rrca", OPCODE_RRCA, 0, 1, 7);
        assertInstr("rla", OPCODE_RLA, 0, 2, 7);
        assertInstr("rra", OPCODE_RRA, 0, 3, 7);
        assertInstr("daa", OPCODE_DAA, 0, 4, 7);
        assertInstr("cpl", OPCODE_CPL, 0, 5, 7);
        assertInstr("scf", OPCODE_SCF, 0, 6, 7);
        assertInstr("ccf", OPCODE_CCF, 0, 7, 7);
        assertInstr("halt", OPCODE_HALT, 1, 6, 6);
        assertInstr("ret", OPCODE_RET, 3, 1, 1);
        assertInstr("ret po", OPCODE_RET, 3, 4, 0);
        assertInstr("ret pe", OPCODE_RET, 3, 5, 0);
        assertInstr("ret p", OPCODE_RET, 3, 6, 0);
        assertInstr("exx", OPCODE_EXX, 3, 3, 1);
        assertInstr("jp hl", OPCODE_JP, 3, 5, 1);
        assertInstr("jp (hl)", OPCODE_JP, 3, 5, 1);
        assertInstr("ld sp, hl", OPCODE_LD, 3, 7, 1);
        assertInstr("ex (sp), hl", OPCODE_EX, 3, 4, 3);
        assertInstr("ex de, hl", OPCODE_EX, 3, 5, 3);
        assertInstr("di", OPCODE_DI, 3, 6, 3);
        assertInstr("ei", OPCODE_EI, 3, 7, 3);
        assertInstrED("in (c)", OPCODE_IN, 6, 0);
        assertInstrED("out (c), 0", OPCODE_OUT, 6, 1);
        assertInstrED("neg", OPCODE_NEG, 0, 4);
        assertInstrED("retn", OPCODE_RETN, 0, 5);
        assertInstrED("reti", OPCODE_RETI, 1, 5);
        assertInstrED("im 0", OPCODE_IM, 0, 6);
        assertInstrED("im 0/1", OPCODE_IM, 1, 6);
        assertInstrED("im 1", OPCODE_IM, 2, 6);
        assertInstrED("im 2", OPCODE_IM, 3, 6);
        assertInstrED("ld i, a", OPCODE_LD, 0, 7);
        assertInstrED("ld r, a", OPCODE_LD, 1, 7);
        assertInstrED("ld a, i", OPCODE_LD, 2, 7);
        assertInstrED("ld a, r", OPCODE_LD, 3, 7);
        assertInstrED("rrd", OPCODE_RRD, 4, 7);
        assertInstrED("rld", OPCODE_RLD, 5, 7);
        assertInstrED("ldi", OPCODE_LDI, 4, 0);
        assertInstrED("ldd", OPCODE_LDD, 5, 0);
        assertInstrED("ldir", OPCODE_LDIR, 6, 0);
        assertInstrED("lddr", OPCODE_LDDR, 7, 0);
        assertInstrED("cpi", OPCODE_CPI, 4, 1);
        assertInstrED("cpd", OPCODE_CPD, 5, 1);
        assertInstrED("cpir", OPCODE_CPIR, 6, 1);
        assertInstrED("cpdr", OPCODE_CPDR, 7, 1);
        assertInstrED("ini", OPCODE_INI, 4, 2);
        assertInstrED("ind", OPCODE_IND, 5, 2);
        assertInstrED("inir", OPCODE_INIR, 6, 2);
        assertInstrED("indr", OPCODE_INDR, 7, 2);
        assertInstrED("outi", OPCODE_OUTI, 4, 3);
        assertInstrED("outd", OPCODE_OUTD, 5, 3);
        assertInstrED("otir", OPCODE_OTIR, 6, 3);
        assertInstrED("otdr", OPCODE_OTDR, 7, 3);
    }

    @Test
    public void testInstrReg() {
        Random random = new Random();
        forRegister(regValue -> {
            int r = CompilerTables.registers.get(regValue.r);
            assertInstr("inc " + regValue.l, OPCODE_INC, 0, r, 4);
            assertInstr("dec " + regValue.l, OPCODE_DEC, 0, r, 5);
            assertInstrExpr("ld " + regValue.l + ", ", OPCODE_LD, 0, r, 6);
            assertInstr("add a, " + regValue.l, OPCODE_ADD, 2, 0, r);
            assertInstr("adc a, " + regValue.l, OPCODE_ADC, 2, 1, r);
            assertInstr("sub " + regValue.l, OPCODE_SUB, 2, 2, r);
            assertInstr("sbc a, " + regValue.l, OPCODE_SBC, 2, 3, r);
            assertInstr("and " + regValue.l, OPCODE_AND, 2, 4, r);
            assertInstr("xor " + regValue.l, OPCODE_XOR, 2, 5, r);
            assertInstr("or " + regValue.l, OPCODE_OR, 2, 6, r);
            assertInstr("cp " + regValue.l, OPCODE_CP, 2, 7, r);

            assertInstrCB("rlc " + regValue.l, OPCODE_RLC, 0, r);
            assertInstrCB("rrc " + regValue.l, OPCODE_RRC, 1, r);
            assertInstrCB("rl " + regValue.l, OPCODE_RL, 2, r);
            assertInstrCB("rr " + regValue.l, OPCODE_RR, 3, r);
            assertInstrCB("sla " + regValue.l, OPCODE_SLA, 4, r);
            assertInstrCB("sra " + regValue.l, OPCODE_SRA, 5, r);
            assertInstrCB("sll " + regValue.l, OPCODE_SLL, 6, r);
            assertInstrCB("srl " + regValue.l, OPCODE_SRL, 7, r);

            int bit = Math.abs(random.nextInt() % 8);
            assertInstrCBExprBit("bit " + bit + ", " + regValue.l, OPCODE_BIT, bit, r);
            assertInstrCBExprBit("res " + bit + ", " + regValue.l, OPCODE_RES, bit, r);
            assertInstrCBExprBit("set " + bit + ", " + regValue.l, OPCODE_SET, bit, r);

            if (r != 6) {
                assertInstrED("in " + regValue.l + ", (c)", OPCODE_IN, r, 0);
                assertInstrED("out (c), " + regValue.l, OPCODE_OUT, r, 1);
            }

            forRegister(regValue2 -> {
                int r2 = CompilerTables.registers.get(regValue2.r);
                assertInstr("ld " + regValue.l + ", " + regValue2.l, OPCODE_LD, 1, r, r2);
            });
        });
    }

    @Test
    public void testInstrRP() {
        forRegPair(regPair -> {
            int rp = CompilerTables.regPairs.get(regPair.r);

            assertInstrExpr("ld " + regPair.l + ", ", OPCODE_LD, 0, rp, 0, 1);
            assertInstr("add hl, " + regPair.l, OPCODE_ADD, 0, (rp << 1) | 1, 1);
            assertInstr("inc " + regPair.l, OPCODE_INC, 0, rp, 0, 3);
            assertInstr("dec " + regPair.l, OPCODE_DEC, 0, rp, 1, 3);
            assertInstrED("sbc hl, " + regPair.l, OPCODE_SBC, rp << 1, 2);
            assertInstrED("adc hl, " + regPair.l, OPCODE_ADC, (rp << 1) | 1, 2);
            if (rp != 2) {
                // HL
                assertInstrEDExpr("ld (", "), " + regPair.l, rp << 1);
                assertInstrEDExpr("ld " + regPair.l + ", (", ")", (rp << 1) | 1);
            }
        });
    }

    @Test
    public void testInstrRP2() {
        forRegPair2(regPair2 -> {
            int rp = CompilerTables.regPairs2.get(regPair2.r);
            assertInstr("pop " + regPair2.l, OPCODE_POP, 3, rp, 0, 1);
            assertInstr("push " + regPair2.l, OPCODE_PUSH, 3, rp, 0, 5);
        });
    }

    @Test
    public void testInstrExpr() {
        assertInstrExpr("ld (", "), hl", OPCODE_LD, 0, 4, 2);
        assertInstrExpr("ld (", "), a", OPCODE_LD, 0, 6, 2);
        assertInstrExpr("ld hl, (", ")", OPCODE_LD, 0, 5, 2);
        assertInstrExpr("ld a, (", ")", OPCODE_LD, 0, 7, 2);
        assertInstrExpr("jp ", OPCODE_JP, 3, 0, 3);
        assertInstrExpr("out (", "), a", OPCODE_OUT, 3, 2, 3);
        assertInstrExpr("in a, (", ")", OPCODE_IN, 3, 3, 3);
        assertInstrExpr("call ", OPCODE_CALL, 3, 0, 1, 5);
        assertInstrExpr("call po,", OPCODE_CALL, 3, 4, 4);
        assertInstrExpr("call pe,", OPCODE_CALL, 3, 5, 4);
        assertInstrExpr("call p,", OPCODE_CALL, 3, 6, 4);
        assertInstrExpr("add a,", OPCODE_ADD, 3, 0, 6);
        assertInstrExpr("adc a,", OPCODE_ADC, 3, 1, 6);
        assertInstrExpr("sub", OPCODE_SUB, 3, 2, 6);
        assertInstrExpr("sbc a,", OPCODE_SBC, 3, 3, 6);
        assertInstrExpr("and", OPCODE_AND, 3, 4, 6);
        assertInstrExpr("xor", OPCODE_XOR, 3, 5, 6);
        assertInstrExpr("or", OPCODE_OR, 3, 6, 6);
        assertInstrExpr("cp", OPCODE_CP, 3, 7, 6);
        assertInstrExpr("rst", OPCODE_RST, 3, 0, 7);
    }

    @Test
    public void testCallLabelWithConditionPrefix() {
        Program program = parseProgram("peter: call peter");
        assertTrees(new Program()
                .addChild(new PseudoLabel(0, 0, "peter")
                    .addChild(new Instr(0, 0, OPCODE_CALL, 3, 1, 5)
                        .addChild(new ExprId(0, 0, "peter")))),
            program
        );
    }

    @Test
    public void testInstrXDCB() {
        Random random = new Random();
        forRegister(register -> {
            int z = CompilerTables.registers.get(register.r);
            forRot(rot -> {
                int y = CompilerTables.rot.get(rot.r);
                if (z != 6) {
                    assertInstrXDCBExpr(rot.l + " (ix + ", "), " + register.l, rot.r, 0xDD, y, z);
                    assertInstrXDCBExpr(rot.l + " (iy + ", "), " + register.l, rot.r, 0xFD, y, z);
                }
            });

            int y = Math.abs(random.nextInt() % 8);
            if (z != 6) {
                assertInstrXDCBExprBit("res " + y + ", (ix + ", "), " + register.l, OPCODE_RES, 0xDD, y, z);
                assertInstrXDCBExprBit("res " + y + ", (iy + ", "), " + register.l, OPCODE_RES, 0xFD, y, z);
                assertInstrXDCBExprBit("set " + y + ", (ix + ", "), " + register.l, OPCODE_SET, 0xDD, y, z);
                assertInstrXDCBExprBit("set " + y + ", (iy + ", "), " + register.l, OPCODE_SET, 0xFD, y, z);
            }
        });

        forRot(rot -> {
            int y = CompilerTables.rot.get(rot.r);
            assertInstrXDCBExpr(rot.l + " (ix + ", ")", rot.r, 0xDD, y, 6);
            assertInstrXDCBExpr(rot.l + " (iy + ", ")", rot.r, 0xFD, y, 6);
        });

        int y = Math.abs(random.nextInt() % 8);
        assertInstrXDCBExprBit("bit " + y + ", (ix + ", ")", OPCODE_BIT, 0xDD, y, 6);
        assertInstrXDCBExprBit("bit " + y + ", (iy + ", ")", OPCODE_BIT, 0xFD, y, 6);
        assertInstrXDCBExprBit("res " + y + ", (ix + ", ")", OPCODE_RES, 0xDD, y, 6);
        assertInstrXDCBExprBit("res " + y + ", (iy + ", ")", OPCODE_RES, 0xFD, y, 6);
        assertInstrXDCBExprBit("set " + y + ", (ix + ", ")", OPCODE_SET, 0xDD, y, 6);
        assertInstrXDCBExprBit("set " + y + ", (iy + ", ")", OPCODE_SET, 0xFD, y, 6);
    }

    @Test
    public void testInstrXD() {
        forPrefixReg(ii -> {
            int prefix = CompilerTables.prefix.get(ii.r);
            forRegPair(regPair -> {
                int rp = CompilerTables.regPairs.get(regPair.r);
                if (regPair.r == 2) {
                    assertInstrXD("add " + ii.l + ", " + ii.l, OPCODE_ADD, prefix, 0, rp, 1, 1);
                }
            });

            forRegister(register -> {
                int r = CompilerTables.registers.get(register.r);
                if (r != 6) {
                    assertInstrXDExpr("ld (" + ii.l + " +", "), " + register.l, OPCODE_LD, prefix, 1, 6, r);
                    assertInstrXDExpr("ld " + register.l + ", (" + ii.l + " +", ")", OPCODE_LD, prefix, 1, r, 6);
                }
            });

            assertInstrXDExpr("ld " + ii.l + ", ", "", OPCODE_LD, prefix, 0, 4, 1);
            assertInstrXDExpr("ld (", "), " + ii.l, OPCODE_LD, prefix, 0, 4, 2);
            assertInstrXDExpr("inc (" + ii.l + "+", ")", OPCODE_INC, prefix, 0, 6, 4);
            assertInstrXDExpr("dec (" + ii.l + "+", ")", OPCODE_DEC, prefix, 0, 6, 5);
            assertInstrXD("inc " + ii.l, OPCODE_INC, prefix, 0, 4, 3);
            assertInstrXDExpr("ld " + ii.l + ", (", ")", OPCODE_LD, prefix, 0, 5, 2);
            assertInstrXD("dec " + ii.l, OPCODE_DEC, prefix, 0, 5, 3);
            assertInstrXD("pop " + ii.l, OPCODE_POP, prefix, 3, 4, 1);
            assertInstrXD("push " + ii.l, OPCODE_PUSH, prefix, 3, 4, 5);
            assertInstrXD("ex (sp), " + ii.l, OPCODE_EX, prefix, 3, 4, 3);
            assertInstrXD("jp (" + ii.l + ")", OPCODE_JP, prefix, 3, 5, 1);
            assertInstrXD("ld sp, " + ii.l, OPCODE_LD, prefix, 3, 7, 1);
            assertInstrXDExprExpr("ld (" + ii.l + "+", prefix);
        });

        forPrefixReg8(ii -> {
            int prefix = CompilerTables.prefix.get(ii.r);
            int r = CompilerTables.registers.get(ii.r);

            assertInstrXD("inc " + ii.l, OPCODE_INC, prefix, 0, r, 4);
            assertInstrXD("dec " + ii.l, OPCODE_DEC, prefix, 0, r, 5);
            assertInstrXDExpr("ld " + ii.l + ", ", "", OPCODE_LD, prefix, 0, r, 6);

            assertInstrXD("add a, " + ii.l, OPCODE_ADD, prefix, 2, 0, r);
            assertInstrXD("adc a, " + ii.l, OPCODE_ADC, prefix, 2, 1, r);
            assertInstrXD("sub " + ii.l, OPCODE_SUB, prefix, 2, 2, r);
            assertInstrXD("sbc a, " + ii.l, OPCODE_SBC, prefix, 2, 3, r);
            assertInstrXD("and " + ii.l, OPCODE_AND, prefix, 2, 4, r);
            assertInstrXD("xor " + ii.l, OPCODE_XOR, prefix, 2, 5, r);
            assertInstrXD("or " + ii.l, OPCODE_OR, prefix, 2, 6, r);
            assertInstrXD("cp " + ii.l, OPCODE_CP, prefix, 2, 7, r);

            forRegister(register -> {
                int r2 = CompilerTables.registers.get(register.r);
                String r2s = (register.r == REG_H) ? (prefix == 0xDD ? "ixh" : "iyh") : ((register.r == REG_L) ? (prefix == 0xDD ? "ixl" : "iyl") : register.l);

                if (r2 != 6) {
                    assertInstrXD("ld " + ii.l + ", " + r2s, OPCODE_LD, prefix, 1, r, r2);
                    assertInstrXD("ld " + r2s + ", " + ii.l, OPCODE_LD, prefix, 1, r2, r);
                }
            });

        });
    }


    private void assertInstr(String instr, int instrType, int x, int y, int z) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new Instr(0, 0, instrType, x, y, z)), program);
        });
    }

    private void assertInstr(String instr, int instrType, int x, int p, int q, int z) {
        assertInstr(instr, instrType, x, (p << 1) | q, z);
    }

    private void assertInstrED(String instr, int instrType, int y, int z) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrED(0, 0, instrType, y, z)), program);
        });
    }

    private void assertInstrEDExpr(String instrPrefix, String instrPostfix, int y) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, prefixVariation -> forStringCaseVariations(instrPostfix, postfixVariation -> {
            Program program = parseProgram(prefixVariation + " $ + 5" + postfixVariation);
            assertTrees(new Program().addChild(new InstrED(0, 0, OPCODE_LD, y, 3).addChild(expr)), program);
        }));
    }

    private void assertInstrCB(String instr, int instrType, int y, int z) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrCB(0, 0, instrType, y, z)), program);
        });
    }

    private void assertInstrExpr(String instrPrefix, String instrPostfix, int instrType, int x, int y, int z) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, prefixVariation -> forStringCaseVariations(instrPostfix, postfixVariation -> {
            Program program = parseProgram(prefixVariation + " $ + 5" + postfixVariation);
            assertTrees(new Program().addChild(new Instr(0, 0, instrType, x, y, z).addChild(expr)), program);
        }));
    }

    private void assertInstrExpr(String instr, int instrType, int x, int y, int z) {
        assertInstrExpr(instr, "", instrType, x, y, z);
    }

    private void assertInstrExpr(String instr, int instrType, int x, int p, int q, int z) {
        assertInstrExpr(instr, instrType, x, (p << 1) | q, z);
    }

    private void assertInstrCBExprBit(String instr, int instrType, int y, int z) {
        Node expr = new ExprNumber(0, 0, y);

        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrCB(0, 0, instrType, 0, z).addChild(expr)), program);
        });
    }

    private void assertInstrXDCBExpr(String instrPrefix, String instrPostfix, int instrType, int prefix, int y, int z) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, instrPrefixVariation -> forStringCaseVariations(instrPostfix, instrPostfixVariation -> {
            Program program = parseProgram(instrPrefixVariation + " $ + 5" + instrPostfixVariation);
            assertTrees(new Program().addChild(new InstrXDCB(0, 0, instrType, prefix, y, z).addChild(expr)), program);
        }));
    }

    private void assertInstrXDCBExprBit(String instrPrefix, String instrPostfix, int instrType, int prefix, int y, int z) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        Node yExpr = new ExprNumber(0, 0, y);

        forStringCaseVariations(instrPrefix, instrPrefixVariation -> forStringCaseVariations(instrPostfix, instrPostfixVariation -> {
            Program program = parseProgram(instrPrefixVariation + " $ + 5" + instrPostfixVariation);
            assertTrees(new Program()
                .addChild(new InstrXDCB(0, 0, instrType, prefix, 0, z)
                    .addChild(yExpr)
                    .addChild(expr)), program);
        }));
    }

    private void assertInstrXD(String instr, int instrType, int prefix, int x, int y, int z) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrXD(0, 0, instrType, prefix, x, y, z)), program);
        });
    }

    private void assertInstrXD(String instr, int instrType, int prefix, int x, int p, int q, int z) {
        assertInstrXD(instr, instrType, prefix, x, (p << 1) | q, z);
    }

    private void assertInstrXDExpr(String instrPrefix, String instrPostfix, int instrType, int prefix, int x, int y, int z) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, prefixVariation -> {
            forStringCaseVariations(instrPostfix, postfixVariation -> {
                Program program = parseProgram(prefixVariation + " $ + 5" + postfixVariation);
                assertTrees(new Program().addChild(new InstrXD(0, 0, instrType, prefix, x, y, z).addChild(expr)), program);
            });
        });
    }

    private void assertInstrXDExprExpr(String instrPrefix, int prefix) {
        Node disp = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        Node expr = new ExprNumber(0, 0, 5);

        forStringCaseVariations(instrPrefix, prefixVariation -> {
            Program program = parseProgram(prefixVariation + " $ + 5), 5");
            assertTrees(new Program()
                .addChild(new InstrXD(0, 0, OPCODE_LD, prefix, 0, 6, 6)
                    .addChild(disp)
                    .addChild(expr)), program);
        });
    }
}
