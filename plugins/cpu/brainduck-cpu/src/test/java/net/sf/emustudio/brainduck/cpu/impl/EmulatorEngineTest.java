package net.sf.emustudio.brainduck.cpu.impl;

import java.io.IOException;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_DEC;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_DECV;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_INC;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_INCV;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_LOOP_END;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_LOOP_START;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_PRINT;
import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class EmulatorEngineTest {
    private Profiler profiler;
    private BrainCPUContextImpl context;
    private MemoryStub memory;

    private EmulatorEngine engine;

    @Before
    public void setUp() throws Exception {
        memory = new MemoryStub(65536);
        profiler = new Profiler(memory);

        context = createNiceMock(BrainCPUContextImpl.class);
        context.writeToDevice(anyShort());
        expectLastCall().anyTimes();
        expect(context.readFromDevice()).andReturn((short)0).anyTimes();
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
        checkProfiler(3, new int[0], new int[0]);

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
        checkProfiler(17, new int[] {2,0,-2}, new int[] {1,2,4});

        runAndCheckCopyLoop(18, 5, new int[] { 10, 0, -10 & 0xFF}, new int[] {1,2,4});
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

        checkProfiler(12, new int[] {1,0,2,0}, new int[] {1,0,2,0});

        runAndCheckCopyLoop(12, 5, new int[] { 5, 0, 10 }, new int[] {1,0,2,0});
    }

    private void checkProfiler(int nextIP, int[] factors, int[] relPositions) {
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
        memory.write(engine.P, (short)valueP);

        engine.step(true);

        for (int i = 0; i < resultValues.length; i++) {
            assertEquals("Expected res[" + i + "]=" + resultValues[i] + " at " + (start + relPositions[i]),
                resultValues[i], (int)memory.read(start + relPositions[i]));
        }
        assertEquals(0, (int)memory.read(engine.P));
    }
}
