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
    public void runStopsAtApplicationBreakpointBeforeExecutingInstruction() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x3e, 0x42, 0x3c}, 3);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        DebugCPU.Listener listener = new DebugCPU.Listener() {
            @Override public boolean beforeInstruction(int address) { return address == 2; }
        };
        engine.debugger.addListener(listener);

        assertEquals(net.emustudio.emulib.plugins.cpu.CPU.RunState.STATE_STOPPED_BREAK, engine.run());

        assertEquals(2, engine.PC);
        assertEquals(0x42, engine.regs[7]);
        engine.debugger.removeListener(listener);
        engine.step();
        assertEquals(3, engine.PC);
        assertEquals(0x43, engine.regs[7]);
    }

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
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
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
    public void indexedPrefixCapturesWholeOpcodeAndAlternateRegisters() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {(byte) 0xdd, 0x21, 0x34, 0x12}, 4);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.regs2[7] = 42;
        engine.flags2 = 0x28;
        List<DebugCPU.Instruction> events = collect(engine);
        engine.step();
        assertEquals(1, events.size());
        assertEquals("DD 21 34 12", events.get(0).opcodes.trim());
        assertEquals(Long.valueOf(0x1234), events.get(0).after.get("IX"));
        assertEquals(Long.valueOf(42), events.get(0).after.get("A2"));
        assertEquals(Long.valueOf(0x28), events.get(0).after.get("F2"));
    }

    @Test
    public void interruptRecordsAcknowledgementAndActualIsrInstruction() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x3c);
        memory.write(0x38, new Byte[] {0x3e, 0x42}, 2);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.SP = 0x100;
        engine.interruptMode = 1;
        engine.IFF[0] = true;
        requestInterrupt(engine, new byte[] {(byte) 0xff});
        List<DebugCPU.Instruction> events = collect(engine);
        engine.step();
        assertEquals(1, events.size());
        assertTrue(events.get(0).mnemonic.startsWith("INT acknowledge; "));
        assertEquals("3E 42", events.get(0).opcodes.trim());
        assertEquals(0x38, events.get(0).address);
        assertEquals(Long.valueOf(0), events.get(0).before.get("PC"));
        assertEquals(Long.valueOf(0x3a), events.get(0).after.get("PC"));
        assertEquals(Long.valueOf(0x42), events.get(0).after.get("A"));
        engine.requestNonMaskableInterrupt();
        engine.step();
        assertEquals("NMI acknowledge", events.get(1).mnemonic);
        assertEquals("", events.get(1).opcodes);
        assertEquals(Long.valueOf(0x66), events.get(1).after.get("PC"));
    }

    @Test
    public void im2RecordingDoesNotAddMemoryContentionOrGuestReads() throws Exception {
        long[] ordinary = executeContendedIm2(false);
        long[] recorded = executeContendedIm2(true);
        assertEquals(ordinary[0], recorded[0]);
        assertEquals(ordinary[1], recorded[1]);
        assertTrue(ordinary[0] > 0);
    }

    private static long[] executeContendedIm2(boolean record) throws Exception {
        ContendedMemory memory = new ContendedMemory();
        memory.write(0, (byte) 0);
        memory.write(0x100, new Byte[] {0x00, 0x02}, 2);
        memory.write(0x200, new Byte[] {0x3e, 0x42}, 2);
        ContextZ80Impl context = new ContextZ80Impl();
        long[] cycles = {0};
        context.addPassedCyclesListener(delta -> cycles[0] += delta);
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        memory.engine = engine;
        engine.SP = 0x300;
        engine.I = 1;
        engine.IFF[0] = true;
        engine.interruptMode = 2;
        List<DebugCPU.Instruction> events = record ? collect(engine) : List.of();
        requestInterrupt(engine, new byte[] {0});
        engine.step();
        assertEquals(0x202, engine.PC);
        assertEquals(0x42, engine.regs[7]);
        if (record) {
            assertEquals(1, events.size());
            assertEquals(0x200, events.get(0).address);

            assertEquals("3E 42", events.get(0).opcodes.trim());
            assertEquals(Long.valueOf(0), events.get(0).before.get("PC"));
        }
        return new long[] {cycles[0], memory.guestReads};
    }

    private static void requestInterrupt(EmulatorEngine engine, byte[] data) throws Exception {
        java.lang.reflect.Field state = EmulatorEngine.class.getDeclaredField("currentRunState");
        state.setAccessible(true);
        state.set(engine, net.emustudio.emulib.plugins.cpu.CPU.RunState.STATE_RUNNING);
        engine.requestMaskableInterrupt(data);
    }

    /** Like the Spectrum bus, guest reads add cycles and inspection reads bypass contention. */
    private static final class ContendedMemory extends ByteMemoryStub {
        EmulatorEngine engine;
        int guestReads;
        ContendedMemory() { super(NumberUtils.Strategy.LITTLE_ENDIAN); }
        @Override public Byte peek(int at) { return super.read(at); }
        @Override public Byte read(int at) {
            guestReads++;
            if (engine != null) engine.addExecutedCyclesPerTimeSlice(2);
            return super.read(at);
        }
        @Override public Byte[] read(int at, int count) {
            Byte[] values = new Byte[count];
            for (int i = 0; i < count; i++) values[i] = read(at + i);
            return values;
        }
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
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
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
