/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.emustudio.plugins.cpu.brainduck;

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
