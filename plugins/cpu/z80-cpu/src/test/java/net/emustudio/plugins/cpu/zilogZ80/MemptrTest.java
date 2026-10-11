/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.cpu.zilogZ80.suite.TimingMemoryStub;
import org.junit.Before;
import org.junit.Test;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static org.junit.Assert.assertEquals;

public class MemptrTest {
    private TimingMemoryStub memory;
    private EmulatorEngine engine;
    private int readPort;

    @Before
    public void setUp() {
        memory = new TimingMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.setMemory(new byte[65536]);
        ContextZ80Impl context = new ContextZ80Impl();
        context.addPassedCyclesListener(memory);
        context.attachDevice(0x7F, new FakeByteDevice() {
            @Override
            public byte read(int portAddress) {
                readPort = portAddress;
                return (byte) 0xFF;
            }
        });
        engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);
    }

    @Test
    public void indirectAccumulatorLoadsSetMemptrToAddressPlusOne() throws Exception {
        for (int opcode : new int[]{0x0A, 0x1A}) {
            for (int address : new int[]{0x15FD, 0xFFFF}) {
                engine.reset(0x8000);
                memory.clearCounters();
                engine.regs[REG_B] = engine.regs[REG_D] = address >>> 8;
                engine.regs[REG_C] = engine.regs[REG_E] = address & 0xFF;
                memory.write(address, (byte) 0x42);
                memory.write(0x8000, (byte) opcode);
                engine.step();
                assertEquals((address + 1) & 0xFFFF, engine.memptr);
                assertEquals(0x42, engine.regs[REG_A]);
                assertEquals(7, memory.getTotalCycles());
            }
        }
    }

    @Test
    public void indexedWordLoadsSetMemptrToAddressPlusOne() throws Exception {
        for (int prefix : new int[]{0xDD, 0xFD}) {
            for (int address : new int[]{0x0CF8, 0xFFFF}) {
                engine.reset(0x8000);
                memory.clearCounters();
                memory.write(address, (byte) 0x34);
                memory.write((address + 1) & 0xFFFF, (byte) 0x12);
                memory.write(0x8000, (byte) prefix);
                memory.write(0x8001, (byte) 0x2A);
                memory.write(0x8002, (byte) address);
                memory.write(0x8003, (byte) (address >>> 8));
                engine.step();
                assertEquals((address + 1) & 0xFFFF, engine.memptr);
                assertEquals(0x1234, prefix == 0xDD ? engine.IX : engine.IY);
                assertEquals(20, memory.getTotalCycles());
            }
        }
    }

    @Test
    public void blockInputUsesOriginalBC() throws Exception {
        for (int opcode : new int[]{0xA2, 0xAA, 0xB2, 0xBA}) {
            prepare(opcode, 0x22, 0x8000);
            engine.step();
            assertEquals("Port for opcode " + opcode, 0x227F, readPort);
            assertEquals(0x21, engine.regs[REG_B]);
            assertEquals(0xFF, memory.read(0xCA7F) & 0xFF);
        }
    }

    @Test
    public void nonRepeatingInputMemptrUsesOriginalBC() throws Exception {
        for (int opcode : new int[]{0xA2, 0xAA}) {
            prepare(opcode, 0x22, 0x8000);
            engine.step();
            assertEquals(opcode == 0xA2 ? 0x2280 : 0x227E, engine.memptr);
            assertEquals(16, memory.getTotalCycles());
        }
    }

    @Test
    public void terminatingInputMemptrUsesOriginalBC() throws Exception {
        for (int opcode : new int[]{0xB2, 0xBA}) {
            prepare(opcode, 1, 0x8000);
            engine.step();
            assertEquals(opcode == 0xB2 ? 0x0180 : 0x017E, engine.memptr);
            assertEquals(0x8002, engine.PC);
            assertEquals(16, memory.getTotalCycles());
        }
    }

    @Test
    public void repeatingIOMemptrIsInstructionAddressPlusOne() throws Exception {
        for (int opcode : new int[]{0xB2, 0xBA, 0xB3, 0xBB}) {
            for (int pc : new int[]{0x3EFE, 0xFFFF}) {
                prepare(opcode, 0x10, pc);
                engine.step();
                assertEquals((pc + 1) & 0xFFFF, engine.memptr);
                assertEquals(pc, engine.PC);
                assertEquals(21, memory.getTotalCycles());
            }
        }
    }

    @Test
    public void terminatingOutputMemptrUsesDecrementedBC() throws Exception {
        for (int opcode : new int[]{0xB3, 0xBB}) {
            prepare(opcode, 1, 0x8000);
            engine.step();
            assertEquals(opcode == 0xB3 ? 0x0080 : 0x007E, engine.memptr);
            assertEquals(16, memory.getTotalCycles());
        }
    }

    private void prepare(int opcode, int b, int pc) {
        engine.reset(pc);
        memory.clearCounters();
        engine.regs[REG_B] = b;
        engine.regs[REG_C] = 0x7F;
        engine.regs[REG_H] = 0xCA;
        engine.regs[REG_L] = 0x7F;
        memory.write(pc, (byte) 0xED);
        memory.write((pc + 1) & 0xFFFF, (byte) opcode);
        memory.write(0xCA7F, (byte) 0x55);
    }
}
