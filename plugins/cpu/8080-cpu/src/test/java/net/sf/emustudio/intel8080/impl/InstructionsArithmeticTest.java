package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InstructionsArithmeticTest extends InstructionsTest {
    private final static int FLAG_S_P = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P;
    private final static int FLAG_S_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C;
    private final static int FLAG_S_AC = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_AC;
    private final static int FLAG_S_AC_P_C = EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P;
    private final static int FLAG_S_AC_C = EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_AC;
    private final static int FLAG_S_Z_P = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_P;
    private final static int FLAG_S_Z_P_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_C;
    private final static int FLAG_S_Z_AC_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_C;
    private final static int FLAG_S_Z_AC_P_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_Z;
    private final static int FLAG_Z_AC_P = EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P;
    private final static int FLAG_Z_AC_C = EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC;
    private final static int FLAG_Z_AC_P_C = EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_AC;
    private final static int FLAG_Z_P = EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_Z;
    private final static int FLAG_Z_P_C = EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C;
    private final static int FLAG_AC_C = EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_C;

    @Test
    public void testADD() throws Exception {
        resetProgram(0x87, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85);

        setRegisters(0xFF, 1, 1, 0xFF, 3, 4, 0xFF-5);

        stepAndCheckAccAndFlags(0xFE, FLAG_S_AC_C, FLAG_Z_P);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_P, FLAG_Z_AC_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P_C, EmulatorEngine.FLAG_S);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_P, FLAG_Z_AC_C);
        stepAndCheckAccAndFlags(2, FLAG_AC_C, FLAG_S_Z_P);
        stepAndCheckAccAndFlags(6, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P_C, EmulatorEngine.FLAG_S);
    }

    @Test
    public void testADD_M() throws Exception {
        resetProgram(
                0x86,
                2 // address 1
        );

        // with overflow, signed
        setRegister(EmulatorEngine.REG_A, 0xFE);
        setFlags(EmulatorEngine.FLAG_S);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckAccAndFlags(0, FLAG_Z_AC_C, EmulatorEngine.FLAG_S);
    }

    @Test
    public void testADI() throws Exception {
        resetProgram(0xC6, 1);

        setRegisters(0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P_C, EmulatorEngine.FLAG_S);
    }

    @Test
    public void testADC() throws Exception {
        resetProgram(0x8F, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D);

        setRegisters(0xFF, 0, 1, 0xFE, 3, 3, 0xFF-5);

        stepAndCheckAccAndFlags(0xFE, FLAG_S_AC_C, FLAG_Z_P);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_P, FLAG_Z_AC_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P_C, EmulatorEngine.FLAG_S);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_P, FLAG_Z_AC_C);
        stepAndCheckAccAndFlags(2, FLAG_AC_C, FLAG_S_Z_P);
        stepAndCheckAccAndFlags(6, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P_C, EmulatorEngine.FLAG_S);
    }

    @Test
    public void testADC_M() throws Exception {
        resetProgram(
                0x8E,
                2 // address 1
        );

        setFlags(EmulatorEngine.FLAG_C);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckAccAndFlags(3, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
    }

    @Test
    public void testACI() throws Exception {
        resetProgram(0xCE, 1);

        setRegisters(0xFE);
        setFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C);

        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P_C, EmulatorEngine.FLAG_S);
    }

    @Test
    public void testSUB() throws Exception {
        resetProgram(0x97, 0x90, 0x91, 0x92, 0x93, 0x94, 0x95);
        setRegisters(0xFF, 1, 0xFE, 1, 0xFF, 0xFF, 0xFF);

        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P, FLAG_S_C);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_P, FLAG_Z_AC_C);
        stepAndCheckAccAndFlags(1, FLAG_S_AC, FLAG_Z_P_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P, FLAG_S_C);
        stepAndCheckAccAndFlags(1, 0xFF, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(2, 0xFF, FLAG_S_Z_AC_P_C);
        stepAndCheckAccAndFlags(3, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
    }

    @Test
    public void testSUB_M() throws Exception {
        resetProgram(
                0x96,
                1 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckAccAndFlags(0xFF, FLAG_S_AC_P_C, EmulatorEngine.FLAG_Z);
    }

    @Test
    public void testSBB() throws Exception {
        resetProgram(0x9F, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D);
        setRegisters(0xFF, 0, 0xFD, 0, 0xFE, 0xFE, 0xFE);

        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P, FLAG_S_C);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_P, FLAG_Z_AC_C);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(1, FLAG_S_AC, FLAG_Z_P_C);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P, FLAG_S_C);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(1, 0xFF, FLAG_S_Z_AC_P_C);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(2, 0xFF, FLAG_S_Z_AC_P_C);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(3, EmulatorEngine.FLAG_P, FLAG_S_Z_AC_C);
    }

    @Test
    public void testSBB_M() throws Exception {
        resetProgram(
                0x9E,
                1 // address 1
        );

        setRegister(EmulatorEngine.REG_A, 2);
        setFlags(EmulatorEngine.FLAG_C);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P, FLAG_S_C);
    }

    @Test
    public void testSBI() throws Exception {
        resetProgram(0xDE, 0xFF);

        setRegister(EmulatorEngine.REG_A, 2);

        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(2, 0xFF, FLAG_S_Z_AC_P_C);
    }

    @Test
    public void testINR() throws Exception {
        resetProgram(0x3C, 0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C);
        setRegisters(0xFF, 0, 127, 0x0F, 2, 0xFE, 0x0E);

        stepAndCheckAccAndFlags(0, FLAG_Z_AC_P, FLAG_S_C);

        stepAndCheck(1, EmulatorEngine.REG_B);
        checkNotFlags(FLAG_S_Z_AC_P_C);

        stepAndCheck(128, EmulatorEngine.REG_C);
        checkFlags(FLAG_S_AC);
        checkNotFlags(FLAG_Z_P_C);

        stepAndCheck(0x10, EmulatorEngine.REG_D);
        checkFlags(EmulatorEngine.FLAG_AC);
        checkNotFlags(FLAG_S_Z_P_C);

        stepAndCheck(3, EmulatorEngine.REG_E);
        checkFlags(EmulatorEngine.FLAG_P);
        checkNotFlags(FLAG_S_Z_AC_C);

        stepAndCheck(0xFF, EmulatorEngine.REG_H);
        checkFlags(FLAG_S_P);
        checkNotFlags(FLAG_Z_AC_C);

        stepAndCheck(0x0F, EmulatorEngine.REG_L);
        checkFlags(EmulatorEngine.FLAG_P);
        checkNotFlags(FLAG_S_Z_AC_C);
    }

    @Test
    public void testINR_M() throws Exception {
        resetProgram(
                0x34,
                0xFF // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckMemoryAndFlags(0, 1, FLAG_Z_AC_P_C, EmulatorEngine.FLAG_S);
    }

    @Test
    public void testDCR() throws Exception {
        resetProgram(0x3D, 0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D);
        setRegisters(0, 1, 128, 0x10, 3, 0xFF, 0x0F);

        stepAndCheckAccAndFlags(0xFF, FLAG_S_P, FLAG_Z_AC_C);

        stepAndCheck(0, EmulatorEngine.REG_B);
        checkFlags(FLAG_Z_AC_P);
        checkNotFlags(FLAG_S_C);

        stepAndCheck(127, EmulatorEngine.REG_C);
        checkNotFlags(FLAG_S_Z_AC_P_C);

        stepAndCheck(0x0F, EmulatorEngine.REG_D);
        checkFlags(EmulatorEngine.FLAG_P);
        checkNotFlags(FLAG_S_Z_AC_C);

        stepAndCheck(2, EmulatorEngine.REG_E);
        checkFlags(EmulatorEngine.FLAG_AC);
        checkNotFlags(FLAG_S_Z_P_C);

        stepAndCheck(0xFE, EmulatorEngine.REG_H);
        checkFlags(FLAG_S_AC);
        checkNotFlags(FLAG_Z_P_C);

        stepAndCheck(0x0E, EmulatorEngine.REG_L);
        checkFlags(EmulatorEngine.FLAG_AC);
        checkNotFlags(FLAG_S_Z_P_C);
    }

    @Test
    public void testDCR_M() throws Exception {
        resetProgram(
                0x35,
                0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckMemoryAndFlags(0xFF, 1, FLAG_S_AC_P_C, EmulatorEngine.FLAG_Z);
    }

    @Test
    public void testINX() throws Exception {
        resetProgram(0x03, 0x13, 0x23, 0x33);

        setRegisters(0, 0xFF, 0xFF, 0, 0xFF, 0x11, 0xFF);
        cpu.getEngine().SP = 0x0F;

        stepAndCheck(0, EmulatorEngine.REG_B);
        checkRegister(EmulatorEngine.REG_C, 0);
        checkNotFlags(FLAG_S_Z_AC_P_C);

        stepAndCheck(1, EmulatorEngine.REG_D);
        checkRegister(EmulatorEngine.REG_E, 0);
        checkNotFlags(FLAG_S_Z_AC_P_C);

        stepAndCheck(0x12, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0);
        checkNotFlags(FLAG_S_Z_AC_P_C);

        cpu.step();
        assertEquals(0x10, cpu.getEngine().SP);
        checkNotFlags(FLAG_S_Z_AC_P_C);
    }

    @Test
    public void testDCX() throws Exception {
        resetProgram(0x0B, 0x1B, 0x2B, 0x3B);

        setRegisters(0, 0, 0, 1, 0, 0x12, 0);
        cpu.getEngine().SP = 0x10;

        stepAndCheck(0xFF, EmulatorEngine.REG_B);
        checkRegister(EmulatorEngine.REG_C, 0xFF);
        checkNotFlags(FLAG_S_Z_AC_P_C);

        stepAndCheck(0, EmulatorEngine.REG_D);
        checkRegister(EmulatorEngine.REG_E, 0xFF);
        checkNotFlags(FLAG_S_Z_AC_P_C);

        stepAndCheck(0x11, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0xFF);
        checkNotFlags(FLAG_S_Z_AC_P_C);

        cpu.step();
        assertEquals(0x0F, cpu.getEngine().SP);
        checkNotFlags(FLAG_S_Z_AC_P_C);
    }

    @Test
    public void testDAD() throws Exception {
        resetProgram(0x09, 0x19, 0x29, 0x39);

        setRegisters(0x12, 0x34, 0x56, 0x78, 0x90, 0xAB, 0xCD);

        stepAndCheck(0xE0, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0x23);
        checkNotFlags(EmulatorEngine.FLAG_C);

        stepAndCheck(0x58, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0xB3);
        checkFlags(EmulatorEngine.FLAG_C);

        stepAndCheck(0xB1, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0x66);
        checkNotFlags(EmulatorEngine.FLAG_C);

        cpu.getEngine().SP = 0x4E9A;
        stepAndCheck(0, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0);
        checkFlags(EmulatorEngine.FLAG_C);
        checkNotFlags(EmulatorEngine.FLAG_Z);
    }
}
