/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

import java.util.Optional;
import java.util.Random;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.Utils.*;
import static org.junit.Assert.assertEquals;

public class ParseInstrTest {

    @Test
    public void testInstrNoArgs() {
        assertInstr("nop", OPCODE_NOP, 0, 0, 0, 1);
        assertInstr("ex af, af'", OPCODE_EX, 0, 1, 0, 1);
        assertInstr("ld (bc), a", OPCODE_LD, 0, 0, 2, 1);
        assertInstr("ld (de), a", OPCODE_LD, 0, 2, 2, 1);
        assertInstr("ld a, (bc)", OPCODE_LD, 0, 1, 2, 1);
        assertInstr("ld a, (de)", OPCODE_LD, 0, 3, 2, 1);
        assertInstr("rlca", OPCODE_RLCA, 0, 0, 7, 1);
        assertInstr("rrca", OPCODE_RRCA, 0, 1, 7, 1);
        assertInstr("rla", OPCODE_RLA, 0, 2, 7, 1);
        assertInstr("rra", OPCODE_RRA, 0, 3, 7, 1);
        assertInstr("daa", OPCODE_DAA, 0, 4, 7, 1);
        assertInstr("cpl", OPCODE_CPL, 0, 5, 7, 1);
        assertInstr("scf", OPCODE_SCF, 0, 6, 7, 1);
        assertInstr("ccf", OPCODE_CCF, 0, 7, 7, 1);
        assertInstr("halt", OPCODE_HALT, 1, 6, 6, 1);
        assertInstr("ret", OPCODE_RET, 3, 1, 1, 1);
        assertInstr("ret po", OPCODE_RET, 3, 4, 0, 1);
        assertInstr("ret pe", OPCODE_RET, 3, 5, 0, 1);
        assertInstr("ret p", OPCODE_RET, 3, 6, 0, 1);
        assertInstr("exx", OPCODE_EXX, 3, 3, 1, 1);
        assertInstr("jp hl", OPCODE_JP, 3, 5, 1, 1);
        assertInstr("jp (hl)", OPCODE_JP, 3, 5, 1, 1);
        assertInstr("ld sp, hl", OPCODE_LD, 3, 7, 1, 1);
        assertInstr("ex (sp), hl", OPCODE_EX, 3, 4, 3, 1);
        assertInstr("ex de, hl", OPCODE_EX, 3, 5, 3, 1);
        assertInstr("di", OPCODE_DI, 3, 6, 3, 1);
        assertInstr("ei", OPCODE_EI, 3, 7, 3, 1);
        assertInstrED("in (c)", OPCODE_IN, 6, 0, 2);
        assertInstrED("out (c), 0", OPCODE_OUT, 6, 1, 2);
        assertInstrED("neg", OPCODE_NEG, 0, 4, 2);
        assertInstrED("retn", OPCODE_RETN, 0, 5, 2);
        assertInstrED("reti", OPCODE_RETI, 1, 5, 2);
        assertInstrED("im 0", OPCODE_IM, 0, 6, 2);
        assertInstrED("im 0/1", OPCODE_IM, 1, 6, 2);
        assertInstrED("im 1", OPCODE_IM, 2, 6, 2);
        assertInstrED("im 2", OPCODE_IM, 3, 6, 2);
        assertInstrED("ld i, a", OPCODE_LD, 0, 7, 2);
        assertInstrED("ld r, a", OPCODE_LD, 1, 7, 2);
        assertInstrED("ld a, i", OPCODE_LD, 2, 7, 2);
        assertInstrED("ld a, r", OPCODE_LD, 3, 7, 2);
        assertInstrED("rrd", OPCODE_RRD, 4, 7, 2);
        assertInstrED("rld", OPCODE_RLD, 5, 7, 2);
        assertInstrED("ldi", OPCODE_LDI, 4, 0, 2);
        assertInstrED("ldd", OPCODE_LDD, 5, 0, 2);
        assertInstrED("ldir", OPCODE_LDIR, 6, 0, 2);
        assertInstrED("lddr", OPCODE_LDDR, 7, 0, 2);
        assertInstrED("cpi", OPCODE_CPI, 4, 1, 2);
        assertInstrED("cpd", OPCODE_CPD, 5, 1, 2);
        assertInstrED("cpir", OPCODE_CPIR, 6, 1, 2);
        assertInstrED("cpdr", OPCODE_CPDR, 7, 1, 2);
        assertInstrED("ini", OPCODE_INI, 4, 2, 2);
        assertInstrED("ind", OPCODE_IND, 5, 2, 2);
        assertInstrED("inir", OPCODE_INIR, 6, 2, 2);
        assertInstrED("indr", OPCODE_INDR, 7, 2, 2);
        assertInstrED("outi", OPCODE_OUTI, 4, 3, 2);
        assertInstrED("outd", OPCODE_OUTD, 5, 3, 2);
        assertInstrED("otir", OPCODE_OTIR, 6, 3, 2);
        assertInstrED("otdr", OPCODE_OTDR, 7, 3, 2);
    }

