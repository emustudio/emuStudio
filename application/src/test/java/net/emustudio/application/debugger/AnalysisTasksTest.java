/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class AnalysisTasksTest {
    @Test(timeout = 15000)
    public void metadataEditRejectsLateResultButBreakpointEditDoesNot() throws Exception {
        for (boolean metadata : new boolean[] {true, false}) {
            DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
            try (DebuggerSession session = machine.session(null)) {
                Completion completion = new Completion();
                AnalysisTasks tasks = new AnalysisTasks(session, completion::busyChanged, completion.failure::set);
                CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
                AtomicBoolean applied = new AtomicBoolean();
                try {
                    SwingUtilities.invokeAndWait(() -> tasks.submit(() -> {
                        entered.countDown();
                        if (!release.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("Missing release");
                        return 42;
                    }, value -> applied.set(true)));
                    assertTrue(entered.await(2, TimeUnit.SECONDS));
                    if (metadata) {
                        session.annotationEngine.put(new net.emustudio.emulib.runtime.annotations.MemoryAnnotations.Entry(
                                7, 1, 0, 1, "comment", "changed", null));
                    } else {
                        session.setBreakpoint(0, true).join();
                    }
                    release.countDown();
                    completion.await();
                    assertEquals(!metadata, applied.get());
                    if (metadata) assertTrue(completion.failure.get().getMessage().contains("metadata changed"));
                    else assertNull(completion.failure.get());
                } finally {
                    release.countDown();
                    SwingUtilities.invokeAndWait(tasks::close);
                }
            }
        }
    }
    @Test(timeout = 15000)
    public void computesOffEventThreadAndAppliesResultOnEventThread() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
            Completion completion = new Completion();
            AnalysisTasks tasks = new AnalysisTasks(session, completion::busyChanged, completion.failure::set);
            AtomicBoolean workerOnEventThread = new AtomicBoolean(true);
            AtomicBoolean applyOnEventThread = new AtomicBoolean();
            AtomicReference<String> applied = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> tasks.submit(() -> {
                    workerOnEventThread.set(SwingUtilities.isEventDispatchThread());
                    return "decoded instructions";
                }, result -> {
                    applyOnEventThread.set(SwingUtilities.isEventDispatchThread());
                    applied.set(result);
                }));
                completion.await();

                assertFalse(workerOnEventThread.get());
                assertTrue(applyOnEventThread.get());
                assertEquals("decoded instructions", applied.get());
                assertEquals(List.of(true, false), completion.busyStates);
                assertNull(completion.failure.get());
                SwingUtilities.invokeAndWait(() -> assertFalse(tasks.isBusy()));
            } finally {
                SwingUtilities.invokeAndWait(tasks::close);
            }
        }
    }

    @Test(timeout = 15000)
    public void operationFailureReportsOriginalCauseAndAllowsAnotherTask() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
            Completion completion = new Completion();
            AnalysisTasks tasks = new AnalysisTasks(session, completion::busyChanged, completion.failure::set);
            IllegalArgumentException cause = new IllegalArgumentException("bad pattern");
            AtomicBoolean applied = new AtomicBoolean();
            try {
                SwingUtilities.invokeAndWait(() -> tasks.submit(() -> { throw cause; }, value -> applied.set(true)));
                completion.await();
                assertSame(cause, completion.failure.get());
                assertFalse(applied.get());

                CountDownLatch recovered = new CountDownLatch(1);
                SwingUtilities.invokeAndWait(() -> tasks.submit(() -> 7, value -> {
                    assertEquals(Integer.valueOf(7), value);
                    recovered.countDown();
                }));
                assertTrue("Next analysis should apply", recovered.await(2, TimeUnit.SECONDS));
            } finally {
                SwingUtilities.invokeAndWait(tasks::close);
            }
        }
    }

    @Test(timeout = 15000)
    public void machineMutationWhileAnalysisRunsDiscardsItsResult() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
            Completion completion = new Completion();
            AnalysisTasks tasks = new AnalysisTasks(session, completion::busyChanged, completion.failure::set);
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicBoolean applied = new AtomicBoolean();
            try {
                SwingUtilities.invokeAndWait(() -> tasks.submit(() -> {
                    entered.countDown();
                    if (!release.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("Test did not release analysis");
                    return 42;
                }, value -> applied.set(true)));
                assertTrue("Analysis should start", entered.await(2, TimeUnit.SECONDS));
                session.runMutation(() -> machine.memoryChanged(0));
                release.countDown();
                completion.await();

                assertFalse(applied.get());
                assertTrue(completion.failure.get() instanceof IllegalStateException);
                assertTrue(completion.failure.get().getMessage().contains("machine changed"));
            } finally {
                release.countDown();
                SwingUtilities.invokeAndWait(tasks::close);
            }
        }
    }

    @Test(timeout = 15000)
    public void cancellingDoesNotReleaseBusyStateBeforeOperationActuallyExits() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
            Completion completion = new Completion();
            AnalysisTasks tasks = new AnalysisTasks(session, completion::busyChanged, completion.failure::set);
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch interrupted = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicBoolean applied = new AtomicBoolean();
            try {
                // Simulate an operation that must finish cleanup after receiving cancellation.
                SwingUtilities.invokeAndWait(() -> tasks.submit(() -> {
                    entered.countDown();
                    try {
                        if (!release.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("Missing cancellation");
                    } catch (InterruptedException expected) {
                        interrupted.countDown();
                        if (!release.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("Missing cleanup release");
                    }
                    return 42;
                }, value -> applied.set(true)));
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                SwingUtilities.invokeAndWait(tasks::cancel);
                assertTrue("Worker should observe cancellation", interrupted.await(2, TimeUnit.SECONDS));

                SwingUtilities.invokeAndWait(() -> {
                    assertTrue(tasks.isBusy());
                    assertThrows(IllegalStateException.class, () -> tasks.submit(() -> 99, value -> {}));
                });
                release.countDown();
                completion.await();
                assertFalse(applied.get());
                assertEquals("Analysis cancelled", completion.failure.get().getMessage());
                assertEquals(List.of(true, false), completion.busyStates);
            } finally {
                release.countDown();
                SwingUtilities.invokeAndWait(tasks::close);
            }
        }
    }

    @Test(timeout = 15000)
    public void controlsRequireEventThreadAndPausedSession() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
            AnalysisTasks tasks = new AnalysisTasks(session, busy -> {}, failure -> {});
            try {
                assertThrows(IllegalStateException.class, () -> tasks.submit(() -> 1, value -> {}));
                assertThrows(IllegalStateException.class, tasks::cancel);
                assertThrows(IllegalStateException.class, tasks::close);
                machine.state(CPU.RunState.STATE_RUNNING);
                SwingUtilities.invokeAndWait(() -> {
                    assertThrows(IllegalStateException.class, () -> tasks.submit(() -> 1, value -> {}));
                    assertFalse(tasks.isBusy());
                });
            } finally {
                SwingUtilities.invokeAndWait(tasks::close);
            }
        }
    }

    @Test(timeout = 15000)
    public void closedWindowRejectsNewAnalysis() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{0}}, 1);
        try (DebuggerSession session = machine.session(null)) {
            AnalysisTasks tasks = new AnalysisTasks(session, busy -> {}, failure -> {});
            SwingUtilities.invokeAndWait(() -> {
                tasks.close();
                assertFalse(tasks.isBusy());
                assertThrows(IllegalStateException.class, () -> tasks.submit(() -> 1, value -> {}));
            });
        }
    }

    private static final class Completion {
        final List<Boolean> busyStates = new ArrayList<>();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch finished = new CountDownLatch(1);

        void busyChanged(boolean busy) {
            assertTrue("Busy notification belongs on event thread", SwingUtilities.isEventDispatchThread());
            busyStates.add(busy);
            if (!busy) finished.countDown();
        }

        void await() throws InterruptedException {
            assertTrue("Analysis should finish", finished.await(2, TimeUnit.SECONDS));
        }
    }
}
