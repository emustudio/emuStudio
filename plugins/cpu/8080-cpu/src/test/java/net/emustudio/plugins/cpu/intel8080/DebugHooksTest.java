/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

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
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
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
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
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
        assertEquals(1, events.get(0).sequence);
        assertEquals(2, events.get(1).sequence);
        assertEquals(0, events.get(0).address);
        assertEquals(2, events.get(1).address);
        assertEquals("3E 42", events.get(0).opcodes.trim());
        assertEquals("32 00 01", events.get(1).opcodes.trim());
        assertFalse(events.get(0).mnemonic.isBlank());
        assertEquals(Long.valueOf(0x42), events.get(0).after.get("A"));
        assertTrue(
                events.get(1).accesses.stream()
                        .anyMatch(a -> a.write && a.address == 0x100 && a.value == 0x42));
        engine.debugger.memory().read(0);
        assertEquals(2, events.size());
        assertEquals(0x42, memory.read(0x100) & 255);
    }

    @Test
    public void historyPreservesLoopIterationsAndExecutionTimeCodeAcrossReset() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x3c, (byte) 0xc3, 0x00, 0x00}, 4);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        List<DebugCPU.Instruction> events = collect(engine);
        engine.step();
        engine.step();
        engine.step();
        assertEquals(3, events.size());
        assertEquals(0, events.get(0).address);
        assertEquals(1, events.get(1).address);
        assertEquals(0, events.get(2).address);
        assertEquals(Long.valueOf(1), events.get(0).after.get("A"));
        assertEquals(Long.valueOf(2), events.get(2).after.get("A"));
        assertEquals("3C", events.get(0).opcodes.trim());
        memory.write(0, (byte) 0);
        engine.reset(0);
        engine.step();
        assertEquals(4, events.get(3).sequence);
        assertEquals("00", events.get(3).opcodes.trim());
        assertEquals("3C", events.get(0).opcodes.trim());
    }

    @Test
    public void interruptRecordsInjectedInstructionInsteadOfOldPcCode() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x3c);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        List<DebugCPU.Instruction> events = collect(engine);
        engine.SP = 0x100;
        engine.INTE = true;
        engine.interrupt((short) 0xcf, (short) 0, (short) 0);
        engine.step();
        assertEquals(1, events.size());
        assertEquals("INT RST", events.get(0).mnemonic);
        assertEquals("CF", events.get(0).opcodes);
        assertEquals(Long.valueOf(8), events.get(0).after.get("PC"));
        assertEquals(Long.valueOf(0), events.get(0).after.get("A"));
    }

    private static List<DebugCPU.Instruction> collect(EmulatorEngine engine) {
        List<DebugCPU.Instruction> events = new ArrayList<>();
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean afterInstruction(DebugCPU.Instruction event) {
                events.add(event);
                return false;
            }
        });
        return events;
    }

    @Test
    public void canStopBeforeInstructionWithoutMutatingRegisters() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x3c);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        engine.debugger.addListener(
                new DebugCPU.Listener() {
                    @Override
                    public boolean beforeInstruction(int pc) {
                        return true;
                    }
                });
        engine.step();
        assertEquals(0, engine.PC);
        assertEquals(0, engine.regs[7]);
    }
}
