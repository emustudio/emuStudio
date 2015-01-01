package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

public class InstructionsArithmeticTest extends InstructionsTest {

    @Test
    public void testADD() throws Exception {
        resetProgram(
                0x87, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85
        );

        setRegisters(1, 2, 3, 4, 5, 6, 7);
        stepAndCheckRegister(EmulatorEngine.REG_A, 2, 4, 7, 11, 16, 22, 29);
    }

    @Test
    public void testADD_Boundaries() throws Exception {
        resetProgram(
                0x87, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85
        );

        setRegisters(0xFF, 1, 1, 0xFF, 3, 4, 0xFF-5);

        stepAndCheck(0xFE, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_AC);
        checkNotFlags(EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_Z);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P);
        checkNotFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_Z);

        stepAndCheck(0, EmulatorEngine.REG_A);
        checkFlags(
                EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_AC
        );
        checkNotFlags(EmulatorEngine.FLAG_S);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P);
        checkNotFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_Z);

        stepAndCheck(2, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC);
        checkNotFlags(EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_P);

        stepAndCheck(6, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_P);
        checkNotFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z);

        stepAndCheck(0, EmulatorEngine.REG_A);
        checkFlags(
                EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_Z
        );
        checkNotFlags(EmulatorEngine.FLAG_S);
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

        stepAndCheck(0, EmulatorEngine.REG_A);
        checkFlags(EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC);
        checkNotFlags(EmulatorEngine.FLAG_S);
    }

    @Test
    public void testADC() throws Exception {
        resetProgram(
                0x8E,
                2 // address 1
        );

        setFlags(EmulatorEngine.FLAG_C);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheck(3, EmulatorEngine.REG_A);
        checkNotFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_Z |
                        EmulatorEngine.FLAG_AC
        );
    }

    @Test
    public void testSUB() throws Exception {
        resetProgram(
                0x96,
                1 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC
                        | EmulatorEngine.FLAG_P
        );
        checkNotFlags(EmulatorEngine.FLAG_Z);
    }

    @Test
    public void testSBB() throws Exception {
        resetProgram(
                0x9E,
                1 // address 1
        );

        setRegister(EmulatorEngine.REG_A, 2);
        setFlags(EmulatorEngine.FLAG_C);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheck(0, EmulatorEngine.REG_A);
        checkNotFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC
        );
        checkFlags(EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_P);
    }

    @Test
    public void testINR() throws Exception {
        resetProgram(
                0x34,
                0xFF // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckMemory(0, 1);
        checkFlags(
                EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC
                        | EmulatorEngine.FLAG_P
        );
        checkNotFlags(EmulatorEngine.FLAG_S);
    }

    @Test
    public void testDCR() throws Exception {
        resetProgram(
                0x35,
                0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckMemory(0xFF, 1);
        checkFlags(
                EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC
                        | EmulatorEngine.FLAG_P
        );
        checkNotFlags(EmulatorEngine.FLAG_Z);
    }

}
