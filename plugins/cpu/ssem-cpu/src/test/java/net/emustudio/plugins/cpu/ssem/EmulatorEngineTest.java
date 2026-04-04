/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.cpu.testsuite.memory.MemoryStub;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmulatorEngineTest {
    private EmulatorEngine engine;
    private MemoryStub<Byte> memoryStub;

    @Before
    public void setUp() {
        memoryStub = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        engine = new EmulatorEngine(memoryStub, p -> false);
    }

    @Test
    public void testAddition() {
        /*
            01: LDN 29  -- A = -X
            02: SUB 30  -- A = -X - Y
            03: STO 31  -- store -Sum
            04: LDN 31  -- A = -(-Sum)
            05: STO 31  -- store Sum
            06: HLT

            29: NUM 5   -- X Parameter
            30: NUM 3   -- Y Parameter
            31:         -- Sum Result will appear here
        */
        memoryStub.setMemory(new short[]{
                0, 0, 0, 0,
                0xB8, 0x02, 0, 0,
                0x78, 0x01, 0, 0,
                0xF8, 0x06, 0, 0,
                0xF8, 0x02, 0, 0,
                0xF8, 0x06, 0, 0,
                0x00, 0x07, 0, 0,

                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0xA0, 0, 0, 0, 0xC0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        });

        engine.reset(0);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.run());
        assertEquals(8, NumberUtils.readInt(memoryStub.read(31 * 4, 4), memoryStub.getWordReadingStrategy()));
    }

    @Test(timeout = 700)
    public void testEndlessLoopPrevention() {
        // instruction 0 is JMP
        memoryStub.setMemory(new short[32 * 4]);
        engine.reset(0);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.run());
    }

    @Test
    public void testHaltInstruction() {
        // STP (opcode 7) at line 0
        memoryStub.setMemory(new short[32 * 4]);
        memoryStub.write(0, (byte) 0);
        memoryStub.write(1, (byte) 0x07); // opcode 7 = STP
        engine.reset(-4); // CI starts at -4, will be incremented to 0
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
    }

    @Test
    public void testResetSetsAccAndCI() {
        engine.reset(10);
        assertEquals(0, engine.Acc.get());
        assertEquals(10, engine.CI.get());
    }

    @Test
    public void testStepWithBadInstruction() {
        // opcode 5 is not defined (DISPATCH_TABLE[5] = null)
        memoryStub.setMemory(new short[32 * 4]);
        memoryStub.write(0, (byte) 0);
        memoryStub.write(1, (byte) 0x05); // opcode 5 = undefined
        engine.reset(-4);
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
    }

    @Test
    public void testCmpWhenAccNegative() {
        // CMP (opcode 3): if ACC < 0, skip next instruction
        memoryStub.setMemory(new short[32 * 4]);
        // line 0: CMP (opcode 3)
        memoryStub.write(0, (byte) 0);
        memoryStub.write(1, (byte) 0x03); // CMP
        // line 1: STP (opcode 7)
        memoryStub.write(4, (byte) 0);
        memoryStub.write(5, (byte) 0x07); // STP
        // line 2: STP (opcode 7)
        memoryStub.write(8, (byte) 0);
        memoryStub.write(9, (byte) 0x07); // STP

        engine.reset(-4);
        engine.Acc.set(-1);
        CPU.RunState state = engine.step(); // CMP: ACC < 0, skip next
        assertEquals(CPU.RunState.STATE_RUNNING, state);
        // CI should have been incremented by 4 (skipping line 1)
        assertEquals(4, engine.CI.get());
    }

    @Test
    public void testCmpWhenAccPositive() {
        memoryStub.setMemory(new short[32 * 4]);
        memoryStub.write(0, (byte) 0);
        memoryStub.write(1, (byte) 0x03); // CMP
        memoryStub.write(4, (byte) 0);
        memoryStub.write(5, (byte) 0x07); // STP

        engine.reset(-4);
        engine.Acc.set(5);
        CPU.RunState state = engine.step(); // CMP: ACC >= 0, no skip
        assertEquals(CPU.RunState.STATE_RUNNING, state);
        assertEquals(0, engine.CI.get());
    }
}
