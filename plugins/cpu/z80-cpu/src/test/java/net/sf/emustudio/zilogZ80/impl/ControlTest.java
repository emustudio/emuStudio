package net.sf.emustudio.zilogZ80.impl;

import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.zilogZ80.impl.suite.IntegerTestBuilder;
import org.junit.Test;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_PV;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_S;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_Z;

public class ControlTest extends InstructionsTTest {

    @Test
    public void testEI_DI() throws Exception {
        cpuVerifierImpl.checkInterruptsAreDisabled(0);
        cpuRunnerImpl.setProgram(0xFB, 0xF3);
        cpuRunnerImpl.reset();

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkInterruptsAreEnabled(0);

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkInterruptsAreDisabled(0);
    }

    @Test
    public void testJP__nn__AND__JP_cc__nn() throws Exception {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryAddressWord(0)
                .verifyPC(context -> context.first)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(3,
                test.runWithFirstOperand(0xC3),
                test.runWithFirstOperand(0xC2),
                test.setFlags(FLAG_Z).runWithFirstOperand(0xCA),
                test.runWithFirstOperand(0xD2),
                test.setFlags(FLAG_C).runWithFirstOperand(0xDA),
                test.runWithFirstOperand(0xE2),
                test.setFlags(FLAG_PV).runWithFirstOperand(0xEA),
                test.runWithFirstOperand(0xF2),
                test.setFlags(FLAG_S).runWithFirstOperand(0xFA)
        );
    }


}
