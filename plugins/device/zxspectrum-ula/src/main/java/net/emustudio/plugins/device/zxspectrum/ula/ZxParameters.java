/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula;

public class ZxParameters {
    public static final int SCREEN_WIDTH = 32; // in bytes; each byte represents 8 pixels in a row, reversed
    public static final int SCREEN_HEIGHT = 192;
    public static final int ATTRIBUTE_HEIGHT = SCREEN_HEIGHT / 8;

    public static final int PRE_SCREEN_LINES = 64;
    public static final int POST_SCREEN_LINES = 56;
    public static final int BORDER_WIDTH = 48; // pixels

    public static final int SCREEN_IMAGE_WIDTH = 2 * BORDER_WIDTH + SCREEN_WIDTH * 8;
    public static final int SCREEN_IMAGE_HEIGHT = PRE_SCREEN_LINES + SCREEN_HEIGHT + POST_SCREEN_LINES;

    // The Spectrum's 'FLASH' effect is also produced by the ULA: Every 16 frames, the ink and paper of all flashing
    // bytes is swapped; ie a normal to inverted to normal cycle takes 32 frames, which is (good as) 0.64 seconds.
    public static final int VIDEO_FLASH_FRAME = 15;
}
