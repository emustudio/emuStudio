/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import static org.junit.Assert.*;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.runtime.helpers.NumberUtils;

import org.junit.Test;

import java.util.*;

public class DebugHooksTest {
    @Test
    public void disabledProbeKeepsOriginalMemoryReference() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        java.lang.reflect.Field field = EmulatorEngine.class.getDeclaredField("memory");
        field.setAccessible(true);
        assertSame(memory, field.get(engine));
        DebugCPU.Listener listener = new DebugCPU.Listener() {};
        engine.debugger.addListener(listener);
        assertNotSame(memory, field.get(engine));
        engine.debugger.removeListener(listener);
        assertSame(memory, field.get(engine));
        assertFalse(engine.debugger.isActive());
    }

    @Test
    public void realInstructionsReportGuestWritesAndRegisterChanges() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x3e, 0x42, 0x32, 0x00, 0x01}, 5);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        List<DebugCPU.Instruction> events = new ArrayList<>();
        engine.debugger.addListener(
                new DebugCPU.Listener() {
                    @Override
                    public boolean afterInstruction(DebugCPU.Instruction e) {
                        events.add(e);
                        return false;
                    }
                });
        engine.step();
        engine.step();
        assertEquals(2, events.size());
        assertEquals(Long.valueOf(0x42), events.get(0).after.get("A"));
        assertTrue(
                events.get(1).accesses.stream()
                        .anyMatch(a -> a.write && a.address == 0x100 && a.value == 0x42));
        engine.debugger.memory().read(0);
        assertEquals(2, events.size());
        assertEquals(0x42, memory.read(0x100) & 255);
    }

    @Test
    public void canStopBeforeInstructionWithoutMutatingRegisters() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x3c);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.debugger.addListener(
                new DebugCPU.Listener() {
                    @Override
                    public boolean beforeInstruction(int pc, int bank) {
                        return true;
                    }
                });
        engine.step();
        assertEquals(0, engine.PC);
        assertEquals(0, engine.regs[7]);
    }
}
