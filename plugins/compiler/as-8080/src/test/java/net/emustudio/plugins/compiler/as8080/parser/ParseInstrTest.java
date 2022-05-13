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
package net.emustudio.plugins.compiler.as8080.parser;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprCurrentAddress;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import static net.emustudio.plugins.compiler.as8080.Utils.*;

public class ParseInstrTest {

    @Test
    public void testInstrNoArgs() {
        assertInstrNoArgs("stc", OPCODE_STC);
        assertInstrNoArgs("cmc", OPCODE_CMC);
        assertInstrNoArgs("daa", OPCODE_DAA);
        assertInstrNoArgs("nop", OPCODE_NOP);
        assertInstrNoArgs("rlc", OPCODE_RLC);
        assertInstrNoArgs("rrc", OPCODE_RRC);
        assertInstrNoArgs("ral", OPCODE_RAL);
        assertInstrNoArgs("rar", OPCODE_RAR);
        assertInstrNoArgs("xchg", OPCODE_XCHG);
        assertInstrNoArgs("xthl", OPCODE_XTHL);
        assertInstrNoArgs("sphl", OPCODE_SPHL);
        assertInstrNoArgs("pchl", OPCODE_PCHL);
        assertInstrNoArgs("ret", OPCODE_RET);
        assertInstrNoArgs("rc", OPCODE_RC);
        assertInstrNoArgs("rnc", OPCODE_RNC);
        assertInstrNoArgs("rz", OPCODE_RZ);
        assertInstrNoArgs("rnz", OPCODE_RNZ);
        assertInstrNoArgs("rm", OPCODE_RM);
        assertInstrNoArgs("rp", OPCODE_RP);
        assertInstrNoArgs("rpe", OPCODE_RPE);
        assertInstrNoArgs("rpo", OPCODE_RPO);
        assertInstrNoArgs("ei", OPCODE_EI);
        assertInstrNoArgs("di", OPCODE_DI);
        assertInstrNoArgs("hlt", OPCODE_HLT);
    }

    @Test
    public void testInstrReg() {
        assertInstrReg("inr", OPCODE_INR);
        assertInstrReg("dcr", OPCODE_DCR);
        assertInstrReg("add", OPCODE_ADD);
        assertInstrReg("adc", OPCODE_ADC);
        assertInstrReg("sub", OPCODE_SUB);
        assertInstrReg("sbb", OPCODE_SBB);
        assertInstrReg("ana", OPCODE_ANA);
        assertInstrReg("xra", OPCODE_XRA);
        assertInstrReg("ora", OPCODE_ORA);
        assertInstrReg("cmp", OPCODE_CMP);
    }

    @Test
    public void testInstrExpr() {
        assertInstrExpr("lda", OPCODE_LDA);
        assertInstrExpr("sta", OPCODE_STA);
        assertInstrExpr("lhld", OPCODE_LHLD);
        assertInstrExpr("shld", OPCODE_SHLD);
        assertInstrExpr("adi", OPCODE_ADI);
        assertInstrExpr("aci", OPCODE_ACI);
        assertInstrExpr("sui", OPCODE_SUI);
        assertInstrExpr("sbi", OPCODE_SBI);
        assertInstrExpr("ani", OPCODE_ANI);
        assertInstrExpr("ori", OPCODE_ORI);
        assertInstrExpr("xri", OPCODE_XRI);
        assertInstrExpr("cpi", OPCODE_CPI);
        assertInstrExpr("jmp", OPCODE_JMP);
        assertInstrExpr("jc", OPCODE_JC);
        assertInstrExpr("jnc", OPCODE_JNC);
        assertInstrExpr("jz", OPCODE_JZ);
        assertInstrExpr("jnz", OPCODE_JNZ);
        assertInstrExpr("jm", OPCODE_JM);
        assertInstrExpr("jp", OPCODE_JP);
        assertInstrExpr("jpe", OPCODE_JPE);
        assertInstrExpr("jpo", OPCODE_JPO);
        assertInstrExpr("call", OPCODE_CALL);
        assertInstrExpr("cc", OPCODE_CC);
        assertInstrExpr("cnc", OPCODE_CNC);
        assertInstrExpr("cz", OPCODE_CZ);
        assertInstrExpr("cnz", OPCODE_CNZ);
        assertInstrExpr("cm", OPCODE_CM);
        assertInstrExpr("cp", OPCODE_CP);
        assertInstrExpr("cpe", OPCODE_CPE);
        assertInstrExpr("cpo", OPCODE_CPO);
        assertInstrExpr("in", OPCODE_IN);
        assertInstrExpr("out", OPCODE_OUT);
        assertInstrExpr("rst", OPCODE_RST);
    }

