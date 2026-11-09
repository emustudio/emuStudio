/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.Numbers;
import net.emustudio.plugins.cpu.zilogZ80.gui.DecoderImpl;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.Assert.*;

public class RuntimeFlowTest {
    @Test
    public void im0ObserverCanStopAfterFirstInstructionAndResumeWithoutDuplicateCapture() throws Exception {
        ByteMemoryStub memory = memory(0x3c, 0x00);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        requestIm0Interrupt(engine);
        List<Integer> addresses = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }
            @Override public boolean afterStep(boolean completed, Optional<DecodedInstruction> instruction,
                                                OptionalInt nextAddress, Optional<StepView> capture) {
                assertTrue(completed);
                assertTrue(capture.isEmpty());
                addresses.add(instruction.orElseThrow().address());
                destinations.add(nextAddress.orElseThrow());
                return addresses.size() == 1;
            }
        }, Debugging.Capture.INSTRUCTION);

        engine.step();

        assertEquals(1, engine.PC);
        assertEquals(1, engine.regs[7]);
        assertEquals(List.of(0), addresses);
        assertEquals(List.of(1), destinations);

        engine.step();

        assertEquals(2, engine.PC);
        assertEquals(List.of(0, 1), addresses);
        assertEquals(List.of(1, 2), destinations);
    }

    @Test
    public void im0ObserverCanRefuseSecondInstructionAndResumeWithoutLeakedCapture() throws Exception {
        ByteMemoryStub memory = memory(0x3c, 0x00);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        requestIm0Interrupt(engine);
        List<Integer> addresses = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        boolean[] refused = {false};
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() {
                if (engine.PC == 1 && !refused[0]) {
                    refused[0] = true;
                    return true;
                }
                return false;
            }
            @Override public boolean afterStep(boolean completed, Optional<DecodedInstruction> instruction,
                                                OptionalInt nextAddress, Optional<StepView> capture) {
                assertTrue(completed);
                assertTrue(capture.isEmpty());
                addresses.add(instruction.orElseThrow().address());
                destinations.add(nextAddress.orElseThrow());
                return false;
            }
        }, Debugging.Capture.INSTRUCTION);

        engine.step();

        assertTrue(refused[0]);
        assertEquals(1, engine.PC);
        assertEquals(List.of(0), addresses);
        assertEquals(List.of(1), destinations);

        engine.step();

        assertEquals(2, engine.PC);
        assertEquals(1, engine.regs[7]);
        assertEquals(List.of(0, 1), addresses);
        assertEquals(List.of(1, 2), destinations);
    }

    @Test
    public void im0NonCallInterruptReportsEveryMemoryInstructionActuallyExecuted() throws Exception {
        ByteMemoryStub memory = memory(0x3c, 0x00);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.interruptMode = 0;
        engine.IFF[0] = true;
        Field runState = EmulatorEngine.class.getDeclaredField("currentRunState");
        runState.setAccessible(true);
        runState.set(engine, net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_RUNNING);
        engine.requestMaskableInterrupt(new byte[]{(byte) 0xff});
        List<DecodedInstruction> instructions = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        observe(engine, instructions, destinations);

        engine.step();

        assertEquals(1, engine.regs[7]);
        assertEquals(2, engine.PC);
        assertEquals(2, instructions.size());
        assertEquals(0, instructions.get(0).address());
        assertEquals("3C", instructions.get(0).encoding());
        assertEquals(1, instructions.get(1).address());
        assertEquals("00", instructions.get(1).encoding());
        assertEquals(List.of(1, 2), destinations);
    }

    @Test
    public void interruptOnlyBoundaryDoesNotInventMemoryInstruction() throws Exception {
        ByteMemoryStub memory = memory(0x3c);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.SP = 0x1000;
        List<Optional<DecodedInstruction>> instructions = new ArrayList<>();
        List<OptionalInt> destinations = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }
            @Override public boolean afterStep(boolean completed, Optional<DecodedInstruction> instruction,
                                                OptionalInt nextAddress, Optional<StepView> capture) {
                assertTrue(completed);
                assertTrue(capture.isEmpty());
                instructions.add(instruction);
                destinations.add(nextAddress);
                return false;
            }
        }, Debugging.Capture.INSTRUCTION);
        engine.requestNonMaskableInterrupt();

        engine.step();

        assertEquals(List.of(Optional.empty()), instructions);
        assertEquals(List.of(OptionalInt.of(0x66)), destinations);
        assertEquals(0, engine.regs[7]);
    }

    @Test
    public void interruptDispatchReportsExecutedIsrInstructionInsteadOfOldPc() throws Exception {
        ByteMemoryStub memory = memory(0x3c);
        memory.write(0x38, new Byte[]{0x3e, 0x42}, 2);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.SP = 0x1000;
        engine.interruptMode = 1;
        engine.IFF[0] = true;
        Field runState = EmulatorEngine.class.getDeclaredField("currentRunState");
        runState.setAccessible(true);
        runState.set(engine, net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_RUNNING);
        engine.requestMaskableInterrupt(new byte[]{(byte) 0xff});
        List<DecodedInstruction> instructions = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        observe(engine, instructions, destinations);

        engine.step();

        assertEquals(List.of(0x3a), destinations);
        assertEquals(0x38, instructions.get(0).address());
        assertEquals("3E 42", instructions.get(0).encoding());
        assertEquals(0x42, engine.regs[7]);
    }

    @Test
    public void conditionalJumpReportsOnlyTheOutcomeActuallyExecuted() throws Exception {
        ByteMemoryStub memory = memory(0xc2, 0x34, 0x12);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        List<DecodedInstruction> instructions = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        observe(engine, instructions, destinations);
        engine.flags = 0x40;

        engine.step();
        engine.PC = 0;
        engine.flags = 0;
        engine.step();

        assertEquals(List.of(3, 0x1234), destinations);
        assertEquals(2, instructions.size());
        assertEquals(0, instructions.get(0).address());
        assertEquals("C2 34 12", instructions.get(0).encoding());
    }

    @Test
    public void callAndReturnReportActualNextAddresses() throws Exception {
        ByteMemoryStub memory = memory(0xcd, 0x34, 0x12);
        memory.write(0x1234, (byte) 0xc9);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.SP = 0x1000;
        List<DecodedInstruction> instructions = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        observe(engine, instructions, destinations);

        engine.step();
        engine.step();

        assertEquals(List.of(0x1234, 3), destinations);
        assertEquals(0, instructions.get(0).address());
        assertEquals(3, instructions.get(0).length());
        assertEquals(0x1234, instructions.get(1).address());
        assertEquals("C9", instructions.get(1).encoding());
    }

    @Test
    public void indirectTransferUsesExecutedRegisterValue() throws Exception {
        ByteMemoryStub memory = memory(0xe9);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.regs[4] = 0x12;
        engine.regs[5] = 0x34;
        List<DecodedInstruction> instructions = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        observe(engine, instructions, destinations);

        engine.step();

        assertEquals(List.of(0x1234), destinations);
        assertEquals("E9", instructions.get(0).encoding());
    }

    @Test
    public void lightObservationKeepsExecutionMemoryAndSkipsFullPresentation() throws Exception {
        ByteMemoryStub memory = memory(0);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.debugger.register("BROKEN", 8, () -> { throw new AssertionError("Full presentation requested"); }, value -> {});
        List<DecodedInstruction> instructions = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        observe(engine, instructions, destinations);
        Field executionMemory = EmulatorEngine.class.getDeclaredField("memory");
        executionMemory.setAccessible(true);

        engine.step();

        assertSame(memory, executionMemory.get(engine));
        assertEquals(List.of(1), destinations);
        assertEquals("00", instructions.get(0).encoding());
    }

    @Test
    public void capturesInstructionBeforeSelfModifyingWrite() throws Exception {
        ByteMemoryStub memory = memory(0x32, 0, 0);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.regs[7] = 0;
        List<DecodedInstruction> instructions = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        observe(engine, instructions, destinations);

        engine.step();

        assertEquals(0, memory.read(0).intValue());
        assertEquals("32 00 00", instructions.get(0).encoding());
        assertEquals(List.of(3), destinations);
    }

    @Test
    public void cpuWiringCapturesInstructionAcrossSixteenBitAddressWrap() throws Exception {
        ByteMemoryStub memory = memory(0x34, 0x12);
        memory.write(65535, (byte) 0xc3);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.PC = 65535;
        List<DecodedInstruction> instructions = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        observe(engine, instructions, destinations);

        engine.step();

        assertEquals(List.of(0x1234), destinations);
        assertEquals(65535, instructions.get(0).address());
        assertEquals(3, instructions.get(0).length());
        assertEquals("C3 34 12", instructions.get(0).encoding());
    }

    @Test
    public void decodeUsesCurrentMappedCellsAndKeepsEarlierCaptureStable() throws Exception {
        byte[][] cells = {{0, 0x34, 0}, {(byte) 0xcd, 0, 0x12}};
        int[] wiring = {1, 0, 1};
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN) {
            @Override public Byte read(int address) { return cells[wiring[address]][address]; }
            @Override public Byte[] read(int address, int count) {
                Byte[] values = new Byte[count];
                for (int i = 0; i < count; i++) values[i] = read(address + i);
                return values;
            }
        };
        Decoder decoder = new DecoderImpl(memory);
        DecodedInstruction before = decoder.decode(0);
        cells[1][1] = 0x78;
        wiring[1] = 1;

        DecodedInstruction after = decoder.decode(0);

        assertNotEquals(before.mnemonic(), after.mnemonic());
        assertEquals("CD 34 12", before.encoding());
        assertEquals("CD 78 12", after.encoding());
        assertEquals(3, before.length());
        assertEquals(3, after.length());
    }

    private static ByteMemoryStub memory(int... bytes) {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.setMemory(new byte[65536]);
        for (int i = 0; i < bytes.length; i++) memory.write(i, (byte) bytes[i]);
        return memory;
    }

    private static void requestIm0Interrupt(EmulatorEngine engine) throws Exception {
        engine.interruptMode = 0;
        engine.IFF[0] = true;
        Field runState = EmulatorEngine.class.getDeclaredField("currentRunState");
        runState.setAccessible(true);
        runState.set(engine, net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_RUNNING);
        engine.requestMaskableInterrupt(new byte[]{(byte) 0xff});
    }

    private static void observe(EmulatorEngine engine, List<DecodedInstruction> instructions,
                                List<Integer> destinations) {
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }
            @Override public boolean afterStep(boolean completed, Optional<DecodedInstruction> instruction,
                                                OptionalInt nextAddress, Optional<StepView> capture) {
                assertTrue(completed);
                assertTrue(capture.isEmpty());
                instructions.add(instruction.orElseThrow());
                destinations.add(nextAddress.orElseThrow());
                return false;
            }
        }, Debugging.Capture.INSTRUCTION);
    }
}
