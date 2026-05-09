/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Executes a list of automation events sequentially.
 * Designed to run on a background thread.
 */
@ThreadSafe
public class AutomationRunner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationRunner.class);
    private final TapePlaybackController controller;
    private final List<AutomationEvent> events;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger currentEventIndex = new AtomicInteger(-1);
    private volatile IntConsumer eventIndexListener;

    public AutomationRunner(TapePlaybackController controller, List<AutomationEvent> events) {
        this.controller = Objects.requireNonNull(controller);
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
    }

    public void setEventIndexListener(IntConsumer listener) {
        this.eventIndexListener = listener;
    }

    public List<AutomationEvent> getEvents() {
        return events;
    }

    public int getCurrentEventIndex() {
        return currentEventIndex.get();
    }

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void run() {
        LOGGER.info("Automation started with {} events", events.size());
        for (int i = 0; i < events.size() && !cancelled.get(); i++) {
            currentEventIndex.set(i);
            notifyListener(i);
            AutomationEvent event = events.get(i);
            LOGGER.info("Automation event [{}]: {}", i, event.getDescription());
            try {
                executeEvent(event);
            } catch (InterruptedException e) {
                LOGGER.info("Automation interrupted at event [{}]", i);
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!cancelled.get()) {
            currentEventIndex.set(events.size());
            notifyListener(events.size());
            LOGGER.info("Automation completed");
        } else {
            LOGGER.info("Automation cancelled");
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
                int seconds = parseSeconds(event.getParameter());
                if (seconds > 0) {
                    Thread.sleep(seconds * 1000L);
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
        while (!cancelled.get()) {
            TapePlaybackController.CassetteState state = controller.getState();
            if (state != TapePlaybackController.CassetteState.PLAYING) {
                break;
            }
            Thread.sleep(100);
        }
    }

    private int parseSeconds(String param) {
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid delay value: '{}', using 0", param);
            return 0;
        }
    }

    private void notifyListener(int index) {
        IntConsumer listener = this.eventIndexListener;
        if (listener != null) {
            listener.accept(index);
        }
    }
}
