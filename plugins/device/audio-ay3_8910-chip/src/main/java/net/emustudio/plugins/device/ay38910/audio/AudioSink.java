/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910.audio;

import net.emustudio.plugins.device.ay38910.Ay38910Chip;

/**
 * Minimal sink for the PCM stream produced by {@link Ay38910Chip}.
 */
public interface AudioSink extends AutoCloseable {
    AudioSink NULL = (pcmSamples, length) -> {
    };

    void accept(byte[] pcmSamples, int length);

    default void flushAudio() {
    }

    @Override
    default void close() {
    }
}
