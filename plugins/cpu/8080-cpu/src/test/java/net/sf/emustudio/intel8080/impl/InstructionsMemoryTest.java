package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

import static net.sf.emustudio.intel8080.impl.Utils.concat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstructionsMemoryTest extends InstructionsTest {

    @Test
    public void testMOVtoGeneralRegisters() throws Exception {
        short[] values = new short[] { 0,0,0,0,0,0,22 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x7E, 0x46, 0x4E, 0x56, 0x5E, 0x66, 0x6E,0,
                0xFF // address 22
        });

        resetProgram();
        stepCount(7);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        stepAndCheck(0xFF, EmulatorEngine.REG_B);
        stepAndCheck(0xFF, EmulatorEngine.REG_C);
        stepAndCheck(0xFF, EmulatorEngine.REG_D);
        stepAndCheck(0xFF, EmulatorEngine.REG_E);
        stepAndCheck(0xFF, EmulatorEngine.REG_H);
        cpu.getEngine().regs[EmulatorEngine.REG_H] = 0; // hack
        stepAndCheck(0xFF, EmulatorEngine.REG_L);
    }

    @Test
    public void testMOVgeneralRegistersToMemory() throws Exception {
        short[] values = new short[] { 1,2,3,4,5,0,22 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x77, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75,0,
                0xFF // address 22
        });

        resetProgram();
        stepCount(7);

        stepAndCheckMemory(1, 22);
        stepAndCheckMemory(2, 22);
        stepAndCheckMemory(3, 22);
        stepAndCheckMemory(4, 22);
        stepAndCheckMemory(5, 22);
        stepAndCheckMemory(0, 22);
        stepAndCheckMemory(22, 22);
    }

    @Test
    public void testLDAX() throws Exception {
        short[] values = new short[] { 0,0,17,0,17,0,0 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x0A, 0x1A, 0,
                0xFF // address 17
        });
        resetProgram();
        stepCount(7);

        stepAndCheck(0xFF, EmulatorEngine.REG_A);

        cpu.getEngine().regs[EmulatorEngine.REG_A] = 0;
        stepAndCheck(0xFF, EmulatorEngine.REG_A);
    }

    @Test
    public void testSTAX() throws Exception {
        short[] values = new short[] { 0xFF,0,17,0,18,0,0 };
        program = concat(fillGeneralRegisters(values), new short[]{
                0x02, 0x12, 0,
                0, // address 17
                0  // address 18
        });
        resetProgram();
        stepCount(7);

        stepAndCheckMemory(0xFF, 17);
        stepAndCheckMemory(0xFF, 18);
    }

    @Test
    public void testLDA() throws Exception {
        program = new short[] {
                0x3A, 5, 0,
                0, 0, 0xFF //address 5
        };
        resetProgram();

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
    }

    @Test
    public void testSTA() throws Exception {
        program = new short[] {
                0x32, 5, 0,
                0, 0, 0xFF //address 5
        };
        resetProgram();
        cpu.getEngine().regs[EmulatorEngine.REG_A] = 0xAB;

        stepAndCheckMemory(0xAB, 5);
    }

    @Test
    public void testMVI() throws Exception {
        program = new short[] {
                0x36, 0xAB, 0,
                0, 0, 0xFF //address 5
        };
        resetProgram();
        cpu.getEngine().regs[EmulatorEngine.REG_L] = 5;

        stepAndCheckMemory(0xAB, 5);
    }

    @Test
    public void testLHLD() throws Exception {
        program = new short[] {
                0x2A, 5, 0,
                0, 0,
                0xAB, //address 5
                0xCD  //address 6
        };
        resetProgram();

        stepAndCheck(0xCD, EmulatorEngine.REG_H);
        stepAndCheck(0xAB, EmulatorEngine.REG_L);
    }

    @Test
    public void testSHLD() throws Exception {
        program = new short[] {
                0x22, 5, 0,
                0, 0,
                0xFF, //address 5
                0xFF  //address 6
        };
        resetProgram();
        cpu.getEngine().regs[EmulatorEngine.REG_H] = 0xCD;
        cpu.getEngine().regs[EmulatorEngine.REG_L] = 0xAB;

        stepAndCheckMemory(0xAB, 5);
        stepAndCheckMemory(0xCD, 6);
    }

    @Test
    public void testXTHL() throws Exception {
        program = new short[] {
                0xE3,
                0xAB, // address 1
                0xCD  // address 2
        };
        resetProgram();
        cpu.getEngine().SP = 1;
        cpu.getEngine().regs[EmulatorEngine.REG_H] = 0x34;
        cpu.getEngine().regs[EmulatorEngine.REG_L] = 0x12;

        stepAndCheckMemory(0x12, 1);
        assertEquals(0x34, (int)memoryStub.read(2));
        assertEquals(0xAB, cpu.getEngine().regs[EmulatorEngine.REG_L]);
        assertEquals(0xCD, cpu.getEngine().regs[EmulatorEngine.REG_H]);
    }

    @Test
    public void testADD() throws Exception {
        program = new short[] {
                0x86,
                2, // address 1
        };
        resetProgram();

        // with overflow, signed
        cpu.getEngine().regs[EmulatorEngine.REG_A] = 0xFE;
        cpu.getEngine().flags |= EmulatorEngine.FLAG_S;

        cpu.getEngine().regs[EmulatorEngine.REG_H] = 0;
        cpu.getEngine().regs[EmulatorEngine.REG_L] = 1;

        stepAndCheck(0, EmulatorEngine.REG_A);

        assertFalse((cpu.getEngine().flags & EmulatorEngine.FLAG_C) == 0);
        assertFalse((cpu.getEngine().flags & EmulatorEngine.FLAG_Z) == 0);
        assertTrue((cpu.getEngine().flags & EmulatorEngine.FLAG_S) == 0);
    }

    /**
     TODO:

     ADC	M	ADC	A,(HL)	8E	A <- A + (HL) + Carry
     SUB	M	SUB	(HL)	96	A <- A - (HL)
     SBB	M	SBC	(HL)	9E	A <- A - (HL) - Carry
     INR	M	INC	(HL)	34	(HL) <- (HL) + 1
     DCR	M	DEC	(HL)	35	(HL) <- (HL) - 1
     ANA	M	AND	(HL)	A6	A <- A AND (HL)
     XRA	M	XOR	(HL)	AE	A <- A XOR (HL)
     ORA	M	OR	(HL)	B6	A <- A OR (HL)
     CMP	M	CP	(HL)	BE	A - (HL)

     */

}
