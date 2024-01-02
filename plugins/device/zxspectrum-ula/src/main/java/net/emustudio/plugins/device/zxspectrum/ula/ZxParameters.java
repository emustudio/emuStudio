/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2024 Peter Jakubčo
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
