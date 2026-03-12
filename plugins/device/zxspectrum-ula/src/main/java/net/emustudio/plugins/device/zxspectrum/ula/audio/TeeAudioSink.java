/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import java.util.Objects;

/**
 * Audio sink that always forwards to the live output sink and optionally mirrors the same PCM
 * frames into a secondary sink, such as a recorder.
 *
 * <p>{@link #flush()} intentionally targets only the primary sink. The beeper uses flush during
 * emulator reset to drop pending host playback latency; recorded audio must keep the already
 * emitted PCM history instead of being cleared.
 */
final class TeeAudioSink implements AudioSink {
    private final AudioSink primarySink;
    private volatile AudioSink secondarySink = AudioSink.NULL;

    TeeAudioSink(AudioSink primarySink) {
        this.primarySink = Objects.requireNonNull(primarySink);
    }

    void setSecondarySink(AudioSink secondarySink) {
        this.secondarySink = Objects.requireNonNull(secondarySink);
    }

    @Override
    public void write(byte[] samples, int length) {
        primarySink.write(samples, length);
        secondarySink.write(samples, length);
    }

    @Override
    public void flush() {
        primarySink.flush();
    }

    @Override
    public void close() {
        primarySink.close();
    }
}
