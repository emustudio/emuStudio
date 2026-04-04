/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import org.junit.Before;
import org.junit.Test;

import static net.emustudio.plugins.cpu.brainduck.EmulatorEngine.*;
import static org.junit.Assert.*;

public class ProfilerTest {
    private MemoryStub memory;
    private Profiler profiler;

    @Before
    public void setUp() {
        memory = new MemoryStub();
    }

    private void setProgram(byte... ops) {
        memory.clear();
        int i = 0;
        for (byte op : ops) {
            memory.write(i++, op);
        }
        profiler = new Profiler(memory);
    }

    @Test
    public void testResetClearsCache() {
        setProgram(I_INC, I_INC, I_STOP);
        profiler.profileAndOptimize(3);
        assertNotNull(profiler.findCachedOperation(0));
        profiler.reset();
        assertNull(profiler.findCachedOperation(0));
    }

    @Test
    public void testRepeatedIncrements() {
        setProgram(I_INC, I_INC, I_INC, I_STOP);
        profiler.profileAndOptimize(4);

        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        assertEquals(I_INC, op.operation);
        assertEquals(3, op.argument);
        assertEquals(3, op.nextIP);
    }

    @Test
    public void testRepeatedDecrements() {
        setProgram(I_DEC, I_DEC, I_STOP);
        profiler.profileAndOptimize(3);

        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        assertEquals(I_DEC, op.operation);
        assertEquals(2, op.argument);
    }

    @Test
    public void testRepeatedIncV() {
        setProgram(I_INCV, I_INCV, I_INCV, I_INCV, I_STOP);
        profiler.profileAndOptimize(5);

        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        assertEquals(I_INCV, op.operation);
        assertEquals(4, op.argument);
    }

    @Test
    public void testClearLoopOptimization() {
        // [-]
        setProgram(I_LOOP_START, I_DECV, I_LOOP_END, I_STOP);
        profiler.profileAndOptimize(4);

        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        assertEquals(I_COPY_AND_CLEAR, op.operation);
        assertNotNull(op.copyLoops);
        assertTrue(op.copyLoops.isEmpty());
    }

    @Test
    public void testSimpleCopyLoop() {
        // [->+<]
        setProgram(I_LOOP_START, I_DECV, I_INC, I_INCV, I_DEC, I_LOOP_END, I_STOP);
        profiler.profileAndOptimize(7);

        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        assertEquals(I_COPY_AND_CLEAR, op.operation);
        assertEquals(1, op.copyLoops.size());
        assertEquals(1, op.copyLoops.get(0).factor);
        assertEquals(1, op.copyLoops.get(0).relativePosition);
    }

    @Test
    public void testScanLoop() {
        // [>]
        setProgram(I_LOOP_START, I_INC, I_LOOP_END, I_STOP);
        profiler.profileAndOptimize(4);

        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        assertEquals(I_SCANLOOP, op.operation);
        assertEquals(1, op.argument);
    }

    @Test
    public void testScanLoopLeft() {
        // [<]
        setProgram(I_LOOP_START, I_DEC, I_LOOP_END, I_STOP);
        profiler.profileAndOptimize(4);

        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        assertEquals(I_SCANLOOP, op.operation);
        assertEquals(-1, op.argument);
    }

    @Test
    public void testFindLoopEnd() {
        setProgram(I_LOOP_START, I_INCV, I_LOOP_END, I_STOP);
        profiler.profileAndOptimize(4);

        Integer loopEnd = profiler.findLoopEnd(0);
        assertNotNull(loopEnd);
        assertEquals(Integer.valueOf(3), loopEnd);
    }

    @Test
    public void testNoOptimizationForSingleOp() {
        setProgram(I_INC, I_STOP);
        profiler.profileAndOptimize(2);
        assertNull(profiler.findCachedOperation(0));
    }

    @Test
    public void testNestedLoop() {
        // [[]]
        setProgram(I_LOOP_START, I_LOOP_START, I_LOOP_END, I_LOOP_END, I_STOP);
        profiler.profileAndOptimize(5);

        Integer innerEnd = profiler.findLoopEnd(1);
        Integer outerEnd = profiler.findLoopEnd(0);
        assertNotNull(innerEnd);
        assertNotNull(outerEnd);
    }

    @Test
    public void testToString() {
        setProgram(I_INC, I_INC, I_STOP);
        profiler.profileAndOptimize(3);
        String str = profiler.toString();
        assertTrue(str.contains("Profiler"));
        assertTrue(str.contains("optimizations"));
    }

    @Test
    public void testCachedOperationToStringRepeat() {
        setProgram(I_INC, I_INC, I_STOP);
        profiler.profileAndOptimize(3);
        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        String str = op.toString();
        assertTrue(str.contains("REPEAT"));
    }

    @Test
    public void testCachedOperationToStringCopyLoop() {
        // [-]
        setProgram(I_LOOP_START, I_DECV, I_LOOP_END, I_STOP);
        profiler.profileAndOptimize(4);
        Profiler.CachedOperation op = profiler.findCachedOperation(0);
        assertNotNull(op);
        String str = op.toString();
        assertTrue(str.contains("COPYLOOP"));
    }

    @Test
    public void testCopyLoopToString() {
        Profiler.CopyLoop copyLoop = new Profiler.CopyLoop(2, 3);
        String str = copyLoop.toString();
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }
}

