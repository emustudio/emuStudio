package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

public class InstructionsMemoryTest extends InstructionsTest {

    @Test
    public void testCMP() throws Exception {
        resetProgram(
                0xBE,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheckAccAndFlags(0xFF, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
    }

}
