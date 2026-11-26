/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotapeplayer;

import net.emustudio.emulib.io.FileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class TapePlaybackController implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapePlaybackController.class);

    public enum CassetteState {
        UNLOADED, PLAYING, STOPPED, CLOSED
    }

    private final TapePlayback listener;
    private CassetteState state = CassetteState.UNLOADED;
    private FileLoader.Format format;
    private Path path;
    private TapeTask playback;

    public TapePlaybackController(TapePlayback listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    public void reset() {
        stop(true).handle((ignored, failure) -> null).join();
    }

    @Override
    public void close() {
        shutdown().handle((ignored, failure) -> null).join();
    }

    synchronized CompletableFuture<Void> shutdown() {
        if (state != CassetteState.CLOSED) {
            state = CassetteState.CLOSED;
            if (!isPlaying()) {
                try {
                    listener.onStateChange(state);
                } catch (Throwable failure) {
                    return CompletableFuture.failedFuture(failure);
                }
            } else {
                playback.requestStop();
            }
        }
        return completion();
    }

    public synchronized void load(Path path) {
        if (isPlaying() || state == CassetteState.CLOSED) return;
        FileLoader.Format.fromPath(path).filter(FileLoader.Format::isTape).ifPresent(format -> {
            this.format = format;
            this.path = path;
            state = CassetteState.STOPPED;
            listener.onStateChange(state);
        });
    }

    public synchronized TapeTask play() {
        if (state == CassetteState.CLOSED) return null;
        if (isPlaying()) return playback;
        if (state != CassetteState.STOPPED) return null;

        TapeTask next = new TapeTask();
        Path tape = path;
        FileLoader.Format tapeFormat = format;
        listener.onStateChange(CassetteState.PLAYING);
        playback = next;
        state = CassetteState.PLAYING;
        next.start("tape-playback", () -> playTape(tape, tapeFormat, next));
        return next;
    }

    private void playTape(Path tape, FileLoader.Format tapeFormat, TapeTask playback) {
        Throwable failure = null;
        try {
            FileLoader.load(tape, tapeFormat, listener);
            playback.ready.complete(null);
            listener.awaitPlaybackEnd();
        } catch (InterruptedException | InterruptedIOException | ClosedByInterruptException stopped) {
            // Cancellation acknowledges only after the cleanup below.
        } catch (Throwable error) {
            failure = error;
        }
        finishPlayback(playback, failure);
    }

    private synchronized void finishPlayback(TapeTask playback, Throwable failure) {
        if (state != CassetteState.CLOSED) {
            state = path == null ? CassetteState.UNLOADED : CassetteState.STOPPED;
        }
        try {
            listener.onStateChange(state);
        } catch (Throwable cleanup) {
            if (failure == null) failure = cleanup;
            else if (failure != cleanup) failure.addSuppressed(cleanup);
        }
        if (failure != null) LOGGER.error("Could not play cassette", failure);
        playback.finish(failure);
    }

    public synchronized CompletableFuture<Void> stop(boolean unload) {
        if (state != CassetteState.CLOSED) {
            if (unload) {
                format = null;
                path = null;
            }
            if (isPlaying()) playback.requestStop();
            else if (unload && state != CassetteState.UNLOADED) {
                state = CassetteState.UNLOADED;
                listener.onStateChange(state);
            }
        }
        return completion();
    }

    private CompletableFuture<Void> completion() {
        return playback == null ? CompletableFuture.completedFuture(null) : playback.completion.copy();
    }

    private boolean isPlaying() {
        return playback != null && playback.isRunning();
    }

    public synchronized CassetteState getState() {
        return state;
    }
}
