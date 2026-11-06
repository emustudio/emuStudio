/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import org.junit.Test;
import java.util.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DebugHooksTest {
    @Test public void recordingDisablesGroupingAndCapturesRawDataWrites() {
        MemoryStub memory = new MemoryStub();
        memory.setProgram(new byte[] {3, 3, 0}); // ++;
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        Profiler profiler = new Profiler(memory);
        EmulatorEngine engine = new EmulatorEngine(memory, context, profiler);
        engine.reset(0);
        int data = engine.P;
        assertFalse(engine.debugger.isActive());
        List<DebugCPU.Instruction> events = new ArrayList<>();
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean afterInstruction(DebugCPU.Instruction event) {
                events.add(event);
                return false;
            }
        });
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step(true));
        assertEquals(1, engine.IP); // Optimizer otherwise groups both increments.
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step(true));
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step(true));
        assertEquals(3, events.size());
        assertEquals(0, events.get(0).address);
        assertEquals(1, events.get(1).address);
        assertFalse(events.get(0).mnemonic.isBlank());
        assertFalse(events.get(0).opcodes.isBlank());
        assertTrue(events.get(0).accesses.stream().anyMatch(a -> a.write && a.address == data && a.value == 1));
        assertTrue(events.get(1).accesses.stream().anyMatch(a -> a.write && a.address == data && a.value == 2));
        assertEquals(2, memory.read(data).intValue());
    }

    @Test public void listenerCanStopBeforeAndAfterWithoutLosingNextInstruction() {
        MemoryStub memory = new MemoryStub();
        memory.setProgram(new byte[] {3, 0});
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        EmulatorEngine engine = new EmulatorEngine(memory, context, new Profiler(memory));
        engine.reset(0);
        DebugCPU.Listener before = new DebugCPU.Listener() {
            @Override public boolean beforeInstruction(int address) { return true; }
        };
        engine.debugger.addListener(before);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step(true));
        assertEquals(0, engine.IP);
        assertTrue(engine.debugStopped);
        engine.debugger.removeListener(before);
        assertFalse(engine.debugger.isActive());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step(false));
        assertEquals(1, engine.IP);
        assertFalse(engine.debugStopped);
    }
    @Test public void repeatedAddressUsesCurrentCodeAfterMutationAndReset() {
        MemoryStub memory = new MemoryStub();
        memory.setProgram(new byte[] {3, 0}); // +;
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        EmulatorEngine engine = new EmulatorEngine(memory, context, new Profiler(memory));
        List<DebugCPU.Instruction> events = new ArrayList<>();
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean afterInstruction(DebugCPU.Instruction event) {
                events.add(event);
                return false;
            }
        });
        engine.reset(0);
        engine.step(false);
        memory.write(0, (byte) 1); // Replace + with > at same address.
        engine.reset(0);
        engine.step(false);
        assertNotEquals(events.get(0).mnemonic, events.get(1).mnemonic);
        assertNotEquals(events.get(0).opcodes, events.get(1).opcodes);
        assertEquals(2, events.get(1).sequence);
        assertEquals(0, events.get(1).address);
    }
    @Test public void selectedBankDoesNotRelabelRawBankZeroExecution() {
        MemoryStub memory = new MemoryStub() {
            { memory = new Byte[2][1000]; java.util.Arrays.fill(memory[0], (byte) 0); java.util.Arrays.fill(memory[1], (byte) 0); }
            @Override public int getBanksCount() { return 2; }
            @Override public int getSelectedBank() { return 1; }
            @Override public int getCommonBoundary() { return 1000; }
            @Override public Byte read(int address) { return memory[1][address]; }
            @Override public Byte[] read(int address, int count) {
                return java.util.Arrays.copyOfRange(memory[1], address, address + count);
            }
        };
        memory.getRawMemory()[0][0] = 3; // + executes from raw bank zero.
        memory.getRawMemory()[1][0] = 1; // > is visible through selected-bank reads.
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        EmulatorEngine engine = new EmulatorEngine(memory, context, new Profiler(memory));
        engine.reset(0);
        List<DebugCPU.Instruction> events = new ArrayList<>();
        List<Integer> beforeBanks = new ArrayList<>();
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean beforeInstruction(int address) { beforeBanks.add(address); return false; }
            @Override public boolean afterInstruction(DebugCPU.Instruction event) { events.add(event); return false; }
        });
        engine.step(false);
        assertEquals(List.of(0), beforeBanks);

        assertEquals("03", events.get(0).opcodes.trim());
        assertEquals(1, memory.getRawMemory()[0][engine.P].intValue());
        assertSame(memory, engine.debugger.memory());
    }
}
