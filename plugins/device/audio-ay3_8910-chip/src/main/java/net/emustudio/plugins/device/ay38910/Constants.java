/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910;

public final class Constants {
    public static final String THREAD_NAME_PREFIX = "emustudio-ay38910-";
    public static final long QUEUE_POLL_TIMEOUT_MS = 10;
    public static final int AUDIO_DEFAULT_BATCH_FRAMES = 512;

    private Constants() {
    }
}
