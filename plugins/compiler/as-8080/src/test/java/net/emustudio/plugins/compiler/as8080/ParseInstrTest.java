package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.Statement;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrNoArgs;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrReg;
import org.junit.Test;

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

    private void assertInstrNoArgs(String instr, int instrType) {
        forStringCaseVariations(instr, variation -> {
            Program program = parseProgram(variation);
            assertTrees(new Program().addChild(new Statement().addChild(new InstrNoArgs(instrType))), program);
        });
    }

    private void assertInstrReg(String instr, int instrType) {
        Map<String, Integer> registers = Map.of(
            "a", REG_A,
            "b", REG_B,
            "c", REG_C,
            "d", REG_D,
            "e", REG_E,
            "h", REG_H,
            "l", REG_L,
            "m", REG_M
        );

        forStringCaseVariations(instr, instrVariation -> {
            for (Map.Entry<String, Integer> register : registers.entrySet()) {
                forStringCaseVariations(register.getKey(), registerVariation -> {
                    String row = instrVariation + " " + registerVariation;
                    Program program = parseProgram(row);
                    assertTrees(
                        new Program()
                        .addChild(new Statement()
                            .addChild(new InstrReg(instrType, register.getValue()))),
                        program
                    );
                });
            }
        });
    }
}
