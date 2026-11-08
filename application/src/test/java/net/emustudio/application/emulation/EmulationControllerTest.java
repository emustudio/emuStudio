/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static net.emustudio.emulib.plugins.cpu.CPU.RunState.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EmulationControllerTest {
    @Test(timeout = 5000)
    public void startAcknowledgesRunningBeforeExecutionExits() throws Exception {
        CPU cpu = mock(CPU.class);
        RunGate gate = new RunGate(STATE_STOPPED_NORMAL);
        when(cpu.execute()).thenAnswer(call -> gate.run());
        EmulationController controller = controller(cpu);

        try {
            assertEquals(STATE_RUNNING, await(controller.start()));
            gate.awaitEntered();

            assertFalse(controller.runCompletion().isDone());
            assertEquals(STATE_RUNNING, controller.runState());
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void pauseWaitsForActualExecutionExitBeforePublishingPaused() throws Exception {
        CPU cpu = mock(CPU.class);
        RunGate gate = new RunGate(STATE_RUNNING);
        when(cpu.execute()).thenAnswer(call -> gate.run());
        EmulationController controller = controller(cpu);
        AtomicBoolean prematureNotification = new AtomicBoolean();
        controller.addCPUListener(listener(state -> {
            if (state == STATE_STOPPED_BREAK && !gate.exited.get()) prematureNotification.set(true);
        }));

        try {
            await(controller.start());
            gate.awaitEntered();
            CompletableFuture<CPU.RunState> paused = controller.pause();
            gate.awaitInterrupted();

            assertPending(paused);
            assertEquals(STATE_RUNNING, controller.runState());
            assertTrue(controller.isBusy());
            gate.release.countDown();

            assertEquals(STATE_STOPPED_BREAK, await(paused));
            assertFalse(prematureNotification.get());
            assertFalse(controller.isBusy());
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void resetRemainsPendingAndResetsMemoryDevicesThenCpuAfterExit() throws Exception {
        CPU cpu = mock(CPU.class);
        Memory memory = mock(Memory.class);
        Device device = mock(Device.class);
        RunGate gate = new RunGate(STATE_RUNNING);
        List<String> order = new CopyOnWriteArrayList<>();
        when(cpu.execute()).thenAnswer(call -> {
            CPU.RunState result = gate.run();
            order.add("exit");
            return result;
        });
        doAnswer(call -> { order.add("memory"); return null; }).when(memory).reset();
        doAnswer(call -> { order.add("device"); return null; }).when(device).reset();
        doAnswer(call -> { order.add("cpu"); return null; }).when(cpu).reset(123);
        EmulationController controller = new EmulationController(cpu, memory, List.of(device), null);

        try {
            await(controller.start());
            gate.awaitEntered();
            CompletableFuture<CPU.RunState> reset = controller.reset(123);
            gate.awaitInterrupted();

            assertPending(reset);
            assertTrue(order.isEmpty());
            gate.release.countDown();

            assertEquals(STATE_STOPPED_BREAK, await(reset));
            assertEquals(List.of("exit", "memory", "device", "cpu"), order);
            assertFalse(controller.isBusy());
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void stoppingRejectsNewMutationsWithoutRunningThemLater() throws Exception {
        CPU cpu = mock(CPU.class);
        RunGate gate = new RunGate(STATE_RUNNING);
        when(cpu.execute()).thenAnswer(call -> gate.run());
        AtomicBoolean edited = new AtomicBoolean();
        EmulationController controller = controller(cpu);

        try {
            await(controller.start());
            gate.awaitEntered();
            CompletableFuture<CPU.RunState> paused = controller.pause();
            gate.awaitInterrupted();

            assertRejected(controller.start());
            assertRejected(controller.step());
            assertRejected(controller.reset());
            assertRejected(controller.mutate(() -> edited.set(true)));
            gate.release.countDown();
            await(paused);

            assertFalse(edited.get());
            verify(cpu, never()).step();
            verify(cpu, never()).reset();
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void naturalHaltWinsRaceWithPause() throws Exception {
        assertTerminalResultWins(STATE_STOPPED_NORMAL, false);
    }

    @Test(timeout = 5000)
    public void instructionFailureWinsRaceWithStop() throws Exception {
        assertTerminalResultWins(STATE_STOPPED_BAD_INSTR, true);
    }

    @Test(timeout = 5000)
    public void addressFailureWinsRaceWithPause() throws Exception {
        assertTerminalResultWins(STATE_STOPPED_ADDR_FALLOUT, false);
    }

    @Test(timeout = 5000)
    public void naturalBreakpointWinsRaceWithStop() throws Exception {
        assertTerminalResultWins(STATE_STOPPED_BREAK, true);
    }

    @Test(timeout = 5000)
    public void resetDuringManualInstructionWaitsForExitEvenWhenCallerCancelsView() throws Exception {
        CPU cpu = mock(CPU.class);
        RunGate gate = new RunGate(STATE_STOPPED_BREAK);
        when(cpu.step()).thenAnswer(call -> gate.run());
        EmulationController controller = controller(cpu);

        try {
            CompletableFuture<CPU.RunState> step = controller.step();
            gate.awaitEntered();
            CompletableFuture<CPU.RunState> reset = controller.reset();
            gate.awaitInterrupted();
            reset.cancel(true);
            CompletableFuture<Void> closed = controller.closeAsync();

            assertPending(closed);
            verify(cpu, never()).reset();
            gate.release.countDown();
            await(step);
            await(closed);

            verify(cpu).reset();
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void cancelledCloseViewCannotAcknowledgeExecutionExit() throws Exception {
        CPU cpu = mock(CPU.class);
        RunGate gate = new RunGate(STATE_RUNNING);
        when(cpu.execute()).thenAnswer(call -> gate.run());
        EmulationController controller = controller(cpu);

        try {
            await(controller.start());
            gate.awaitEntered();
            CompletableFuture<Void> abandoned = controller.closeAsync();
            gate.awaitInterrupted();
            abandoned.cancel(true);
            CompletableFuture<Void> actualClose = controller.closeAsync();

            assertPending(actualClose);
            assertFalse(gate.exited.get());
            gate.release.countDown();
            await(actualClose);

            assertTrue(gate.exited.get());
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void thrownFailureCompletesRunExceptionallyAndStillAllowsReset() throws Exception {
        CPU cpu = mock(CPU.class);
        IllegalStateException failure = new IllegalStateException("broken instruction");
        when(cpu.execute()).thenThrow(failure);

        try (EmulationController controller = controller(cpu)) {
            await(controller.start());

            assertSame(failure, assertThrows(ExecutionException.class,
                    () -> await(controller.runCompletion())).getCause());
            assertEquals(STATE_STOPPED_BAD_INSTR, controller.runState());
            assertEquals(STATE_STOPPED_BREAK, await(controller.reset()));
            verify(cpu).reset();
        }
    }

    @Test(timeout = 5000)
    public void immediatePauseAfterStartDoesNotLoseInterruption() throws Exception {
        CPU cpu = mock(CPU.class);
        when(cpu.execute()).thenAnswer(call -> {
            while (!Thread.currentThread().isInterrupted()) Thread.yield();
            return STATE_RUNNING;
        });

        try (EmulationController controller = controller(cpu)) {
            await(controller.start());

            assertEquals(STATE_STOPPED_BREAK, await(controller.pause()));
            assertEquals(STATE_STOPPED_BREAK, await(controller.runCompletion()));
        }
    }

    @Test(timeout = 5000)
    public void cancelledRunFutureDoesNotPretendExecutionExited() throws Exception {
        CPU cpu = mock(CPU.class);
        RunGate gate = new RunGate(STATE_RUNNING);
        when(cpu.execute()).thenAnswer(call -> gate.run());
        EmulationController controller = controller(cpu);

        try {
            await(controller.start());
            gate.awaitEntered();
            assertTrue(controller.runCompletion().cancel(true));

            CompletableFuture<CPU.RunState> paused = controller.pause();
            gate.awaitInterrupted();
            assertPending(paused);
            assertFalse(gate.exited.get());
            gate.release.countDown();

            assertEquals(STATE_STOPPED_BREAK, await(paused));
            assertFalse(controller.isBusy());
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void closeRemainsPendingUntilExecutionAndPendingResetFinish() throws Exception {
        CPU cpu = mock(CPU.class);
        Memory memory = mock(Memory.class);
        RunGate gate = new RunGate(STATE_RUNNING);
        when(cpu.execute()).thenAnswer(call -> gate.run());
        EmulationController controller = new EmulationController(cpu, memory, List.of(), null);

        try {
            await(controller.start());
            gate.awaitEntered();
            CompletableFuture<CPU.RunState> reset = controller.reset();
            gate.awaitInterrupted();
            CompletableFuture<Void> closed = controller.closeAsync();

            assertPending(closed);
            assertRejected(controller.step());
            verify(memory, never()).reset();
            verify(cpu, never()).destroy();
            gate.release.countDown();

            assertEquals(STATE_STOPPED_BREAK, await(reset));
            await(closed);
            verify(memory).reset();
            verify(cpu).reset();
            verify(cpu, never()).destroy();
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    @Test(timeout = 5000)
    public void stepAndPausedEditRunOnSameCpuWorkerAndPublishBeforeCompletion() throws Exception {
        CPU cpu = mock(CPU.class);
        AtomicReference<Thread> stepThread = new AtomicReference<>();
        AtomicReference<Thread> editThread = new AtomicReference<>();
        List<CPU.RunState> states = new CopyOnWriteArrayList<>();
        when(cpu.step()).thenAnswer(call -> {
            stepThread.set(Thread.currentThread());
            return STATE_RUNNING;
        });

        try (EmulationController controller = controller(cpu)) {
            controller.addCPUListener(listener(states::add));

            assertEquals(STATE_STOPPED_BREAK, await(controller.step()));
            assertEquals(STATE_STOPPED_BREAK, states.get(states.size() - 1));
            await(controller.mutate(() -> editThread.set(Thread.currentThread())));

            assertSame(stepThread.get(), editThread.get());
            assertEquals("machine-cpu", editThread.get().getName());
        }
    }

    @Test(timeout = 5000)
    public void throwingListenerCannotStrandCommandCompletion() throws Exception {
        CPU cpu = mock(CPU.class);
        when(cpu.step()).thenReturn(STATE_STOPPED_BREAK);

        try (EmulationController controller = controller(cpu)) {
            controller.addCPUListener(listener(state -> { throw new IllegalStateException("observer failed"); }));

            assertEquals(STATE_STOPPED_BREAK, await(controller.step()));
            assertEquals(STATE_STOPPED_BREAK, await(controller.reset()));
        }
    }

    @Test(timeout = 5000)
    public void failedPausedEditPreservesRunStateAndAllowsAnotherEdit() throws Exception {
        CPU cpu = mock(CPU.class);
        IllegalArgumentException failure = new IllegalArgumentException("register value outside range");
        AtomicBoolean edited = new AtomicBoolean();

        try (EmulationController controller = controller(cpu)) {
            CompletableFuture<Void> rejected = controller.mutate(() -> { throw failure; });

            assertSame(failure, assertThrows(ExecutionException.class, () -> await(rejected)).getCause());
            assertEquals(STATE_STOPPED_BREAK, controller.runState());
            assertFalse(controller.isBusy());
            await(controller.mutate(() -> edited.set(true)));
            assertTrue(edited.get());
        }
    }

    private void assertTerminalResultWins(CPU.RunState terminal, boolean stop) throws Exception {
        CPU cpu = mock(CPU.class);
        RunGate gate = new RunGate(terminal);
        when(cpu.execute()).thenAnswer(call -> gate.run());
        EmulationController controller = controller(cpu);

        try {
            await(controller.start());
            gate.awaitEntered();
            CompletableFuture<CPU.RunState> stopped = stop ? controller.stop() : controller.pause();
            gate.awaitInterrupted();
            gate.release.countDown();

            assertEquals(terminal, await(stopped));
            assertEquals(terminal, await(controller.runCompletion()));
            assertEquals(terminal, controller.runState());
        } finally {
            gate.release.countDown();
            await(controller.closeAsync());
        }
    }

    static EmulationController controller(CPU cpu) {
        return new EmulationController(cpu, null, List.of(), null);
    }

    static CPU.CPUListener listener(java.util.function.Consumer<CPU.RunState> changed) {
        return new CPU.CPUListener() {
            @Override public void runStateChanged(CPU.RunState state) { changed.accept(state); }
            @Override public void internalStateChanged() { }
        };
    }

    static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(2, TimeUnit.SECONDS);
    }

    static void assertPending(CompletableFuture<?> future) {
        assertThrows(TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
        assertFalse(future.isDone());
    }

    static void assertRejected(CompletableFuture<?> future) throws Exception {
        assertTrue(assertThrows(ExecutionException.class, () -> await(future)).getCause() instanceof IllegalStateException);
    }

    static final class RunGate {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicBoolean exited = new AtomicBoolean();
        private final CPU.RunState result;

        RunGate(CPU.RunState result) { this.result = result; }

        CPU.RunState run() {
            entered.countDown();
            while (true) {
                try {
                    release.await();
                    exited.set(true);
                    return result;
                } catch (InterruptedException ignored) { interrupted.countDown(); }
            }
        }

        void awaitEntered() throws InterruptedException { assertTrue(entered.await(2, TimeUnit.SECONDS)); }
        void awaitInterrupted() throws InterruptedException { assertTrue(interrupted.await(2, TimeUnit.SECONDS)); }
    }
}
