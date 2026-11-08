/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.emulib.plugins.cpu.CPU;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static net.emustudio.application.emulation.EmulationControllerTest.*;
import static net.emustudio.emulib.plugins.cpu.CPU.RunState.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EmulationControllerSchedulingTest {
    private final CPU cpu = mock(CPU.class);
    private final BlockingQueue<Integer> steps = new LinkedBlockingQueue<>();
    private final AtomicInteger stepCount = new AtomicInteger();

    @Before
    public void setUp() throws Exception {
        when(cpu.step()).thenAnswer(call -> {
            steps.add(stepCount.incrementAndGet());
            return STATE_STOPPED_BREAK;
        });
    }

    @Test(timeout = 5000)
    public void pauseAndManualStepDoNotWaitForTimedInterval() throws Exception {
        try (EmulationController controller = controller(cpu)) {
            await(controller.step(1, TimeUnit.DAYS));
            awaitStep(1);

            await(controller.pause());
            assertFalse(controller.snapshot().isTimedRunning());
            await(controller.step());

            awaitStep(2);
            assertNoStep();
        }
    }

    @Test(timeout = 5000)
    public void stopCancelsTimedSteppingWithoutWaitingForInterval() throws Exception {
        try (EmulationController controller = controller(cpu)) {
            await(controller.step(1, TimeUnit.DAYS));
            awaitStep(1);

            assertEquals(STATE_STOPPED_NORMAL, await(controller.stop()));
            assertFalse(controller.snapshot().isTimedRunning());
            await(controller.reset());
            await(controller.step());

            awaitStep(2);
            assertNoStep();
        }
    }

    @Test(timeout = 5000)
    public void resetCancelsTimedSteppingWithoutWaitingForInterval() throws Exception {
        try (EmulationController controller = controller(cpu)) {
            await(controller.step(1, TimeUnit.DAYS));
            awaitStep(1);

            await(controller.reset());
            assertFalse(controller.snapshot().isTimedRunning());
            await(controller.step());

            awaitStep(2);
            assertNoStep();
            verify(cpu).reset();
        }
    }

    @Test(timeout = 5000)
    public void timedRunRepeatsUntilCpuReturnsTerminalState() throws Exception {
        when(cpu.step()).thenAnswer(call -> {
            int step = stepCount.incrementAndGet();
            steps.add(step);
            return step == 3 ? STATE_STOPPED_NORMAL : STATE_STOPPED_BREAK;
        });

        try (EmulationController controller = controller(cpu)) {
            await(controller.step(1, TimeUnit.MILLISECONDS));

            awaitStep(1);
            awaitStep(2);
            awaitStep(3);
            assertNoStep();
            assertFalse(controller.snapshot().isTimedRunning());
            assertEquals(STATE_STOPPED_NORMAL, controller.snapshot().runState());
        }
    }

    @Test(timeout = 5000)
    public void replacementDuringInstructionDoesNotReviveOldTimer() throws Exception {
        RunGate gate = new RunGate(STATE_STOPPED_BREAK);
        doAnswer(call -> {
            int step = stepCount.incrementAndGet();
            if (step == 1) gate.run();
            steps.add(step);
            return STATE_STOPPED_BREAK;
        }).when(cpu).step();
        EmulationController controller = controller(cpu);

        try {
            await(controller.step(1, TimeUnit.MILLISECONDS));
            gate.awaitEntered();
            await(controller.step(1, TimeUnit.DAYS));
            gate.release.countDown();

            awaitStep(1);
            assertNoStep();
            assertTrue(controller.snapshot().isTimedRunning());
            assertEquals(1, stepCount.get());
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void pauseCancelsInFlightTimerBeforeRestart() throws Exception {
        RunGate gate = new RunGate(STATE_STOPPED_BREAK);
        doAnswer(call -> {
            int step = stepCount.incrementAndGet();
            if (step == 1) gate.run();
            steps.add(step);
            return STATE_STOPPED_BREAK;
        }).when(cpu).step();
        EmulationController controller = controller(cpu);

        try {
            await(controller.step(1, TimeUnit.MILLISECONDS));
            gate.awaitEntered();
            CompletableFuture<CPU.RunState> paused = controller.pause();
            gate.awaitInterrupted();
            assertPending(paused);
            gate.release.countDown();
            await(paused);
            awaitStep(1);
            await(controller.step(1, TimeUnit.DAYS));

            awaitStep(2);
            assertNoStep();
            assertEquals(2, stepCount.get());
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void timedDelayStartsAfterPreviousInstructionCompletes() throws Exception {
        RunGate gate = new RunGate(STATE_STOPPED_BREAK);
        BlockingQueue<Long> completed = new LinkedBlockingQueue<>();
        BlockingQueue<Long> started = new LinkedBlockingQueue<>();
        doAnswer(call -> {
            int step = stepCount.incrementAndGet();
            started.add(System.nanoTime());
            if (step == 1) {
                gate.run();
                completed.add(System.nanoTime());
            }
            return step == 2 ? STATE_STOPPED_NORMAL : STATE_STOPPED_BREAK;
        }).when(cpu).step();
        EmulationController controller = controller(cpu);

        try {
            await(controller.step(200, TimeUnit.MILLISECONDS));
            gate.awaitEntered();
            assertNotNull(started.poll(1, TimeUnit.SECONDS));
            assertNull("A blocked instruction must not create additional ticks", started.poll(300, TimeUnit.MILLISECONDS));
            gate.release.countDown();
            Long firstCompleted = completed.poll(1, TimeUnit.SECONDS);
            Long secondStarted = started.poll(1, TimeUnit.SECONDS);

            assertNotNull(firstCompleted);
            assertNotNull(secondStarted);
            assertTrue("Delay must follow actual completion", secondStarted - firstCompleted >= TimeUnit.MILLISECONDS.toNanos(200));
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void closeCancelsDelayedTicksAndRejectsFurtherCommands() throws Exception {
        EmulationController controller = controller(cpu);
        await(controller.step(1, TimeUnit.DAYS));
        awaitStep(1);

        await(controller.closeAsync());

        assertFalse(controller.snapshot().isTimedRunning());
        assertNoStep();
        assertThrows(ExecutionException.class, () -> await(controller.step()));
        await(controller.closeAsync());
        await(controller.closeAsync());
    }

    @Test(timeout = 5000)
    public void frequencySamplingSharesCommandThreadAndBothHostThreadsExitOnClose() throws Exception {
        RunGate gate = new RunGate(STATE_RUNNING);
        Set<Thread> threads = ConcurrentHashMap.newKeySet();
        CountDownLatch sampled = new CountDownLatch(1);
        AtomicReference<Thread> commandThread = new AtomicReference<>();
        when(cpu.execute()).thenAnswer(call -> {
            threads.add(Thread.currentThread());
            return gate.run();
        });
        doAnswer(call -> {
            threads.add(Thread.currentThread());
            assertSame(commandThread.get(), Thread.currentThread());
            sampled.countDown();
            return null;
        }).when(cpu).sampleFrequency();
        EmulationController controller = controller(cpu);
        controller.executionState().subscribe(listener(state -> commandThread.set(Thread.currentThread())));

        try {
            await(controller.start());
            gate.awaitEntered();
            assertTrue(sampled.await(2, TimeUnit.SECONDS));

            assertEquals(2, threads.size());
            assertEquals(Set.of("machine-commands", "machine-cpu"),
                    threads.stream().map(Thread::getName).collect(java.util.stream.Collectors.toSet()));
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }

        for (Thread thread : threads) {
            thread.join(1000);
            assertFalse("Closed machine must not retain " + thread.getName(), thread.isAlive());
        }
    }

    private void awaitStep(int expected) throws InterruptedException {
        assertEquals(Integer.valueOf(expected), steps.poll(2, TimeUnit.SECONDS));
    }

    private void assertNoStep() throws InterruptedException {
        assertNull("Cancelled or terminal timer must not execute another instruction", steps.poll(100, TimeUnit.MILLISECONDS));
    }
}
