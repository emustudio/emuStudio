package net.emustudio.plugins.compiler.asZ80.parser;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprCurrentAddress;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrCB;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrED;
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
        assertInstr("exx", OPCODE_EXX, 3, 3, 1);
        assertInstr("jp hl", OPCODE_JP, 3, 5, 1);
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
            assertInstr("inc " + regValue.l, OPCODE_INC, 0, regValue.r, 4);
            assertInstr("dec " + regValue.l, OPCODE_DEC, 0, regValue.r, 5);
            assertInstrExpr("ld " + regValue.l + ", ", OPCODE_LD, 0, regValue.r, 6);
            assertInstr("add a, " + regValue.l, OPCODE_ADD, 2, 0, regValue.r);
            assertInstr("adc a, " + regValue.l, OPCODE_ADC, 2, 1, regValue.r);
            assertInstr("sub " + regValue.l, OPCODE_SUB, 2, 2, regValue.r);
            assertInstr("sbc a, " + regValue.l, OPCODE_SBC, 2, 3, regValue.r);
            assertInstr("and " + regValue.l, OPCODE_AND, 2, 4, regValue.r);
            assertInstr("xor " + regValue.l, OPCODE_XOR, 2, 5, regValue.r);
            assertInstr("or " + regValue.l, OPCODE_OR, 2, 6, regValue.r);
            assertInstr("cp " + regValue.l, OPCODE_CP, 2, 7, regValue.r);

            assertInstrCBNoArgs("rlc " + regValue.l, OPCODE_RLC, 0, regValue.r);
            assertInstrCBNoArgs("rrc " + regValue.l, OPCODE_RRC, 1, regValue.r);
            assertInstrCBNoArgs("rl " + regValue.l, OPCODE_RL, 2, regValue.r);
            assertInstrCBNoArgs("rr " + regValue.l, OPCODE_RR, 3, regValue.r);
            assertInstrCBNoArgs("sla " + regValue.l, OPCODE_SLA, 4, regValue.r);
            assertInstrCBNoArgs("sra " + regValue.l, OPCODE_SRA, 5, regValue.r);
            assertInstrCBNoArgs("sll " + regValue.l, OPCODE_SLL, 6, regValue.r);
            assertInstrCBNoArgs("srl " + regValue.l, OPCODE_SRL, 7, regValue.r);

            int bit = random.nextInt() % 8;
            assertInstrCBExprBit("bit " + bit + ", " + regValue.l, OPCODE_BIT, bit, regValue.r);
            assertInstrCBExprBit("res " + bit + ", " + regValue.l, OPCODE_RES, bit, regValue.r);
            assertInstrCBExprBit("set " + bit + ", " + regValue.l, OPCODE_SET, bit, regValue.r);

            assertInstrED("in " + regValue.l + ", (c)", OPCODE_IN, regValue.r, 0);
            assertInstrED("out (c), " + regValue.l, OPCODE_OUT, regValue.r, 1);

            forRegister(regValue2 -> {
                assertInstr("ld " + regValue.l + ", " + regValue2.l, OPCODE_LD, 1, regValue.r, regValue2.r);
            });
        });
    }

    @Test
    public void testInstrRP() {
        forRegPair(regPair -> {
            assertInstrExpr("ld " + regPair.l + ", ", OPCODE_LD, 0, regPair.r, 0, 1);
            assertInstr("add hl, " + regPair.l, OPCODE_ADD, 0, (regPair.r << 1) | 1, 1);
            assertInstr("inc " + regPair.l, OPCODE_INC, 0, regPair.r, 0, 3);
            assertInstr("dec " + regPair.l, OPCODE_DEC, 0, regPair.r, 1, 3);
            assertInstrED("sbc hl, " + regPair.l, OPCODE_SBC, regPair.r << 1, 2);
            assertInstrED("adc hl, " + regPair.l, OPCODE_ADC, (regPair.r << 1) | 1, 2);
            assertInstrEDExpr("ld (", "), " + regPair.l, OPCODE_LD, regPair.r << 1, 3);
            assertInstrEDExpr("ld " + regPair.l + ", (", ")" + regPair.l, OPCODE_LD, (regPair.r << 1) | 1, 3);
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

    private void assertInstrEDExpr(String instrPrefix, String instrPostfix, int instrType, int y, int z) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, prefixVariation -> {
            forStringCaseVariations(instrPostfix, postfixVariation -> {
                Program program = parseProgram(prefixVariation + " $ + 5" + postfixVariation);
                assertTrees(new Program().addChild(new InstrED(0, 0, instrType, y, z).addChild(expr)), program);
            });
        });
    }

    private void assertInstrCBNoArgs(String instr, int instrType, int y, int z) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new InstrCB(0, 0, instrType, y, z)), program);
        });
    }

    private void assertInstrExpr(String instrPrefix, String instrPostfix, int instrType, int x, int y, int z) {
        Node expr = new ExprInfix(0, 0, OP_ADD)
            .addChild(new ExprCurrentAddress(0, 0))
            .addChild(new ExprNumber(0, 0, 5));

        forStringCaseVariations(instrPrefix, prefixVariation -> {
            forStringCaseVariations(instrPostfix, postfixVariation -> {
                Program program = parseProgram(prefixVariation + " $ + 5" + postfixVariation);
                assertTrees(new Program().addChild(new Instr(0, 0, instrType, x, y, z).addChild(expr)), program);
            });
        });
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
}