    @Test
    public void testInstrReg() {
        Random random = new Random();
        forRegister(regValue -> {
            int r = CompilerTables.registers.get(regValue.r);
            assertInstr("inc " + regValue.l, OPCODE_INC, 0, r, 4, 1);
            assertInstr("dec " + regValue.l, OPCODE_DEC, 0, r, 5, 1);
            assertInstrExpr("ld " + regValue.l + ", ", OPCODE_LD, 0, r, 6, 2);
            assertInstr("add a, " + regValue.l, OPCODE_ADD, 2, 0, r, 1);
            assertInstr("adc a, " + regValue.l, OPCODE_ADC, 2, 1, r, 1);
            assertInstr("sub " + regValue.l, OPCODE_SUB, 2, 2, r, 1);
            assertInstr("sbc a, " + regValue.l, OPCODE_SBC, 2, 3, r, 1);
            assertInstr("and " + regValue.l, OPCODE_AND, 2, 4, r, 1);
            assertInstr("xor " + regValue.l, OPCODE_XOR, 2, 5, r, 1);
            assertInstr("or " + regValue.l, OPCODE_OR, 2, 6, r, 1);
            assertInstr("cp " + regValue.l, OPCODE_CP, 2, 7, r, 1);

            assertInstrCB("rlc " + regValue.l, OPCODE_RLC, 0, r, 2);
            assertInstrCB("rrc " + regValue.l, OPCODE_RRC, 1, r, 2);
            assertInstrCB("rl " + regValue.l, OPCODE_RL, 2, r, 2);
            assertInstrCB("rr " + regValue.l, OPCODE_RR, 3, r, 2);
            assertInstrCB("sla " + regValue.l, OPCODE_SLA, 4, r, 2);
            assertInstrCB("sra " + regValue.l, OPCODE_SRA, 5, r, 2);
            assertInstrCB("sll " + regValue.l, OPCODE_SLL, 6, r, 2);
            assertInstrCB("srl " + regValue.l, OPCODE_SRL, 7, r, 2);

            int bit = Math.abs(random.nextInt() % 8);
            assertInstrCBExprBit("bit " + bit + ", " + regValue.l, OPCODE_BIT, bit, r, 2);
            assertInstrCBExprBit("res " + bit + ", " + regValue.l, OPCODE_RES, bit, r, 2);
            assertInstrCBExprBit("set " + bit + ", " + regValue.l, OPCODE_SET, bit, r, 2);

            if (r != 6) {
                assertInstrED("in " + regValue.l + ", (c)", OPCODE_IN, r, 0, 2);
                assertInstrED("out (c), " + regValue.l, OPCODE_OUT, r, 1, 2);
            }

            forRegister(regValue2 -> {
                int r2 = CompilerTables.registers.get(regValue2.r);
                assertInstr("ld " + regValue.l + ", " + regValue2.l, OPCODE_LD, 1, r, r2, 1);
            });
        });
    }

    @Test
    public void testInstrRP() {
        forRegPair(regPair -> {
            int rp = CompilerTables.regPairs.get(regPair.r);

            assertInstrExpr("ld " + regPair.l + ", ", OPCODE_LD, 0, rp, 0, 1, 3);
            assertInstr("add hl, " + regPair.l, OPCODE_ADD, 0, (rp << 1) | 1, 1, 1);
            assertInstr("inc " + regPair.l, OPCODE_INC, 0, rp, 0, 3, 1);
            assertInstr("dec " + regPair.l, OPCODE_DEC, 0, rp, 1, 3, 1);
            assertInstrED("sbc hl, " + regPair.l, OPCODE_SBC, rp << 1, 2, 2);
            assertInstrED("adc hl, " + regPair.l, OPCODE_ADC, (rp << 1) | 1, 2, 2);
            if (rp != 2) {
                // HL
                assertInstrEDExpr("ld (", "), " + regPair.l, rp << 1, 4);
                assertInstrEDExpr("ld " + regPair.l + ", (", ")", (rp << 1) | 1, 4);
            }
        });
    }

