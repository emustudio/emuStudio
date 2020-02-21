package net.sf.emustudio.brainduck.cpu.impl;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.*;
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
        context.writeToDevice(anyShort());
        expectLastCall().anyTimes();
        expect(context.readFromDevice()).andReturn((short) 0).anyTimes();
    }

    private void resetProgram(short... operations) {
        int i = 0;
        for (short op : operations) {
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
    public void testCopyLoopWeird() throws Exception {
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

        memory.write(6, (short) 5);
        memory.write(7, (short) 5);
        memory.write(8, (short) 5);
        memory.write(9, (short) 5);
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
        memory.write(engine.P, (short) valueP);

        engine.step(true);

        for (int i = 0; i < resultValues.length; i++) {
            assertEquals("Expected res[" + i + "]=" + resultValues[i] + " at " + (start + relPositions[i]),
                resultValues[i], (int) memory.read(start + relPositions[i]));
        }
        assertEquals(0, (int) memory.read(engine.P));
    }
}
