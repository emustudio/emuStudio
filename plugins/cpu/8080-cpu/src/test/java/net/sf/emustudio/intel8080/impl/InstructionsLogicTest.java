package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

public class InstructionsLogicTest extends InstructionsTest {


    @Test
    public void testCMP_M() throws Exception {
        resetProgram(
                0xBE,
                0xC0 // address 1
        );

        setRegisters(0xFF, 0, 0, 0, 0, 0, 1);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheckAccAndFlags(0xFF, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
    }

    @Test
    public void testCPI() throws Exception {
        resetProgram(0xFE, 0xC0);

        setRegisters(0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheckAccAndFlags(0xFF, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
    }
}