    @Test
    public void testInstrRP2() {
        forRegPair2(regPair2 -> {
            int rp = CompilerTables.regPairs2.get(regPair2.r);
            assertInstr("pop " + regPair2.l, OPCODE_POP, 3, rp, 0, 1, 1);
            assertInstr("push " + regPair2.l, OPCODE_PUSH, 3, rp, 0, 5, 1);
        });
    }

    @Test
    public void testInstrExpr() {
        assertInstrExpr("ld (", "), hl", OPCODE_LD, 0, 4, 2, 3);
        assertInstrExpr("ld (", "), a", OPCODE_LD, 0, 6, 2, 3);
        assertInstrExpr("ld hl, (", ")", OPCODE_LD, 0, 5, 2, 3);
        assertInstrExpr("ld a, (", ")", OPCODE_LD, 0, 7, 2, 3);
        assertInstrExpr("jp ", OPCODE_JP, 3, 0, 3, 3);
        assertInstrExpr("out (", "), a", OPCODE_OUT, 3, 2, 3, 2);
        assertInstrExpr("in a, (", ")", OPCODE_IN, 3, 3, 3, 2);
        assertInstrExpr("call ", OPCODE_CALL, 3, 0, 1, 5, 3);
        assertInstrExpr("call po,", OPCODE_CALL, 3, 4, 4, 3);
        assertInstrExpr("call pe,", OPCODE_CALL, 3, 5, 4, 3);
        assertInstrExpr("call p,", OPCODE_CALL, 3, 6, 4, 3);
        assertInstrExpr("add a,", OPCODE_ADD, 3, 0, 6, 2);
        assertInstrExpr("adc a,", OPCODE_ADC, 3, 1, 6, 2);
        assertInstrExpr("sub", OPCODE_SUB, 3, 2, 6, 2);
        assertInstrExpr("sbc a,", OPCODE_SBC, 3, 3, 6, 2);
        assertInstrExpr("and", OPCODE_AND, 3, 4, 6, 2);
        assertInstrExpr("xor", OPCODE_XOR, 3, 5, 6, 2);
        assertInstrExpr("or", OPCODE_OR, 3, 6, 6, 2);
        assertInstrExpr("cp", OPCODE_CP, 3, 7, 6, 2);
        assertInstrExpr("rst", OPCODE_RST, 3, 0, 7, 1);
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
        assertEquals(Optional.of(3), program.getChild(0).getChild(0).getSizeBytes());
    }

