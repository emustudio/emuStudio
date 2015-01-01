package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

public class InstructionsLogicTest extends InstructionsTest {

    @Test
    public void testANA() throws Exception {
        resetProgram(
                0xA6,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheck(0xC0, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P);
        checkNotFlags(EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC);
    }

    @Test
    public void testXRA() throws Exception {
        resetProgram(
                0xAE,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheck(0x3F, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_P);
        checkNotFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C |
                        EmulatorEngine.FLAG_AC
        );
    }

    @Test
    public void testORA() throws Exception {
        resetProgram(
                0xB6,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P);
        checkNotFlags(
                EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC
        );
    }

}
