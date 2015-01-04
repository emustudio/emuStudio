package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

public class InstructionsLogicTest extends InstructionsTest {

    @Test
    public void testANA() throws Exception {
        resetProgram(0xA7, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5);

        setRegisters(0xC5, 0x5, 0x7, 0x4, 0x1, 0xF0, 0x0F);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0xC5, FLAG_S_P, FLAG_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0x05, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0x05, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0x04, -1, FLAG_S_Z_AC_P_C);
        setFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC);
        setRegisters(5);
        stepAndCheckAccAndFlags(0x01, EmulatorEngine.FLAG_AC, FLAG_S_Z_P_C);

        setRegisters(0xFF);
        setFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C);
        resetFlags(EmulatorEngine.FLAG_AC);
        stepAndCheckAccAndFlags(0xF0, FLAG_S_P, FLAG_Z_AC_C);

        stepAndCheckAccAndFlags(0, FLAG_Z_P, FLAG_S_AC_C);
    }

    @Test
    public void testANA_M() throws Exception {
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
    public void testANI() throws Exception {
        resetProgram(0xE6, 0xC0);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheckAccAndFlags(0xC0, FLAG_S_P, FLAG_Z_AC_C);
    }

    @Test
    public void testXRA() throws Exception {
        resetProgram(0xAF, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD);
        setRegisters(0xC5, 0x5, 0x0F, 0x8, 0x1, 0xF0, 0x0F);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_P, FLAG_S_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0x05, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0x0A, FLAG_AC_P, FLAG_S_Z_C);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0x2, EmulatorEngine.FLAG_AC, FLAG_S_Z_P_C);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0x3, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        setRegisters(0xFF);
        setFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C);
        resetFlags(EmulatorEngine.FLAG_AC);
        stepAndCheckAccAndFlags(0x0F, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P, FLAG_S_C);
    }

    @Test
    public void testXRA_M() throws Exception {
        resetProgram(
                0xAE,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheckAccAndFlags(0x3F, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
    }

    @Test
    public void testXRI() throws Exception {
        resetProgram(0xEE, 0xC0);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheckAccAndFlags(0x3F, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
    }

    @Test
    public void testORA() throws Exception {
        resetProgram(0xB7, 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5);
        setRegisters(0xC5, 0x5, 0x0F, 0x5, 0x1, 0, 0x1F);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0xC5, FLAG_S_P, FLAG_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0xC5, FLAG_S_P, FLAG_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        setRegisters(0x8);
        stepAndCheckAccAndFlags(0x0F, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        setRegisters(0xA);
        stepAndCheckAccAndFlags(0x0F, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_C);
        setRegisters(0);
        setFlags(EmulatorEngine.FLAG_Z);
        stepAndCheckAccAndFlags(0x1, -1, FLAG_S_Z_AC_P_C);
        setRegisters(0);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_P, FLAG_S_AC_C);
        stepAndCheckAccAndFlags(0x1F, -1, FLAG_S_Z_AC_P_C);
    }

    @Test
    public void testORA_M() throws Exception {
        resetProgram(
                0xB6,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C);

        stepAndCheckAccAndFlags(0xFF, FLAG_S_P, FLAG_Z_AC_C);
    }

}
