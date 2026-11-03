/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/** One background analysis at a time, with results confined to the same paused CPU generation. */
final class AnalysisTasks implements AutoCloseable {
    private final DebugSession session;
    private final Consumer<Boolean> busy;
    private final Consumer<Throwable> failure;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(
                    task -> {
                        Thread thread = new Thread(task, "debugger-analysis");
                        thread.setDaemon(true);
                        return thread;
                    });
    private FutureTask<?> active;
    private boolean closed;

    AnalysisTasks(DebugSession session, Consumer<Boolean> busy, Consumer<Throwable> failure) {
        this.session = session;
        this.busy = busy;
        this.failure = failure;
    }

    boolean isBusy() {
        return active != null;
    }

    <T> void submit(Callable<T> operation, Consumer<T> apply) {
        submit(operation, apply, false);
    }

    <T> void submit(Callable<T> operation, Consumer<T> apply, boolean publishesContent) {
        requireEdt();
        if (closed) throw new IllegalStateException("Analysis window closed");
        if (active != null) throw new IllegalStateException("Analysis already in progress");
        long generation = session.pausedGeneration();
        ProgramIndex index = session.memory == null ? null : session.programIndex();
        ProgramIndex.Snapshot base = index == null ? null : index.snapshot();
        long memoryRevision = base == null ? 0 : base.memoryRevision();
        long contentRevision = base == null ? 0 : base.contentRevision();
        FutureTask<T> task =
                new FutureTask<T>(operation) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } finally {
                            // Future cancellation does not establish that the operation has exited.
                            SwingUtilities.invokeLater(() -> complete(this, generation, index, memoryRevision,
                                    publishesContent ? -1 : contentRevision, apply));
                        }
                    }
                };
        active = task;
        busy.accept(true);
        executor.execute(task);
    }

    private <T> void complete(FutureTask<T> task, long generation, ProgramIndex index,
                              long memoryRevision, long contentRevision, Consumer<T> apply) {
        if (closed || active != task) return;
        active = null;
        try {
            session.applyPaused(generation, () -> {
                ProgramIndex.Snapshot current = index == null ? null : index.snapshot();
                if (index != null && (session.programIndex() != index
                        || current.memoryRevision() != memoryRevision))
                    throw new IllegalStateException("Analysis discarded: memory image changed");
                if (current != null && contentRevision >= 0 && current.contentRevision() != contentRevision)
                    throw new IllegalStateException("Analysis discarded: source or metadata changed");
                apply.accept(result(task));
            });
        } catch (CancellationException e) {
            failure.accept(new IllegalStateException("Analysis cancelled", e));
        } catch (RuntimeException e) {
            failure.accept(e.getCause() == null ? e : e.getCause());
        } finally {
            busy.accept(false);
        }
    }

    private static <T> T result(FutureTask<T> task) {
        try {
            return task.get();
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }

    void cancel() {
        requireEdt();
        if (active != null) active.cancel(true);
    }

    @Override
    public void close() {
        requireEdt();
        closed = true;
        cancel();
        active = null;
        // Let a cancelled task that has not started run its exit/finally path.
        executor.shutdown();
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Analysis controls require Swing event thread");
    }
}
