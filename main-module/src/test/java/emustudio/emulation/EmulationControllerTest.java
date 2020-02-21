package emustudio.emulation;

import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emulib.runtime.exceptions.PluginInitializationException;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class EmulationControllerTest {
    private CPUStub cpuStub;
    private CPUListenerStub listener;

    @Before
    public void setUp() throws Exception {
        listener = new CPUListenerStub();
        cpuStub = new CPUStub();
        cpuStub.addCPUListener(listener);
    }

    @Test
    public void testStartIsNotExecutedWhenRunStateIsNotBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.start();

        assertEquals(0, cpuStub.callCalled);
        assertEquals(null, listener.runState);
    }


    @Test(timeout = 1000)
    public void testStartIsExecutedWhenRunStateIsBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();

        awaitFor(controller::start, 2);

        assertEquals(1, cpuStub.callCalled);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, listener.runState);
    }


    @Test
    public void textStopIsNotExecutedWhenRunStateIsStopped() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.stop();

        assertEquals(null, listener.runState);
    }


    @Test(timeout = 1000)
    public void testStopIsNotExecutedWhenRunStateIsStoppedBadInstr() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();
        cpuStub.callReturnState = CPU.RunState.STATE_STOPPED_BAD_INSTR;

        awaitFor(controller::start, 2);
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, listener.runState);

        controller.stop();

        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, listener.runState);
    }

    @Test(timeout = 1000)
    public void testStopIsNotExecutedWhenRunStateIsStoppedAddrFallout() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();
        cpuStub.callReturnState = CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;

        awaitFor(controller::start, 2);
        assertEquals(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT, listener.runState);

        controller.stop();

        assertEquals(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT, listener.runState);
    }

    @Test(timeout = 1000)
    public void testStopIsExecutedWhenRunStateIsBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();

        awaitFor(controller::stop, 1);

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, listener.runState);
    }

    @Test(timeout = 1000)
    public void testStopIsExecutedWhenRunStateIsRunning() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();
        cpuStub.callReturnState = CPU.RunState.STATE_RUNNING;

        awaitFor(controller::start, 2);
        assertEquals(CPU.RunState.STATE_RUNNING, listener.runState);

        awaitFor(controller::stop, 1);

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, listener.runState);
    }

    @Test
    public void testStepIsNotExecutedWhenRunStateIsNotBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.step();

        assertEquals(0, cpuStub.stepCalled);
        assertEquals(null, listener.runState);
    }

    @Test(timeout = 1000)
    public void testStepIsExecutedWhenRunStateIsBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();
        cpuStub.stepReturnState = CPU.RunState.STATE_RUNNING;

        awaitFor(controller::step, 1);

        assertEquals(1, cpuStub.stepCalled);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, listener.runState);
    }

    @Test(timeout = 1000)
    public void testStepWithSleepIsNotExecutedWhenRunStateIsNotBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.step(1, TimeUnit.MINUTES); // will never sleep

        assertEquals(0, cpuStub.stepCalled);
        assertEquals(null, listener.runState);
    }

    @Test(timeout = 1000)
    public void testStepWithSleepIsExecutedWhenRunStateIsBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();
        cpuStub.stepReturnState = CPU.RunState.STATE_STOPPED_NORMAL;

        awaitFor(() -> controller.step(10, TimeUnit.MILLISECONDS), 1);

        assertEquals(1, cpuStub.stepCalled);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, listener.runState);
    }

    @Test(timeout = 1000)
    public void testResetCallsCPUresetWithProgramStartIfMemoryIsAvailable() throws Exception {
        Memory memory = createMock(Memory.class);
        expect(memory.getProgramStart()).andReturn(10).once();
        replay(memory);

        EmulationController controller = new EmulationController(
            cpuStub, Optional.of(memory), () -> Collections.emptyList()
        );

        awaitFor(controller::reset, 1);

        assertEquals(10, cpuStub.resetStartPos);
        assertEquals(1, cpuStub.resetCalled);
        verify(memory);
    }

    @Test(timeout = 1000)
    public void testResetCallsCPUresetIfMemoryIsNotAvailable() throws Exception {
        EmulationController controller = createPlainEmulationController();

        awaitFor(controller::reset, 1);

        assertEquals(0, cpuStub.resetStartPos);
        assertEquals(1, cpuStub.resetCalled);
    }

    @Test(timeout = 1000)
    public void testResetCallsDevicesReset() throws Exception {
        Device dev1 = createMock(Device.class);
        dev1.reset();
        expectLastCall().once();

        Device dev2 = createMock(Device.class);
        dev2.reset();
        expectLastCall().once();

        replay(dev1, dev2);

        EmulationController controller = new EmulationController(
            cpuStub, Optional.empty(), () -> Arrays.asList(dev1, dev2)
        );

        awaitFor(controller::reset, 1);

        Thread.sleep(300);

        assertEquals(1, cpuStub.resetCalled);
        verify(dev1, dev2);
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallStart() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.close();

        controller.start();
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallStep() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.close();

        controller.step();
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallStepWithTimeout() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.close();

        controller.step(10, TimeUnit.MILLISECONDS);
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallStop() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.close();

        controller.stop();
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallReset() throws Exception {
        EmulationController controller = createPlainEmulationController();

        controller.close();

        controller.reset();
    }

    private EmulationController createPlainEmulationController() {
        return new EmulationController(
            cpuStub, Optional.empty(), () -> Collections.emptyList()
        );
    }

    private void awaitFor(Runnable runnable, int times) throws InterruptedException {
        listener.initializeAwait(times);
        runnable.run();
        listener.await();
    }

    private void cpuReset() {
        cpuStub.reset();
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, listener.runState);
    }


    private static class CPUStub extends AbstractCPU {
        private volatile int stepCalled;
        private volatile int callCalled;
        private volatile int resetCalled;
        private volatile int resetStartPos;

        private RunState callReturnState = RunState.STATE_STOPPED_NORMAL;
        private RunState stepReturnState = RunState.STATE_STOPPED_BREAK;

        public CPUStub() {
            super(0L);
        }

        @Override
        protected void destroyInternal() {

        }

        @Override
        protected RunState stepInternal() throws Exception {
            stepCalled++;
            return stepReturnState;
        }

        @Override
        public JPanel getStatusPanel() {
            return null;
        }

        @Override
        public int getInstructionPosition() {
            return 0;
        }

        @Override
        public boolean setInstructionPosition(int pos) {
            return false;
        }

        @Override
        public Disassembler getDisassembler() {
            return null;
        }

        @Override
        public void initialize(SettingsManager settingsManager) throws PluginInitializationException {

        }

        @Override
        public String getVersion() {
            return null;
        }

        @Override
        public RunState call() throws Exception {
            callCalled++;
            return callReturnState;
        }

        @Override
        public void resetInternal(int startPos) {
            resetStartPos = startPos;
            resetCalled++;
        }
    }

    private static class CPUListenerStub implements CPU.CPUListener {
        private volatile CPU.RunState runState;
        private volatile CountDownLatch latch;

        @Override
        public void runStateChanged(CPU.RunState runState) {
            this.runState = runState;
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        public void internalStateChanged() {

        }

        public void initializeAwait(int times) {
            latch = new CountDownLatch(times);
        }

        public void await() throws InterruptedException {
            latch.await();
        }
    }
}
