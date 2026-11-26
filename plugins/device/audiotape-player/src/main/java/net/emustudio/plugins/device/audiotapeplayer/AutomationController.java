/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotapeplayer;

import net.emustudio.emulib.Numbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Runs a tape event sequence and acknowledges when its worker has stopped. */
public class AutomationController implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationController.class);

    public enum State {
        PLAYING, STOPPED, CLOSED
    }

    public interface AutomationListener {
        void stateChanged(State state);

        void currentIndexChanged(int index);
    }

    private final TapePlaybackController controller;
    private volatile AutomationListener listener;
    private boolean closed;
    private TapeTask task;

    public AutomationController(TapePlaybackController controller) {
        this.controller = Objects.requireNonNull(controller);
    }

    public void reset() {
        stop().handle((ignored, failure) -> null).join();
    }

    public void setListener(AutomationListener listener) {
        this.listener = listener;
    }

    public synchronized boolean isPlaying() {
        return task != null && task.isRunning();
    }

    public synchronized CompletableFuture<Void> play(List<AutomationEvent> events) {
        if (!closed && !isPlaying()) {
            List<AutomationEvent> sequence = List.copyOf(events);
            TapeTask next = new TapeTask();
            task = next;
            next.start("tape-automation", () -> playEvents(sequence, next));
        }
        return task == null ? CompletableFuture.completedFuture(null) : task.ready.copy();
    }

    private void playEvents(List<AutomationEvent> events, TapeTask task) {
        Throwable failure = null;
        int index = 0;
        LOGGER.info("AudioTape started with {} events", events.size());
        try {
            AutomationListener observer = listener;
            if (observer != null) observer.stateChanged(State.PLAYING);
            for (; index < events.size(); index++) {
                if (Thread.interrupted()) throw new InterruptedException();
                if (observer != null) observer.currentIndexChanged(index);
                AutomationEvent event = events.get(index);
                LOGGER.info("AudioTape event [{}]: {}", index, event.getDescription());
                executeEvent(event, task);
            }
            LOGGER.info("AudioTape finished");
        } catch (InterruptedException stopped) {
            LOGGER.info("AudioTape interrupted at event [{}]", index);
        } catch (ExecutionException error) {
            failure = error.getCause();
        } catch (Throwable error) {
            failure = error;
        }
        finish(task, failure);
    }

    private synchronized void finish(TapeTask task, Throwable failure) {
        try {
            AutomationListener observer = listener;
            if (observer != null) observer.stateChanged(closed ? State.CLOSED : State.STOPPED);
        } catch (Throwable cleanup) {
            if (failure == null) failure = cleanup;
            else if (failure != cleanup) failure.addSuppressed(cleanup);
        }
        if (failure != null) LOGGER.error("AudioTape sequence failed", failure);
        task.finish(failure);
    }

    public synchronized CompletableFuture<Void> stop() {
        if (task == null) return CompletableFuture.completedFuture(null);
        task.requestStop();
        return task.completion.copy();
    }

    synchronized CompletableFuture<Void> shutdown() {
        if (!closed) {
            closed = true;
            if (!isPlaying()) {
                AutomationListener observer = listener;
                try {
                    if (observer != null) observer.stateChanged(State.CLOSED);
                } catch (Throwable failure) {
                    return CompletableFuture.failedFuture(failure);
                }
            }
        }
        return stop();
    }

    @Override
    public void close() {
        shutdown().handle((ignored, failure) -> null).join();
    }

    private void executeEvent(AutomationEvent event, TapeTask task) throws InterruptedException, ExecutionException {
        switch (event.getType()) {
            case LOAD_TAPE:
                String path = event.getParameter();
                if (path.isEmpty()) LOGGER.warn("LOAD_TAPE event has no path, skipping");
                else controller.load(Path.of(path));
                break;
            case DELAY:
                int seconds = Numbers.parseRadix(event.getParameter());
                if (seconds > 0) {
                    task.ready.complete(null);
                    TimeUnit.SECONDS.sleep(seconds);
                }
                break;
            case PLAY:
                TapeTask playback = controller.play();
                if (playback != null) {
                    try {
                        playback.ready.get();
                        task.ready.complete(null);
                        playback.completion.get();
                    } catch (InterruptedException stopped) {
                        controller.stop(false).join();
                        throw stopped;
                    }
                }
                break;
            case STOP:
                controller.stop(false).get();
                break;
            case RESET:
                controller.reset();
                break;
            case UNLOAD:
                controller.stop(true).get();
                break;
        }
    }
}
