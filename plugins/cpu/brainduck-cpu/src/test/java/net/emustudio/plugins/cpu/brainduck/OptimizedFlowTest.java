/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.ExecutedBlock;
import net.emustudio.emulib.plugin.debugger.StepView;
import org.junit.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class OptimizedFlowTest {
    @Test public void repeatsPreserveWrappingStateAndEveryInstructionEdge() {
        compare("+++++>>>---<<<----", run -> run.memory.write(run.engine.P, (byte) 254), true);
    }

    @Test public void copyAndClearPreserveZeroOneAndRepeatedLoopPaths() {
        for (String code : List.of("[-]", "[->++>---<<]", "[>++>---<<-]")) {
            for (int initial : new int[] {0, 1, 2, 127, 255}) {
                compare(code, run -> {
                    run.memory.write(run.engine.P, (byte) initial);
                    run.memory.write(run.engine.P + 1, (byte) 253);
                    run.memory.write(run.engine.P + 2, (byte) 7);
                }, true);
            }
        }
    }

    @Test public void scansPreserveZeroOneMultipleAndChunkedPaths() {
        for (int count : new int[] {0, 1, 2, 300}) {
            compare("[>]", run -> {
                for (int i = 0; i < count; i++) run.memory.write(run.engine.P + i, (byte) 1);
            }, true);
            compare("[<>>]", run -> {
                run.engine.P = 20;
                for (int i = 0; i < count; i++) run.memory.write(20 + i, (byte) 1);
            }, true);
        }
    }

    @Test public void nestedLoopsPreserveSurroundingLoopStack() {
        compare("++[>++[>+<-]<-]", run -> {}, true);
    }

    @Test public void unsafePointerPathsFallBackWithoutLosingCompletedPrefix() {
        compare(">>>", run -> run.engine.P = 998, false);
        compare("[>]", run -> {
            run.engine.P = 998;
            run.memory.write(998, (byte) 1);
            run.memory.write(999, (byte) 1);
        }, true);
        compare("[<<>>-]", run -> {
            run.engine.P = 1;
            run.memory.write(1, (byte) 1);
        }, false);
    }

    @Test public void changedCodeAndCodeWritesNeverUseStaleBlocks() {
        compare("++++", run -> run.memory.write(1, (byte) 4), false);
        Run run = new Run("++++", true, Debugging.Capture.INSTRUCTION);
        run.engine.P = 0;
        run.engine.step(true);
        assertEquals(0, run.trace.blocks);
        assertEquals(1, run.engine.IP);
        assertEquals(4, run.memory.read(0).intValue());
    }

    @Test public void instructionFactsFollowChangedBytesWithoutReset() {
        Run run = new Run("++++", true, Debugging.Capture.INSTRUCTION);
        run.memory.write(0, (byte) 4);
        run.engine.step(true);
        assertEquals(1, run.engine.IP);
        assertTrue(run.trace.instructions.get(0).endsWith(":04"));
        run.memory.write(0, (byte) 3);
        run.engine.IP = 0;
        run.engine.step(true);
        assertEquals(4, run.engine.IP);
        assertTrue(run.trace.instructions.get(0).endsWith(":03"));
    }

    @Test public void detailedCaptureAndNonOptInObserversKeepOriginalBoundaries() {
        Run full = new Run("++++", true, Debugging.Capture.FULL);
        full.engine.step(true);
        assertEquals(1, full.engine.IP);
        assertEquals(1, full.trace.captures);
        assertEquals(0, full.trace.blocks);
        Run exact = new Run("++++", false, Debugging.Capture.INSTRUCTION);
        exact.engine.step(true);
        assertEquals(1, exact.engine.IP);
        assertEquals(0, exact.trace.blocks);
    }

    @Test public void deviceOperationsKeepExactOrderAndFlow() {
        compare("+++[->+.<]>>,,..", run -> {}, true);
    }

    @Test public void zeroStrideScanRemainsInterruptibleAtOriginalBoundaries() {
        Run run = new Run("[><]", true, Debugging.Capture.INSTRUCTION);
        run.memory.write(run.engine.P, (byte) 1);
        for (int i = 0; i < 20; i++) run.engine.step(true);
        assertEquals(0, run.trace.blocks);
    }

    private static void compare(String code, Consumer<Run> prepare, boolean expectBatch) {
        Run exact = new Run(code, false, Debugging.Capture.INSTRUCTION);
        Run batched = new Run(code, true, Debugging.Capture.INSTRUCTION);
        prepare.accept(exact);
        prepare.accept(batched);
        CPU.RunState exactState = exact.finish(false);
        CPU.RunState batchedState = batched.finish(true);
        String label = code + " at " + exact.engine.IP;
        assertEquals(label, exactState, batchedState);
        assertArrayEquals(label, exact.memory.getRawMemory()[0], batched.memory.getRawMemory()[0]);
        assertEquals(label, exact.engine.IP, batched.engine.IP);
        assertEquals(label, exact.engine.P, batched.engine.P);
        assertEquals(label, exact.engine.getLoopLevel(), batched.engine.getLoopLevel());
        assertEquals(label, exact.output, batched.output);
        assertEquals(label, exact.trace.instructions, batched.trace.instructions);
        assertEquals(label, exact.trace.edges, batched.trace.edges);
        assertEquals(label, exact.trace.last, batched.trace.last);
        if (expectBatch) assertTrue("Expected optimized execution: " + label, batched.trace.blocks > 0);
    }

    private static final class Run {
        final MemoryStub memory = new MemoryStub();
        final List<Byte> output = new ArrayList<>();
        final Trace trace;
        final EmulatorEngine engine;

        Run(String code, boolean batching, Debugging.Capture capture) {
            byte[] program = new byte[code.length() + 1];
            String alphabet = ";><+-.,[]";
            for (int i = 0; i < code.length(); i++) program[i] = (byte) alphabet.indexOf(code.charAt(i));
            memory.setProgram(program);
            BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
            context.writeToDevice(anyByte());
            expectLastCall().andAnswer(() -> { output.add(getCurrentArgument(0)); return null; }).anyTimes();
            expect(context.readFromDevice()).andReturn((byte) 42).anyTimes();
            replay(context);
            engine = new EmulatorEngine(memory, context, new Profiler(memory));
            engine.reset(0);
            trace = new Trace(batching);
            engine.debugger.observe(trace, capture);
        }

        CPU.RunState finish(boolean optimized) {
            try {
                for (int calls = 0; calls < 100_000; calls++) {
                    CPU.RunState state = engine.step(optimized);
                    if (state != CPU.RunState.STATE_STOPPED_BREAK) return state;
                }
                throw new AssertionError("Program did not terminate");
            } catch (IndexOutOfBoundsException failure) {
                return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
        }
    }

    private static final class Trace implements Debugging.Observer {
        final boolean batching;
        final Map<Integer, String> instructions = new TreeMap<>();
        final Set<String> edges = new TreeSet<>();
        int blocks, captures, last = -1;
        Trace(boolean batching) { this.batching = batching; }
        @Override public boolean beforeStep() { return false; }
        @Override public boolean canBatchInstructions() { return batching; }
        @Override public boolean afterStep(boolean completed, Optional<DecodedInstruction> instruction,
                                           OptionalInt nextAddress, Optional<StepView> capture) {
            if (capture.isPresent()) captures++;
            if (completed && instruction.isPresent()) {
                add(instruction.get());
                last = instruction.get().address();
                nextAddress.ifPresent(next -> edges.add(last + ":" + next));
            }
            return false;
        }
        @Override public void afterBlock(ExecutedBlock block) {
            blocks++;
            block.instructions().forEach(this::add);
            block.transitions().forEach(edge -> edges.add(edge.from() + ":" + edge.to()));
            last = block.lastAddress();
        }
        private void add(DecodedInstruction instruction) {
            instructions.put(instruction.address(), instruction.length() + ":" + instruction.mnemonic() + ":" + instruction.encoding());
        }
    }
}
