/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;

import static net.emustudio.emulib.runtime.helpers.SleepUtils.preciseSleepNanos;

/**
 * Executes a list of automation events sequentially.
 * Designed to run on a background thread.
 */
@ThreadSafe
public class AutomationController implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationController.class);

    public enum State {
        PLAYING,
        STOPPED,
        CLOSED // terminal state
    }

    public interface AutomationListener {
        void stateChanged(State state);

        void currentIndexChanged(int index);
    }

    private final ExecutorService pool = Executors.newFixedThreadPool(1);
    private final Object lock = new Object();
    @GuardedBy("lock")
    private State state = State.STOPPED;
    @GuardedBy("lock")
    private Future<?> future;

    private final Queue<State> stateNotifications = new ConcurrentLinkedQueue<>();

    private AutomationListener listener;
    private final RadixUtils radixUtils = RadixUtils.getInstance();
    private final TapePlaybackController controller;

    public AutomationController(TapePlaybackController controller) {
        this.controller = Objects.requireNonNull(controller);
    }

    public void reset() {
        stop();
    }

    public void setListener(AutomationListener listener) {
        this.listener = listener;
    }

    public boolean isPlaying() {
        synchronized (lock) {
            return this.state == State.PLAYING;
        }
    }

    public void play(List<AutomationEvent> events) {
        AutomationListener tmpListener = listener;

        synchronized (lock) {
            if (this.state == State.STOPPED) {
                this.state = State.PLAYING;

                LOGGER.info("AudioTape started with {} events", events.size());
                this.future = pool.submit(() -> {
                    int currrentIndex = 0;
                    try {
                        for (; currrentIndex < events.size(); currrentIndex++) {
                            if (tmpListener != null) {
                                tmpListener.currentIndexChanged(currrentIndex);
                            }

                            AutomationEvent event = events.get(currrentIndex);
                            LOGGER.info("AudioTape event [{}]: {}", currrentIndex, event.getDescription());
                            executeEvent(event);
                        }
                        LOGGER.info("AudioTape finished");
                    } catch (InterruptedException e) {
                        LOGGER.info("AudioTape interrupted at event [{}]", currrentIndex);
                        controller.stop(false);
                        Thread.currentThread().interrupt();
                    } finally {
                        synchronized (lock) {
                            this.state = this.state == State.CLOSED ? this.state : State.STOPPED;
                            stateNotifications.add(this.state);
                        }
                        notifyStateChange();
                    }
                });
            }
        }
    }

    public void stop() {
        synchronized (lock) {
            if (this.state == State.PLAYING) {
                Future<?> tmpFuture = this.future;
                this.future = null;
                if (tmpFuture != null) {
                    tmpFuture.cancel(true);
                }
                this.state = State.STOPPED;
            }
            stateNotifications.add(this.state);
        }
        notifyStateChange();
    }

    @Override
    public void close() {
        synchronized (lock) {
            this.state = State.CLOSED;
            Future<?> tmpFuture = this.future;
            this.future = null;
            if (tmpFuture != null) {
                tmpFuture.cancel(true);
            }
            pool.shutdown();
            stateNotifications.add(this.state);
        }
        notifyStateChange();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void executeEvent(AutomationEvent event) throws InterruptedException {
        switch (event.getType()) {
            case LOAD_TAPE:
                String path = event.getParameter();
                if (!path.isEmpty()) {
                    controller.load(Path.of(path));
                } else {
                    LOGGER.warn("LOAD_TAPE event has no path, skipping");
                }
                break;
            case DELAY:
                int seconds = radixUtils.parseRadix(event.getParameter());
                if (seconds > 0) {
                    preciseSleepNanos(TimeUnit.SECONDS.toNanos(seconds));
                }
                break;
            case PLAY:
                controller.play();
                waitForPlaybackEnd();
                break;
            case STOP:
                controller.stop(false);
                break;
            case RESET:
                controller.reset();
                break;
            case UNLOAD:
                controller.stop(true);
                break;
        }
    }

    private void waitForPlaybackEnd() throws InterruptedException {
        do {
            TapePlaybackController.CassetteState state = controller.getState();
            if (state != TapePlaybackController.CassetteState.PLAYING) {
                break;
            }
            preciseSleepNanos(TimeUnit.MILLISECONDS.toNanos(100));
        } while (getState() == State.PLAYING);
    }

    private State getState() {
        synchronized (lock) {
            return this.state;
        }
    }

    private void notifyStateChange() {
        State notification = stateNotifications.poll();
        AutomationListener tmpListener = listener;
        if (notification != null && tmpListener != null) {
            tmpListener.stateChanged(notification);
        }
    }
}
