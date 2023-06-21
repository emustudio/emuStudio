/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.audiotape_player;

import net.emustudio.plugins.device.audiotape_player.loaders.Loader;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

@ThreadSafe
public class TapePlaybackController implements AutoCloseable {
    private final static Logger LOGGER = LoggerFactory.getLogger(TapePlaybackController.class);

    public enum CassetteState {
        UNLOADED,
        PLAYING,
        STOPPED,
        CLOSED // terminal state
    }

    private final Loader.TapePlayback listener;
    private final ExecutorService playPool = Executors.newFixedThreadPool(1);

    private final Object stateLock = new Object();
    @GuardedBy("stateLock")
    private CassetteState state = CassetteState.UNLOADED;
    @GuardedBy("stateLock")
    private Loader loader;
    @GuardedBy("stateLock")
    private Future<?> playFuture;

    private final Queue<CassetteState> stateNotifications = new ConcurrentLinkedQueue<>();

    public TapePlaybackController(Loader.TapePlayback listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    public void reset() {
        stop(true);
    }

    @Override
    public void close() {
        synchronized (stateLock) {
            this.state = CassetteState.CLOSED;
            Future<?> tmpFuture = this.playFuture;
            this.playFuture = null;
            if (tmpFuture != null) {
                tmpFuture.cancel(true);
            }
            playPool.shutdown();
            stateNotifications.add(this.state);
        }
        notifyStateChange();
        try {
            if (!playPool.awaitTermination(5, TimeUnit.SECONDS)) {
                playPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void load(Path path) {
        Loader.create(path).ifPresent(tmpLoader -> {
            synchronized (stateLock) {
                switch (state) {
                    case UNLOADED:
                    case STOPPED:
                        this.loader = tmpLoader;
                        this.state = CassetteState.STOPPED;
                }
                stateNotifications.add(this.state);
            }
        });
        notifyStateChange();
    }

    public void play() {
        synchronized (stateLock) {
            if (this.state == CassetteState.STOPPED) {
                Loader tmpLoader = this.loader;
                if (tmpLoader != null) {
                    this.state = CassetteState.PLAYING;
                    this.playFuture = playPool.submit(() -> {
                        try {
                            tmpLoader.load(listener);
                            synchronized (stateLock) {
                                this.state = CassetteState.STOPPED;
                                stateNotifications.add(this.state);
                            }
                            notifyStateChange();
                        } catch (IOException e) {
                            LOGGER.error("Could not load cassette", e);
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
            stateNotifications.add(this.state);
        }
        notifyStateChange();
    }

    public void stop(boolean unload) {
        synchronized (stateLock) {
            if (this.state == CassetteState.PLAYING) {
                Future<?> tmpFuture = this.playFuture;
                this.playFuture = null;
                if (tmpFuture != null) {
                    tmpFuture.cancel(true);
                }
                this.state = CassetteState.STOPPED;
            }
            if (unload && this.state == CassetteState.STOPPED) {
                this.loader = null;
                this.state = CassetteState.UNLOADED;
            }
            stateNotifications.add(this.state);
        }
        notifyStateChange();
    }

    public CassetteState getState() {
        synchronized (stateLock) {
            return this.state;
        }
    }

    private void notifyStateChange() {
        Optional.ofNullable(stateNotifications.poll()).ifPresent(listener::onStateChange);
    }
}
