package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

public class InstructionsStackTest extends InstructionsTest {

    @Test
    public void testPUSH() throws Exception {
        resetProgram(
                0xC5, 0xD5, 0xE5, 0xF5,
                0, // address 4
                0, 0, 0, 0, 0, 0, 0, 0
        );
        setRegisters(1, 2, 3, 4, 5, 6, 7);

        cpu.getEngine().SP = 0xC;
        stepAndCheckPCandSPandMemory(1, 0xA, 0x203);
        stepAndCheckPCandSPandMemory(2, 8, 0x405);
        stepAndCheckPCandSPandMemory(3, 6, 0x607);
        setFlags(FLAG_S_Z_AC_P_C);
        stepAndCheckPCandSPandMemory(4, 4, 0x01D7);
    }

    @Test
    public void testPOP() throws Exception {
        resetProgram(
                0xC1, 0xD1, 0xE1, 0xF1,
                0, //address 4
                3, 2, 5, 4, 7, 6, 0xD7, 1
        );

        cpu.getEngine().SP = 5;
        stepAndCheckPCandSP(1, 7);
        checkRegister(EmulatorEngine.REG_B, 2);
        checkRegister(EmulatorEngine.REG_C, 3);

        stepAndCheckPCandSP(2, 9);
        checkRegister(EmulatorEngine.REG_D, 4);
        checkRegister(EmulatorEngine.REG_E, 5);

        stepAndCheckPCandSP(3, 0xB);
        checkRegister(EmulatorEngine.REG_H, 6);
        checkRegister(EmulatorEngine.REG_L, 7);

        stepAndCheckPCandSP(4, 0xD);
        checkRegister(EmulatorEngine.REG_A, 1);
        checkFlags(FLAG_S_Z_AC_P_C);
    }
}
