/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.emulib.plugins.cpu.CPU;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EmulationControllerSchedulingTest {
    private final CPU cpu = mock(CPU.class);
    private final List<CPU.CPUListener> listeners = new CopyOnWriteArrayList<>();
    private final BlockingQueue<Integer> steps = new LinkedBlockingQueue<>();
    private final AtomicInteger stepCount = new AtomicInteger();

    @Before
    public void setUp() {
        doAnswer(call -> { listeners.add(call.getArgument(0)); return null; })
                .when(cpu).addCPUListener(any());
        doAnswer(call -> { listeners.remove(call.getArgument(0)); return null; })
                .when(cpu).removeCPUListener(any());
        doAnswer(call -> { state(CPU.RunState.STATE_STOPPED_BREAK); return null; }).when(cpu).pause();
        doAnswer(call -> { state(CPU.RunState.STATE_STOPPED_NORMAL); return null; }).when(cpu).stop();
        doAnswer(call -> { state(CPU.RunState.STATE_STOPPED_BREAK); return null; }).when(cpu).reset();
        doAnswer(call -> { completeStep(); return null; }).when(cpu).step();
    }

    @Test(timeout = 3000)
    public void pauseAndManualStepDoNotWaitForTimedInterval() throws Exception {
        try (EmulationController controller = controller()) {
            controller.step(1, TimeUnit.DAYS);
            awaitStep(1);

            controller.pause();
            assertFalse(controller.isTimedRunning());
            controller.step();
            awaitStep(2);
        }
        assertEquals(2, stepCount.get());
    }

    @Test(timeout = 3000)
    public void stopCancelsTimedSteppingWithoutWaitingForInterval() throws Exception {
        CountDownLatch stopped = new CountDownLatch(1);
        doAnswer(call -> {
            state(CPU.RunState.STATE_STOPPED_NORMAL);
            stopped.countDown();
            return null;
        }).when(cpu).stop();
        try (EmulationController controller = controller()) {
            controller.step(1, TimeUnit.DAYS);
            awaitStep(1);

            controller.stop();
            assertFalse(controller.isTimedRunning());
            assertTrue(stopped.await(1, TimeUnit.SECONDS));
            controller.reset();
            controller.step();
            awaitStep(2);
        }
        assertEquals(2, stepCount.get());
    }

    @Test(timeout = 3000)
    public void resetAndManualStepDoNotWaitForTimedInterval() throws Exception {
        try (EmulationController controller = controller()) {
            controller.step(1, TimeUnit.DAYS);
            awaitStep(1);

            controller.reset();
            assertFalse(controller.isTimedRunning());
            controller.step();
            awaitStep(2);
        }
        assertEquals(2, stepCount.get());
    }

    @Test(timeout = 3000)
    public void closeCancelsTimedWorkAndReleasesListener() throws Exception {
        try (EmulationController controller = controller()) {
            controller.step(1, TimeUnit.DAYS);
            awaitStep(1);
        }
        assertTrue(listeners.isEmpty());
        verify(cpu).removeCPUListener(any());
        assertEquals(1, stepCount.get());
    }

    @Test(timeout = 3000)
    public void timedRunRepeatsUntilCpuReachesTerminalState() throws Exception {
        doAnswer(call -> {
            int step = stepCount.incrementAndGet();
            state(step == 3 ? CPU.RunState.STATE_STOPPED_NORMAL : CPU.RunState.STATE_STOPPED_BREAK);
            steps.add(step);
            return null;
        }).when(cpu).step();
        try (EmulationController controller = controller()) {
            controller.step(1, TimeUnit.MILLISECONDS);
            awaitStep(1);
            awaitStep(2);
            awaitStep(3);

            assertNull("Terminal state must end timed execution", steps.poll(100, TimeUnit.MILLISECONDS));
            assertFalse(controller.isTimedRunning());
        }
        assertEquals(3, stepCount.get());
    }

    @Test(timeout = 3000)
    public void replacingInFlightTimedRunDoesNotResumeItsOldLoop() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> {
            if (stepCount.get() == 0) {
                entered.countDown();
                assertTrue(release.await(1, TimeUnit.SECONDS));
            }
            completeStep();
            return null;
        }).when(cpu).step();
        try (EmulationController controller = controller()) {
            try {
                controller.step(1, TimeUnit.MILLISECONDS);
                assertTrue(entered.await(1, TimeUnit.SECONDS));
                controller.step(1, TimeUnit.DAYS);
            } finally {
                release.countDown();
            }
            awaitStep(1);
            awaitStep(2);
            assertTrue(controller.isTimedRunning());
            assertNull("Replaced timer must not step again", steps.poll(100, TimeUnit.MILLISECONDS));
        }
        assertEquals(2, stepCount.get());
    }

    @Test(timeout = 3000)
    public void pauseAndResetCancelInFlightTimedRunBeforeRestart() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> {
            if (stepCount.get() == 0) {
                entered.countDown();
                assertTrue(release.await(1, TimeUnit.SECONDS));
            }
            completeStep();
            return null;
        }).when(cpu).step();
        try (EmulationController controller = controller()) {
            try {
                controller.step(1, TimeUnit.MILLISECONDS);
                assertTrue(entered.await(1, TimeUnit.SECONDS));
                controller.pause();
                controller.reset();
                controller.step(1, TimeUnit.DAYS);
            } finally {
                release.countDown();
            }
            awaitStep(1);
            awaitStep(2);
            assertNull("Cancelled timer must not step after restart", steps.poll(100, TimeUnit.MILLISECONDS));
        }
        assertEquals(2, stepCount.get());
    }

    @Test(timeout = 3000)
    public void asynchronousStepAcknowledgementStillPrecedesNextCommand() throws Exception {
        CountDownLatch requested = new CountDownLatch(1);
        CountDownLatch reset = new CountDownLatch(1);
        doAnswer(call -> { requested.countDown(); return null; }).when(cpu).step();
        doAnswer(call -> {
            state(CPU.RunState.STATE_STOPPED_BREAK);
            reset.countDown();
            return null;
        }).when(cpu).reset();
        try (EmulationController controller = controller()) {
            controller.step();
            assertTrue(requested.await(1, TimeUnit.SECONDS));
            controller.reset();
            try {
                assertFalse("Reset must wait for step acknowledgement", reset.await(100, TimeUnit.MILLISECONDS));
            } finally {
                state(CPU.RunState.STATE_STOPPED_BREAK);
            }
            assertTrue(reset.await(1, TimeUnit.SECONDS));
        }
    }

    private EmulationController controller() {
        return new EmulationController(cpu, null, List.of(), null);
    }

    private void state(CPU.RunState state) {
        listeners.forEach(listener -> listener.runStateChanged(state));
    }

    private void completeStep() {
        state(CPU.RunState.STATE_STOPPED_BREAK);
        steps.add(stepCount.incrementAndGet());
    }

    private void awaitStep(int expected) throws InterruptedException {
        assertEquals(Integer.valueOf(expected), steps.poll(1, TimeUnit.SECONDS));
    }
}
