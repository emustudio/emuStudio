/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

package net.emustudio.application.emulation;

import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.PluginSettings;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EmulationControllerTest {
    private CPUStub cpuStub;
    private CPUListenerStub listener;

    @Before
    public void setUp() {
        listener = new CPUListenerStub();
        cpuStub = new CPUStub();
        cpuStub.addCPUListener(listener);
    }

    @Test
    public void testStartIsNotExecutedWhenRunStateIsNotBreakpoint() {
        EmulationController controller = createPlainEmulationController();

        controller.start();

        assertEquals(0, cpuStub.callCalled.get());
        assertNull(listener.runState);
    }


    @Test(timeout = 1000)
    public void testStartIsExecutedWhenRunStateIsBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();

        awaitFor(controller::start, 2);

        assertEquals(1, cpuStub.callCalled.get());
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, listener.runState);
    }


    @Test
    public void textStopIsNotExecutedWhenRunStateIsStopped() {
        EmulationController controller = createPlainEmulationController();

        controller.stop();
        assertNull(listener.runState);
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

        awaitFor(controller::start, 1);
        assertEquals(CPU.RunState.STATE_RUNNING, listener.runState);

        awaitFor(controller::stop, 1);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, listener.runState);
    }

    @Test
    public void testStepIsNotExecutedWhenRunStateIsNotBreakpoint() {
        EmulationController controller = createPlainEmulationController();

        controller.step();

        assertEquals(0, cpuStub.stepCalled.get());
        assertNull(listener.runState);
    }

    @Test(timeout = 1000)
    public void testStepIsExecutedWhenRunStateIsBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();
        cpuStub.stepReturnState = CPU.RunState.STATE_RUNNING;

        awaitFor(controller::step, 1);

        assertEquals(1, cpuStub.stepCalled.get());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, listener.runState);
    }

    @Test(timeout = 1000)
    public void testStepWithSleepIsNotExecutedWhenRunStateIsNotBreakpoint() {
        EmulationController controller = createPlainEmulationController();

        controller.step(1, TimeUnit.MINUTES); // will never sleep

        assertEquals(0, cpuStub.stepCalled.get());
        assertNull(listener.runState);
    }

    @Test(timeout = 1000)
    public void testStepWithSleepIsExecutedWhenRunStateIsBreakpoint() throws Exception {
        EmulationController controller = createPlainEmulationController();
        cpuReset();
        cpuStub.stepReturnState = CPU.RunState.STATE_STOPPED_NORMAL;

        awaitFor(() -> controller.step(10, TimeUnit.MILLISECONDS), 1);

        assertEquals(1, cpuStub.stepCalled.get());
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, listener.runState);
    }

    @Test(timeout = 1000)
    public void testResetCallsCPUresetWithProgramStartIfMemoryIsAvailable() throws Exception {
        Memory memory = createMock(Memory.class);
        expect(memory.getProgramLocation()).andReturn(10).once();
        memory.reset();
        expectLastCall().anyTimes(); // our latch won't wait for memory reset. So sometimes we catch it, sometimes not.
        replay(memory);

        EmulationController controller = new EmulationController(cpuStub, memory, Collections.emptyList());
        awaitFor(controller::reset, 1);

        assertEquals(10, cpuStub.resetStartPos);
        assertEquals(1, cpuStub.resetCalled.get());
        verify(memory);
    }

    @Test(timeout = 1000)
    public void testResetCallsCPUresetIfMemoryIsNotAvailable() throws Exception {
        EmulationController controller = createPlainEmulationController();

        awaitFor(controller::reset, 1);

        assertEquals(0, cpuStub.resetStartPos);
        assertEquals(1, cpuStub.resetCalled.get());
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
            cpuStub, null, Arrays.asList(dev1, dev2)
        );

        awaitFor(controller::reset, 1);

        Thread.sleep(300);

        assertEquals(1, cpuStub.resetCalled.get());
        verify(dev1, dev2);
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallStart() {
        EmulationController controller = createPlainEmulationController();

        controller.close();
        controller.start();
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallStep() {
        EmulationController controller = createPlainEmulationController();

        controller.close();
        controller.step();
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallStepWithTimeout() {
        EmulationController controller = createPlainEmulationController();

        controller.close();
        controller.step(10, TimeUnit.MILLISECONDS);
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallStop() {
        EmulationController controller = createPlainEmulationController();

        controller.close();
        controller.stop();
    }

    @Test(expected = RejectedExecutionException.class)
    public void testAfterCloseCannotCallReset() {
        EmulationController controller = createPlainEmulationController();

        controller.close();
        controller.reset();
    }

    private EmulationController createPlainEmulationController() {
        return new EmulationController(cpuStub, null, Collections.emptyList());
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
        private final AtomicInteger stepCalled = new AtomicInteger();
        private final AtomicInteger callCalled = new AtomicInteger();
        private final AtomicInteger resetCalled = new AtomicInteger();
        private volatile int resetStartPos;

        private RunState callReturnState = RunState.STATE_STOPPED_NORMAL;
        private RunState stepReturnState = RunState.STATE_STOPPED_BREAK;

        public CPUStub() {
            super(0L, createNiceMock(ApplicationApi.class), createNiceMock(PluginSettings.class));
        }

        @Override
        protected void destroyInternal() {

        }

        @Override
        protected RunState stepInternal() {
            stepCalled.incrementAndGet();
            return stepReturnState;
        }

        @Override
        public JPanel getStatusPanel() {
            return null;
        }

        @Override
        public int getInstructionLocation() {
            return 0;
        }

        @Override
        public boolean setInstructionLocation(int pos) {
            return false;
        }

        @Override
        public Disassembler getDisassembler() {
            return null;
        }

        @Override
        public void initialize() {

        }

        @Override
        public String getVersion() {
            return null;
        }

        @Override
        public String getCopyright() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public RunState call() {
            callCalled.incrementAndGet();
            return callReturnState;
        }

        @Override
        public void resetInternal(int startPos) {
            resetStartPos = startPos;
            resetCalled.incrementAndGet();
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
