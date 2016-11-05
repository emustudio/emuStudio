package net.sf.emustudio.brainduck.cpu.impl;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.function.Function;

import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_DEC_BACKWARD_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_DEC_FORWARD_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_INC_BACKWARD_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_INC_FORWARD_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_DEC;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_DECV;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_INC;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_INCV;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_LOOP_END;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_LOOP_START;
import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EmulatorEngineTest {
    private Profiler profiler;
    private BrainCPUContextImpl context;
    private MemoryStub memory;

    private EmulatorEngine engine;

    private final Function<Integer, Integer> forward = count -> engine.P + 1 + count;
    private final Function<Integer, Integer> backward = count -> engine.P - 1 - count;

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
        checkProfiler(I_CLEAR, 0, 3);

        runAndCheckCopyLoop(5, 0, 0, 0, forward);
    }

    @Test
    public void testCopyLoopIncForward() throws Exception {
        resetProgram(I_LOOP_START, I_INC, I_INCV, I_DEC, I_DECV, I_LOOP_END); // [>+<-]
        checkProfiler(I_COPY_INC_FORWARD_AND_CLEAR, 1, 6);

        runAndCheckCopyLoop(7, 1, 5, 5, forward);
    }

    @Test
    public void testCopyLoopIncForwardMultiple() throws Exception {
        resetProgram(
            I_LOOP_START, I_INC, I_INCV, I_INC, I_INCV, I_INC, I_INCV, I_DEC, I_DEC, I_DEC, I_DECV, I_LOOP_END
        ); // [>+>+>+<<<-]
        checkProfiler(I_COPY_INC_FORWARD_AND_CLEAR, 3, 12);

        runAndCheckCopyLoop(13, 3, 5, 5, forward);
    }

    @Test
    public void testCopyLoopIncForwardDECVfirstMultiple() throws Exception {
        resetProgram(
            I_LOOP_START, I_DECV, I_INC, I_INCV, I_INC, I_INCV, I_INC, I_INCV, I_DEC, I_DEC, I_DEC, I_LOOP_END
        ); // [->+>+>+<<<]
        checkProfiler(I_COPY_INC_FORWARD_AND_CLEAR, 3, 12);

        runAndCheckCopyLoop(13, 3, 5, 5, forward);
    }

    @Test
    public void testCopyLoopDecForward() throws Exception {
        resetProgram(I_LOOP_START, I_INC, I_DECV, I_DEC, I_DECV, I_LOOP_END); // [>-<-]
        checkProfiler(I_COPY_DEC_FORWARD_AND_CLEAR, 1, 6);

        memory.write(8, (short)10);
        runAndCheckCopyLoop(7, 1, 3, 7, forward);
    }

    @Test
    public void testCopyLoopDecForwardMultiple() throws Exception {
        resetProgram(
            I_LOOP_START, I_INC, I_DECV, I_INC, I_DECV, I_INC, I_DECV, I_DEC, I_DEC, I_DEC, I_DECV, I_LOOP_END
        ); // [>->->-<<<-]
        checkProfiler(I_COPY_DEC_FORWARD_AND_CLEAR, 3, 12);

        memory.write(14, (short)10);
        memory.write(15, (short)10);
        memory.write(16, (short)10);
        runAndCheckCopyLoop(13, 3, 3, 7, forward);
    }

    @Test
    public void testCopyLoopDecForwardDECVfirstMultiple() throws Exception {
        resetProgram(
            I_LOOP_START, I_DECV, I_INC, I_DECV, I_INC, I_DECV, I_INC, I_DECV, I_DEC, I_DEC, I_DEC, I_LOOP_END
        ); // [->->->-<<<]
        checkProfiler(I_COPY_DEC_FORWARD_AND_CLEAR, 3, 12);

        memory.write(14, (short)10);
        memory.write(15, (short)10);
        memory.write(16, (short)10);
        runAndCheckCopyLoop(13, 3, 3, 7, forward);
    }

    @Test
    public void testCopyLoopIncBackward() throws Exception {
        resetProgram(I_LOOP_START, I_DEC, I_INCV, I_INC, I_DECV, I_LOOP_END); // [<+>-]
        checkProfiler(I_COPY_INC_BACKWARD_AND_CLEAR, 1, 6);

        runAndCheckCopyLoop(10, 1, 5, 5, backward);
    }

    @Test
    public void testCopyLoopIncBackwardMultiple() throws Exception {
        resetProgram(
            I_LOOP_START, I_DEC, I_INCV, I_DEC, I_INCV, I_DEC, I_INCV, I_INC, I_INC, I_INC, I_DECV, I_LOOP_END
        ); // [<+<+<+>>>-]
        checkProfiler(I_COPY_INC_BACKWARD_AND_CLEAR, 3, 12);

        runAndCheckCopyLoop(16, 3, 5, 5, backward);
    }

    @Test
    public void testCopyLoopIncBackwardDECVfirstMultiple() throws Exception {
        resetProgram(
            I_LOOP_START, I_DECV, I_DEC, I_INCV, I_DEC, I_INCV, I_DEC, I_INCV, I_INC, I_INC, I_INC, I_LOOP_END
        ); // [-<+<+<+>>>]
        checkProfiler(I_COPY_INC_BACKWARD_AND_CLEAR, 3, 12);

        runAndCheckCopyLoop(16, 3, 5, 5, backward);
    }

    @Test
    public void testCopyLoopDecBackward() throws Exception {
        resetProgram(I_LOOP_START, I_DEC, I_DECV, I_INC, I_DECV, I_LOOP_END); // [<->-]
        checkProfiler(I_COPY_DEC_BACKWARD_AND_CLEAR, 1, 6);

        memory.write(15, (short)10);
        runAndCheckCopyLoop(16, 1, 3, 7, backward);
    }

    @Test
    public void testCopyLoopDecBackwardMultiple() throws Exception {
        resetProgram(
            I_LOOP_START, I_DEC, I_DECV, I_DEC, I_DECV, I_DEC, I_DECV, I_INC, I_INC, I_INC, I_DECV, I_LOOP_END
        ); // [<-<-<->>>-]
        checkProfiler(I_COPY_DEC_BACKWARD_AND_CLEAR, 3, 12);

        memory.write(15, (short)10);
        memory.write(14, (short)10);
        memory.write(13, (short)10);
        runAndCheckCopyLoop(16, 3, 3, 7, backward);
    }

    private void checkProfiler(short OP, int argument, int nextIP) {
        engine.reset(0);

        Profiler.CachedOperation operation = profiler.findCachedOperation(0);

        assertNotNull(operation);
        assertEquals(OP, operation.operation);
        assertEquals(argument, operation.argument);
        assertEquals(nextIP, operation.nextIP);
    }

    private void runAndCheckCopyLoop(int start, int count, int value, int initValue,
                                     Function<Integer, Integer> addressOp) throws IOException {
        engine.P = start;
        memory.write(engine.P, (short)initValue);

        engine.step(true);

        for (int i = 0; i < count; i++) {
            assertEquals(value, (int)memory.read(addressOp.apply(i)));
        }
        assertEquals(0, (int)memory.read(engine.P));
    }
}
