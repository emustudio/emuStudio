/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.cpu.CPU;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static net.emustudio.plugins.cpu.brainduck.EmulatorEngine.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class EmulatorEngineTest {
    private Profiler profiler;
    private BrainCPUContextImpl context;
    private MemoryStub memory;

    private EmulatorEngine engine;

    @Before
    public void setUp() throws Exception {
        memory = new MemoryStub();
        profiler = new Profiler(memory);

        context = createNiceMock(BrainCPUContextImpl.class);
        context.writeToDevice(anyByte());
        expectLastCall().anyTimes();
        expect(context.readFromDevice()).andReturn((byte) 0).anyTimes();
    }

    private void resetProgram(byte... operations) {
        int i = 0;
        for (byte op : operations) {
            memory.write(i++, op);
        }
        engine = new EmulatorEngine(memory, context, profiler);
    }

    @Test
    public void testCellClear() throws Exception {
        resetProgram(I_LOOP_START, I_DECV, I_LOOP_END); // [-]
        checkProfilerCopyLoop(3, new int[0], new int[0]);

        runAndCheckCopyLoop(5, 5, new int[0], new int[0]);
    }

    @Test
    public void testCopyLoop() throws Exception {
        resetProgram(
                I_LOOP_START,
                I_INC, I_INCV, I_INCV,
                I_INC, I_INCV, I_DECV,
                I_INC, I_INC, I_DECV, I_DECV,
                I_DEC, I_DEC, I_DEC, I_DEC,
                I_DECV,
                I_LOOP_END
        ); // [>++>+->>++<<<<-]
        checkProfilerCopyLoop(17, new int[]{2, 0, -2}, new int[]{1, 2, 4});

        runAndCheckCopyLoop(18, 5, new int[]{10, 0, -10 & 0xFF}, new int[]{1, 2, 4});
    }

    @Test
    public void testCopyLoopWeird() {
        resetProgram(
                I_LOOP_START,
                I_DECV,
                I_DEC, I_INC, I_DECV,
                I_LOOP_END
        ); // [-<>-]

        engine.reset(0);
        assertNull(profiler.findCachedOperation(0));
    }

    @Test
    public void testCopyLoopWithPrints() throws Exception {
        resetProgram(
                I_LOOP_START,
                I_DECV,
                I_INC, I_INCV,
                I_PRINT,
                I_INC, I_INCV, I_INCV,
                I_PRINT,
                I_DEC, I_DEC,
                I_LOOP_END
        ); // [->+.>++.<<]

        checkProfilerCopyLoop(12, new int[]{1, 0, 2, 0}, new int[]{1, 0, 2, 0});

        runAndCheckCopyLoop(12, 5, new int[]{5, 0, 10}, new int[]{1, 0, 2, 0});
    }

    @Test
    public void testScanloop() throws Exception {
        resetProgram(
                I_LOOP_START,
                I_DEC,
                I_INC,
                I_INC,
                I_LOOP_END
        ); // [<>>]

        engine.reset(0);

        Profiler.CachedOperation operation = profiler.findCachedOperation(0);
        assertNotNull(operation);
        assertEquals(I_SCANLOOP, operation.operation);

        memory.write(6, (byte) 5);
        memory.write(7, (byte) 5);
        memory.write(8, (byte) 5);
        memory.write(9, (byte) 5);
        engine.P = 6;

        engine.step(true);

        assertEquals(10, engine.P);
    }

    // --- New basic instruction tests ---

    @Test
    public void testStopInstruction() {
        resetProgram(I_STOP);
        replay(context);
        engine.reset(0);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step(false));
    }

    @Test
    public void testIncInstruction() {
        resetProgram(I_INC, I_STOP);
        replay(context);
        engine.reset(0);
        int initialP = engine.P;
        engine.step(false); // >
        assertEquals(initialP + 1, engine.P);
    }

    @Test
    public void testDecInstruction() {
        resetProgram(I_INC, I_DEC, I_STOP);
        replay(context);
        engine.reset(0);
        int initialP = engine.P;
        engine.step(false); // >
        engine.step(false); // <
        assertEquals(initialP, engine.P);
    }

    @Test
    public void testDecBelowZeroReturnsAddrFallout() {
        resetProgram(I_DEC, I_STOP);
        replay(context);
        engine.reset(0);
        engine.P = 0;
        assertEquals(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT, engine.step(false));
    }

    @Test
    public void testIncvInstruction() {
        resetProgram(I_INCV, I_STOP);
        replay(context);
        engine.reset(0);
        memory.write(engine.P, (byte) 0);
        engine.step(false); // +
        assertEquals(1, memory.read(engine.P).byteValue());
    }

    @Test
    public void testDecvInstruction() {
        resetProgram(I_DECV, I_STOP);
        replay(context);
        engine.reset(0);
        memory.write(engine.P, (byte) 5);
        engine.step(false); // -
        assertEquals(4, memory.read(engine.P).byteValue());
    }

    @Test
    public void testPrintInstruction() {
        resetProgram(I_PRINT, I_STOP);
        replay(context);
        engine.reset(0);
        memory.write(engine.P, (byte) 65);
        engine.step(false); // .
        verify(context);
    }

    @Test
    public void testReadInstruction() {
        resetProgram(I_READ, I_STOP);
        replay(context);
        engine.reset(0);
        engine.step(false); // ,
        verify(context);
    }

    @Test
    public void testLoopSkipsWhenZero() {
        resetProgram(I_LOOP_START, I_INCV, I_LOOP_END, I_STOP);
        replay(context);
        engine.reset(0);
        // P points to data area which is 0
        memory.write(engine.P, (byte) 0);
        engine.step(false); // [ - should skip to after ]
        // IP should be after ]
        assertEquals(3, engine.IP);
    }

    @Test
    public void testLoopEntersWhenNonZero() {
        resetProgram(I_LOOP_START, I_DECV, I_LOOP_END, I_STOP);
        replay(context);
        engine.reset(0);
        memory.write(engine.P, (byte) 1);
        engine.step(false); // [ - should enter loop
        assertEquals(1, engine.IP);
        assertEquals(1, engine.getLoopLevel());
    }

    @Test
    public void testLoopEndJumpsBackWhenNonZero() {
        resetProgram(I_LOOP_START, I_DECV, I_LOOP_END, I_STOP);
        replay(context);
        engine.reset(0);
        memory.write(engine.P, (byte) 2);
        engine.step(false); // [ - enter
        engine.step(false); // - (value becomes 1)
        engine.step(false); // ] - should jump back to [
        assertEquals(0, engine.IP);
    }

    @Test
    public void testInvalidInstruction() {
        memory.write(0, (byte) 99);
        replay(context);
        engine = new EmulatorEngine(memory, context, profiler);
        engine.IP = 0;
        engine.P = 10;
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step(false));
    }

    @Test
    public void testGetLoopLevel() {
        resetProgram(I_STOP);
        replay(context);
        engine.reset(0);
        assertEquals(0, engine.getLoopLevel());
    }

    @Test
    public void testGetP() {
        resetProgram(I_STOP);
        replay(context);
        engine.reset(0);
        engine.P = 42;
        assertEquals(42, engine.getP());
    }

    @Test
    public void testIncOverflow() {
        resetProgram(I_INC, I_STOP);
        replay(context);
        engine.reset(0);
        engine.P = memory.memory[0].length;
        assertEquals(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT, engine.step(false));
    }

    private void checkProfilerCopyLoop(int nextIP, int[] factors, int[] relPositions) {
        engine.reset(0);

        Profiler.CachedOperation operation = profiler.findCachedOperation(0);

        assertNotNull(operation);
        assertEquals(I_COPY_AND_CLEAR, operation.operation);
        assertEquals(nextIP, operation.nextIP);

        for (int i = 0; i < operation.copyLoops.size(); i++) {
            Profiler.CopyLoop copyLoop = operation.copyLoops.get(i);
            assertEquals("Expected factor[" + i + "]=" + factors[i], factors[i], copyLoop.factor);
            assertEquals(relPositions[i], copyLoop.relativePosition);
        }
    }

    private void runAndCheckCopyLoop(int start, int valueP, int[] resultValues, int[] relPositions) throws IOException {
        engine.P = start;
        memory.write(engine.P, (byte) valueP);

        engine.step(true);

        for (int i = 0; i < resultValues.length; i++) {
            assertEquals("Expected res[" + i + "]=" + resultValues[i] + " at " + (start + relPositions[i]),
                    resultValues[i], memory.read(start + relPositions[i]) & 0xFF);
        }
        assertEquals(0, memory.read(engine.P) & 0xFF);
    }
}