    @Test
    public void testInstrXDCB() {
        Random random = new Random();
        forRegister(register -> {
            int z = CompilerTables.registers.get(register.r);
            forRot(rot -> {
                int y = CompilerTables.rot.get(rot.r);
                if (z != 6) {
                    assertInstrXDCBExpr(rot.l + " (ix + ", "), " + register.l, rot.r, 0xDD, y, z, 4);
                    assertInstrXDCBExpr(rot.l + " (iy + ", "), " + register.l, rot.r, 0xFD, y, z, 4);
                }
            });

            int y = Math.abs(random.nextInt() % 8);
            if (z != 6) {
                assertInstrXDCBExprBit("res " + y + ", (ix + ", "), " + register.l, OPCODE_RES, 0xDD, y, z, 4);
                assertInstrXDCBExprBit("res " + y + ", (iy + ", "), " + register.l, OPCODE_RES, 0xFD, y, z, 4);
                assertInstrXDCBExprBit("set " + y + ", (ix + ", "), " + register.l, OPCODE_SET, 0xDD, y, z, 4);
                assertInstrXDCBExprBit("set " + y + ", (iy + ", "), " + register.l, OPCODE_SET, 0xFD, y, z, 4);
            }
        });

        forRot(rot -> {
            int y = CompilerTables.rot.get(rot.r);
            assertInstrXDCBExpr(rot.l + " (ix + ", ")", rot.r, 0xDD, y, 6, 4);
            assertInstrXDCBExpr(rot.l + " (iy + ", ")", rot.r, 0xFD, y, 6, 4);
        });

        int y = Math.abs(random.nextInt() % 8);
        assertInstrXDCBExprBit("bit " + y + ", (ix + ", ")", OPCODE_BIT, 0xDD, y, 6, 4);
        assertInstrXDCBExprBit("bit " + y + ", (iy + ", ")", OPCODE_BIT, 0xFD, y, 6, 4);
        assertInstrXDCBExprBit("res " + y + ", (ix + ", ")", OPCODE_RES, 0xDD, y, 6, 4);
        assertInstrXDCBExprBit("res " + y + ", (iy + ", ")", OPCODE_RES, 0xFD, y, 6, 4);
        assertInstrXDCBExprBit("set " + y + ", (ix + ", ")", OPCODE_SET, 0xDD, y, 6, 4);
        assertInstrXDCBExprBit("set " + y + ", (iy + ", ")", OPCODE_SET, 0xFD, y, 6, 4);
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
                    assertInstrXDExpr("ld (" + ii.l + " +", "), " + register.l, OPCODE_LD, prefix, 1, 6, r, 3);
                    assertInstrXDExpr("ld " + register.l + ", (" + ii.l + " +", ")", OPCODE_LD, prefix, 1, r, 6, 3);
                }
            });

            assertInstrXDExpr("ld " + ii.l + ", ", "", OPCODE_LD, prefix, 0, 4, 1, 4);
            assertInstrXDExpr("ld (", "), " + ii.l, OPCODE_LD, prefix, 0, 4, 2, 4);
            assertInstrXDExpr("inc (" + ii.l + "+", ")", OPCODE_INC, prefix, 0, 6, 4, 3);
            assertInstrXDExpr("dec (" + ii.l + "+", ")", OPCODE_DEC, prefix, 0, 6, 5, 3);
            assertInstrXD("inc " + ii.l, OPCODE_INC, prefix, 0, 4, 3, 2);
            assertInstrXDExpr("ld " + ii.l + ", (", ")", OPCODE_LD, prefix, 0, 5, 2, 4);
            assertInstrXD("dec " + ii.l, OPCODE_DEC, prefix, 0, 5, 3, 2);
            assertInstrXD("pop " + ii.l, OPCODE_POP, prefix, 3, 4, 1, 2);
            assertInstrXD("push " + ii.l, OPCODE_PUSH, prefix, 3, 4, 5, 2);
            assertInstrXD("ex (sp), " + ii.l, OPCODE_EX, prefix, 3, 4, 3, 2);
            assertInstrXD("jp (" + ii.l + ")", OPCODE_JP, prefix, 3, 5, 1, 2);
            assertInstrXD("ld sp, " + ii.l, OPCODE_LD, prefix, 3, 7, 1, 2);
            assertInstrXDExprExpr("ld (" + ii.l + "+", prefix, 4);
        });

        forPrefixReg8(ii -> {
            int prefix = CompilerTables.prefix.get(ii.r);
            int r = CompilerTables.registers.get(ii.r);

            assertInstrXD("inc " + ii.l, OPCODE_INC, prefix, 0, r, 4, 2);
            assertInstrXD("dec " + ii.l, OPCODE_DEC, prefix, 0, r, 5, 2);
            assertInstrXDExpr("ld " + ii.l + ", ", "", OPCODE_LD, prefix, 0, r, 6, 3);

            assertInstrXD("add a, " + ii.l, OPCODE_ADD, prefix, 2, 0, r, 2);
            assertInstrXD("adc a, " + ii.l, OPCODE_ADC, prefix, 2, 1, r, 2);
            assertInstrXD("sub " + ii.l, OPCODE_SUB, prefix, 2, 2, r, 2);
            assertInstrXD("sbc a, " + ii.l, OPCODE_SBC, prefix, 2, 3, r, 2);
            assertInstrXD("and " + ii.l, OPCODE_AND, prefix, 2, 4, r, 2);
            assertInstrXD("xor " + ii.l, OPCODE_XOR, prefix, 2, 5, r, 2);
            assertInstrXD("or " + ii.l, OPCODE_OR, prefix, 2, 6, r, 2);
            assertInstrXD("cp " + ii.l, OPCODE_CP, prefix, 2, 7, r, 2);

            forRegister(register -> {
                int r2 = CompilerTables.registers.get(register.r);
                String r2s = (register.r == REG_H) ? (prefix == 0xDD ? "ixh" : "iyh") : ((register.r == REG_L) ? (prefix == 0xDD ? "ixl" : "iyl") : register.l);

                if (r2 != 6) {
                    assertInstrXD("ld " + ii.l + ", " + r2s, OPCODE_LD, prefix, 1, r, r2, 2);
                    assertInstrXD("ld " + r2s + ", " + ii.l, OPCODE_LD, prefix, 1, r2, r, 2);
                }
            });

        });
    }


    private void assertInstr(String instr, int instrType, int x, int y, int z, int size) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new Instr(0, 0, instrType, x, y, z)), program);
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        });
    }

    private void assertInstr(String instr, int instrType, int x, int p, int q, int z, int size) {
        assertInstr(instr, instrType, x, (p << 1) | q, z, size);
    }

    private void assertInstrED(String instr, int instrType, int y, int z, int size) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrED(0, 0, instrType, y, z)), program);
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        });
    }

    private void assertInstrEDExpr(String instrPrefix, String instrPostfix, int y, int size) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, prefixVariation -> forStringCaseVariations(instrPostfix, postfixVariation -> {
            Program program = parseProgram(prefixVariation + " $ + 5" + postfixVariation);
            assertTrees(new Program().addChild(new InstrED(0, 0, OPCODE_LD, y, 3).addChild(expr)), program);
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        }));
    }

    private void assertInstrCB(String instr, int instrType, int y, int z, int size) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrCB(0, 0, instrType, y, z)), program);
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        });
    }

    private void assertInstrExpr(String instrPrefix, String instrPostfix, int instrType, int x, int y, int z, int size) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, prefixVariation -> forStringCaseVariations(instrPostfix, postfixVariation -> {
            Program program = parseProgram(prefixVariation + " $ + 5" + postfixVariation);
            assertTrees(new Program().addChild(new Instr(0, 0, instrType, x, y, z).addChild(expr)), program);
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        }));
    }

    private void assertInstrExpr(String instr, int instrType, int x, int y, int z, int size) {
        assertInstrExpr(instr, "", instrType, x, y, z, size);
    }

    private void assertInstrExpr(String instr, int instrType, int x, int p, int q, int z, int size) {
        assertInstrExpr(instr, instrType, x, (p << 1) | q, z, size);
    }

    private void assertInstrCBExprBit(String instr, int instrType, int y, int z, int size) {
        Node expr = new ExprNumber(0, 0, y);

        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrCB(0, 0, instrType, 0, z).addChild(expr)), program);
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        });
    }

    private void assertInstrXDCBExpr(String instrPrefix, String instrPostfix, int instrType, int prefix, int y, int z, int size) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, instrPrefixVariation -> forStringCaseVariations(instrPostfix, instrPostfixVariation -> {
            Program program = parseProgram(instrPrefixVariation + " $ + 5" + instrPostfixVariation);
            assertTrees(new Program().addChild(new InstrXDCB(0, 0, instrType, prefix, y, z).addChild(expr)), program);
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        }));
    }

    private void assertInstrXDCBExprBit(String instrPrefix, String instrPostfix, int instrType, int prefix, int y, int z, int size) {
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
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        }));
    }

    private void assertInstrXD(String instr, int instrType, int prefix, int x, int y, int z, int size) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrXD(0, 0, instrType, prefix, x, y, z)), program);
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        });
    }

    private void assertInstrXD(String instr, int instrType, int prefix, int x, int p, int q, int z, int size) {
        assertInstrXD(instr, instrType, prefix, x, (p << 1) | q, z, size);
    }

    private void assertInstrXDExpr(String instrPrefix, String instrPostfix, int instrType, int prefix, int x, int y, int z, int size) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, prefixVariation -> {
            forStringCaseVariations(instrPostfix, postfixVariation -> {
                Program program = parseProgram(prefixVariation + " $ + 5" + postfixVariation);
                assertTrees(new Program().addChild(new InstrXD(0, 0, instrType, prefix, x, y, z).addChild(expr)), program);
                assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
            });
        });
    }

    private void assertInstrXDExprExpr(String instrPrefix, int prefix, int size) {
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
            assertEquals(Optional.of(size), program.getChild(0).getSizeBytes());
        });
    }
}
