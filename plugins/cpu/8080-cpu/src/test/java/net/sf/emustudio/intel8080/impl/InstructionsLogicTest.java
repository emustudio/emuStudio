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

    @Test
    public void testORI() throws Exception {
        resetProgram(0xF6, 0xC0);
        setRegister(EmulatorEngine.REG_A, 0x5);
        setFlags(FLAG_P_C);

        stepAndCheckAccAndFlags(0xC5, FLAG_S_P, FLAG_Z_AC_C);
    }

    @Test
    public void testDAA() throws Exception {
        resetProgram(0x27, 0x27, 0x27, 0x27, 0x27);

        setRegisters(0x9B);
        setFlags(EmulatorEngine.FLAG_S);
        stepAndCheckAccAndFlags(1, FLAG_AC_C, FLAG_S_Z_P);

        setRegisters(0x22);
        resetFlags(FLAG_S_Z_AC_C);
        setFlags(EmulatorEngine.FLAG_P);
        stepAndCheckAccAndFlags(0x22, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);

        setRegisters(0x22);
        resetFlags(FLAG_S_Z_C);
        setFlags(FLAG_AC_P);
        stepAndCheckAccAndFlags(0x28, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);

        setRegisters(0x22);
        resetFlags(FLAG_S_Z_AC);
        setFlags(FLAG_P_C);
        stepAndCheckAccAndFlags(0x82, FLAG_S_P_C, FLAG_Z_AC);

        resetFlags(FLAG_S_AC_P_C);
        setFlags(FLAG_Z_P);
        setRegisters(0);
        stepAndCheckAccAndFlags(0, FLAG_Z_P, FLAG_S_AC_C);
    }

    @Test
    public void testCMA() throws Exception {
        resetProgram(0x2F, 0x2F, 0x2F);

        stepAndCheckAccAndFlags(0xFF, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0, -1, FLAG_S_Z_AC_P_C);

        setRegisters(0x2F);
        stepAndCheckAccAndFlags(0xD0, -1, FLAG_S_Z_AC_P_C);
    }

    @Test
    public void testSTC_CMC() throws Exception {
        resetProgram(0x37, 0x3F);

        stepAndCheckAccAndFlags(0, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0, -1, FLAG_S_Z_AC_P_C);
    }

    @Test
    public void testRLC() throws Exception {
        resetProgram(0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07);

        setRegisters(0xF2);
        stepAndCheckAccAndFlags(0xE5, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xCB, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0x97, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0x2F, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0x5E, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0xBC, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0x79, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xF2, -1, FLAG_S_Z_AC_P_C);
    }

    @Test
    public void testRRC() throws Exception {
        resetProgram(0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F);

        setRegisters(0xF2);
        stepAndCheckAccAndFlags(0x79, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0xBC, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0x5E, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0x2F, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0x97, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xCB, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xE5, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xF2, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
    }

    @Test
    public void testRAL() throws Exception {
        resetProgram(0x17, 0x17, 0x17, 0x17, 0x17, 0x17, 0x17, 0x17);

        setRegisters(0xB5);
        stepAndCheckAccAndFlags(0x6A, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xD5, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0xAA, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0x55, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xAB, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0x56, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xAD, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0x5A, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
    }

    @Test
    public void testRAR() throws Exception {
        resetProgram(0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F);

        setRegisters(0x6A);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0xB5, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0x5A, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xAD, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0x56, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xAB, -1, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(0x55, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xAA, EmulatorEngine.FLAG_C, FLAG_S_Z_AC_P);
        stepAndCheckAccAndFlags(0xD5, -1, FLAG_S_Z_AC_P_C);

    }

    @Test
    public void testCMP() throws Exception {
        resetProgram(0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD);

        // TODO

    }

    @Test
    public void testCMP_M() throws Exception {
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

    /*CMP		A		CP		A		BF		A - A
CMP		B		CP		B		B8		A - B
CMP		C		CP		C		B9		A - C
CMP		D		CP		D		BA		A - D
CMP		E		CP		E		BB		A - E
CMP		H		CP		H		BC		A - H
CMP		L		CP		L		BD		A - L*/
}
