/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.cpu.zilogZ80.suite.TimingMemoryStub;
import org.junit.Before;
import org.junit.Test;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static org.junit.Assert.*;

public class SpectrumRegressionTest {
    private TimingMemoryStub memory;
    private EmulatorEngine engine;

    @Before
    public void setUp() {
        memory = new TimingMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.setMemory(new byte[65536]);
        ContextZ80Impl context = new ContextZ80Impl();
        context.addPassedCyclesListener(memory);
        engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);
    }

    @Test
    public void indexedBitFlagsUseEffectiveAddressHighByteIncludingPageWrap() throws Exception {
        int[][] cases = {{0x1FFF, 1, 0x2000}, {0x2800, -1, 0x27FF},
                {0xFFFF, 1, 0}, {0, -1, 0xFFFF}};
        for (int prefix : new int[]{0xDD, 0xFD}) {
            for (int[] address : cases) {
                for (int bit = 0; bit < 8; bit++) {
                    for (int alias = 0; alias < 8; alias++) {
                        for (int value : new int[]{0, 0xFF}) {
                            engine.reset(0x8000);
                            memory.clearCounters();
                            engine.IX = engine.IY = address[0];
                            engine.flags = FLAG_C | FLAG_N;
                            int[] registers = engine.regs.clone();
                            memory.write(address[2], (byte) value);
                            memory.write(0x8000, new Byte[]{(byte) prefix, (byte) 0xCB,
                                    (byte) address[1], (byte) (0x40 | bit << 3 | alias)}, 4);
                            engine.step();
                            int expected = FLAG_C | FLAG_H | ((address[2] >>> 8) & (FLAG_X | FLAG_Y))
                                    | (value == 0 ? FLAG_Z | FLAG_PV : bit == 7 ? FLAG_S : 0);
                            assertEquals(expected, engine.flags);
                            assertEquals(address[2], engine.memptr);
                            assertEquals(value, memory.read(address[2]) & 0xFF);
                            assertArrayEquals(registers, engine.regs);
                            assertEquals(2, engine.R);
                            assertEquals(20, memory.getTotalCycles());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void specialRegisterLoadsKeepOldIROnBusDuringInternalCycle() throws Exception {
        for (int opcode : new int[]{0x47, 0x4F, 0x57, 0x5F}) {
            prepare(opcode, 1);
            engine.regs[REG_A] = 0xA5;
            engine.step();
            assertEquals(1, memory.getPassiveCycleCount(0x7F80));
            assertEquals(0, memory.getPassiveCycleCount(0xA580));
            assertEquals(9, memory.getTotalCycles());
            if (opcode == 0x47) assertEquals(0xA5, engine.I);
            if (opcode == 0x4F) assertEquals(0xA5, engine.R);
            if (opcode == 0x57) assertEquals(0x7F, engine.regs[REG_A]);
            if (opcode == 0x5F) assertEquals(0x80, engine.regs[REG_A]);
        }
    }

    @Test
    public void blockIOUsesIRAndRepeatsWithCorrectBusAddress() throws Exception {
        for (int opcode : new int[]{0xA2, 0xAA, 0xB2, 0xBA, 0xA3, 0xAB, 0xB3, 0xBB}) {
            for (int b : new int[]{1, 0x41}) {
                prepare(opcode, b);
                engine.step();
                assertEquals(1, memory.getPassiveCycleCount(0x7F80));
                boolean repeat = opcode >= 0xB0 && b != 1;
                int repeatAddress = (opcode & 1) == 0 ? 0x7000 : 0x40FE;
                assertEquals(repeat ? 5 : 0, memory.getPassiveCycleCount(repeatAddress));
                assertEquals(repeat ? 21 : 16, memory.getTotalCycles());
            }
        }
    }

    private void prepare(int opcode, int b) {
        engine.reset(0x8000);
        memory.clearCounters();
        engine.I = 0x7F;
        engine.R = 0xFE;
        engine.regs[REG_B] = b;
        engine.regs[REG_C] = 0xFE;
        engine.regs[REG_H] = 0x70;
        engine.regs[REG_L] = 0;
        memory.write(0x7000, (byte) 0);
        memory.write(0x8000, new Byte[]{(byte) 0xED, (byte) opcode}, 2);
    }
}