    @Test
    public void testRegPair() {
        assertInstrRegPair("stax", OPCODE_STAX, regPairsBD);
        assertInstrRegPair("ldax", OPCODE_LDAX, regPairsBD);
        assertInstrRegPair("dad", OPCODE_DAD, regPairsBDHSP);
        assertInstrRegPair("inx", OPCODE_INX, regPairsBDHSP);
        assertInstrRegPair("dcx", OPCODE_DCX, regPairsBDHSP);
        assertInstrRegPair("push", OPCODE_PUSH, regPairsBDHPSW);
        assertInstrRegPair("pop", OPCODE_POP, regPairsBDHPSW);
    }

    @Test
    public void testMVI() {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations("mvi", instrVariation -> {
            for (Map.Entry<String, Integer> register : registers.entrySet()) {
                forStringCaseVariations(register.getKey(), registerVariation -> {
                    String row = instrVariation + " " + registerVariation + ", $ + 5";
                    Program program = parseProgram(row);
                    assertTrees(
                        new Program()
                            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, register.getValue())
                                .addChild(expr)),
                        program
                    );
                });
            }
        });
    }

    @Test
    public void testLXI() {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations("lxi", instrVariation -> {
            for (Map.Entry<String, Integer> regPair : regPairsBDHSP.entrySet()) {
                forStringCaseVariations(regPair.getKey(), registerVariation -> {
                    String row = instrVariation + " " + registerVariation + ", $ + 5";
                    Program program = parseProgram(row);
                    assertTrees(
                        new Program()
                            .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, regPair.getValue())
                                .addChild(expr)),
                        program
                    );
                });
            }
        });
    }

    @Test
    public void testMOV() {
        forStringCaseVariations("mov", instrVariation -> {
            for (Map.Entry<String, Integer> register1 : registers.entrySet()) {
                forStringCaseVariations(register1.getKey(), registerVariation1 -> {
                    for (Map.Entry<String, Integer> register2 : registers.entrySet()) {
                        forStringCaseVariations(register2.getKey(), registerVariation2 -> {
                            if (!registerVariation1.toLowerCase(Locale.ENGLISH).equals("m") || !registerVariation1.equals(registerVariation2)) {
                                String row = instrVariation + " " + registerVariation1 + ", " + registerVariation2;
                                Program program = parseProgram(row);
                                assertTrees(
                                    new Program()
                                        .addChild(new InstrRegReg(0, 0, OPCODE_MOV, register1.getValue(), register2.getValue())),
                                    program
                                );
                            }
                        });
                    }
                });
            }
        });
    }

    private void assertInstrNoArgs(String instr, int instrType) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrNoArgs(0, 0, instrType)), program);
        });
    }

    private void assertInstrReg(String instr, int instrType) {
        forStringCaseVariations(instr, instrVariation -> {
            for (Map.Entry<String, Integer> register : registers.entrySet()) {
                forStringCaseVariations(register.getKey(), registerVariation -> {
                    String row = instrVariation + " " + registerVariation;
                    Program program = parseProgram(row);
                    assertTrees(
                        new Program().addChild(new InstrReg(0, 0, instrType, register.getValue())),
                        program
                    );
                });
            }
        });
    }

    private void assertInstrExpr(String instr, int instrType) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation + " $ + 5");
            assertTrees(new Program().addChild(new InstrExpr(0, 0, instrType).addChild(expr)), program);
        });
    }

    private void assertInstrRegPair(String instr, int instrType, Map<String, Integer> regPairs) {
        forStringCaseVariations(instr, instrVariation -> {
            for (Map.Entry<String, Integer> regPair : regPairs.entrySet()) {
                forStringCaseVariations(regPair.getKey(), registerVariation -> {
                    String row = instrVariation + " " + registerVariation;
                    Program program = parseProgram(row);
                    assertTrees(
                        new Program().addChild(new InstrRegPair(0, 0, instrType, regPair.getValue())),
                        program
                    );
                });
            }
        });
    }
}
