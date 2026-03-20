/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import java.awt.*;

/**
 * Packed video attribute for a terminal cell.
 * <p>
 * Encoding (32-bit int):
 * <pre>
 *   bits 0-7:   foreground color index (0-15 for standard/bright ANSI, 16-255 for extended)
 *   bits 8-15:  background color index
 *   bit 16:     bold
 *   bit 17:     dim/faint
 *   bit 18:     italic
 *   bit 19:     underline
 *   bit 20:     blink
 *   bit 21:     inverse/reverse
 *   bit 22:     hidden/invisible
 *   bit 23:     strikethrough
 * </pre>
 * <p>
 * <a href="https://graphcomp.com/info/specs/ansi_col.html">ANSI Color Specification</a>
 */
public final class VideoAttribute {

    // Default attribute: white foreground (7), black background (0), no flags
    public static final int DEFAULT_FG = 7;
    public static final int DEFAULT_BG = 0;
    public static final int DEFAULT = pack(DEFAULT_FG, DEFAULT_BG, 0);

    // Bit masks and shifts
    private static final int FG_MASK = 0xFF;
    private static final int BG_SHIFT = 8;
    private static final int BG_MASK = 0xFF00;
    private static final int FLAG_BOLD = 1 << 16;
    private static final int FLAG_DIM = 1 << 17;
    private static final int FLAG_ITALIC = 1 << 18;
    private static final int FLAG_UNDERLINE = 1 << 19;
    private static final int FLAG_BLINK = 1 << 20;
    private static final int FLAG_INVERSE = 1 << 21;
    private static final int FLAG_HIDDEN = 1 << 22;
    private static final int FLAG_STRIKETHROUGH = 1 << 23;
    private static final int ALL_FLAGS = FLAG_BOLD | FLAG_DIM | FLAG_ITALIC | FLAG_UNDERLINE | FLAG_BLINK | FLAG_INVERSE | FLAG_HIDDEN | FLAG_STRIKETHROUGH;

    /**
     * Standard ANSI color palette (16 colors: 8 normal + 8 bright).
     */
    public static final Color[] COLOR_TABLE = {
            // Standard colors (0-7)
            new Color(0, 0, 0),         // 0: Black
            new Color(170, 0, 0),       // 1: Red
            new Color(0, 170, 0),       // 2: Green
            new Color(170, 170, 0),     // 3: Yellow
            new Color(0, 0, 170),       // 4: Blue
            new Color(170, 0, 170),     // 5: Magenta
            new Color(0, 170, 170),     // 6: Cyan
            new Color(170, 170, 170),   // 7: White (light gray)

            // Bright colors (8-15)
            new Color(85, 85, 85),      // 8: Bright Black (Dark Gray)
            new Color(255, 85, 85),     // 9: Bright Red
            new Color(85, 255, 85),     // 10: Bright Green
            new Color(255, 255, 85),    // 11: Bright Yellow
            new Color(85, 85, 255),     // 12: Bright Blue
            new Color(255, 85, 255),    // 13: Bright Magenta
            new Color(85, 255, 255),    // 14: Bright Cyan
            new Color(255, 255, 255),   // 15: Bright White
    };

    private VideoAttribute() {
    }

    public static int pack(int fg, int bg, int flags) {
        return (fg & 0xFF) | ((bg & 0xFF) << BG_SHIFT) | (flags & ALL_FLAGS);
    }

    public static int getFg(int attr) {
        return attr & FG_MASK;
    }

    public static int getBg(int attr) {
        return (attr & BG_MASK) >> BG_SHIFT;
    }

    public static boolean isBold(int attr) {
        return (attr & FLAG_BOLD) != 0;
    }

    public static boolean isDim(int attr) {
        return (attr & FLAG_DIM) != 0;
    }

    public static boolean isItalic(int attr) {
        return (attr & FLAG_ITALIC) != 0;
    }

    public static boolean isUnderline(int attr) {
        return (attr & FLAG_UNDERLINE) != 0;
    }

    public static boolean isBlink(int attr) {
        return (attr & FLAG_BLINK) != 0;
    }

    public static boolean isInverse(int attr) {
        return (attr & FLAG_INVERSE) != 0;
    }

    public static boolean isHidden(int attr) {
        return (attr & FLAG_HIDDEN) != 0;
    }

    public static boolean isStrikethrough(int attr) {
        return (attr & FLAG_STRIKETHROUGH) != 0;
    }

    public static int withFg(int attr, int fg) {
        return (attr & ~FG_MASK) | (fg & 0xFF);
    }

    public static int withBg(int attr, int bg) {
        return (attr & ~BG_MASK) | ((bg & 0xFF) << BG_SHIFT);
    }

    public static int withBold(int attr, boolean bold) {
        return bold ? (attr | FLAG_BOLD) : (attr & ~FLAG_BOLD);
    }

    public static int withDim(int attr, boolean dim) {
        return dim ? (attr | FLAG_DIM) : (attr & ~FLAG_DIM);
    }

    public static int withItalic(int attr, boolean italic) {
        return italic ? (attr | FLAG_ITALIC) : (attr & ~FLAG_ITALIC);
    }

    public static int withUnderline(int attr, boolean underline) {
        return underline ? (attr | FLAG_UNDERLINE) : (attr & ~FLAG_UNDERLINE);
    }

    public static int withBlink(int attr, boolean blink) {
        return blink ? (attr | FLAG_BLINK) : (attr & ~FLAG_BLINK);
    }

    public static int withInverse(int attr, boolean inverse) {
        return inverse ? (attr | FLAG_INVERSE) : (attr & ~FLAG_INVERSE);
    }

    public static int withHidden(int attr, boolean hidden) {
        return hidden ? (attr | FLAG_HIDDEN) : (attr & ~FLAG_HIDDEN);
    }

    public static int withStrikethrough(int attr, boolean strikethrough) {
        return strikethrough ? (attr | FLAG_STRIKETHROUGH) : (attr & ~FLAG_STRIKETHROUGH);
    }

    /**
     * Resolve the effective foreground Color for rendering.
     * Handles inverse mode and bold-to-bright color mapping.
     */
    public static Color resolveFgColor(int attr) {
        int fgIndex = isInverse(attr) ? getBg(attr) : getFg(attr);
        // Bold maps standard colors (0-7) to bright (8-15)
        if (isBold(attr) && fgIndex < 8) {
            fgIndex += 8;
        }
        if (fgIndex < 0 || fgIndex >= COLOR_TABLE.length) {
            fgIndex = DEFAULT_FG;
        }
        Color c = COLOR_TABLE[fgIndex];
        if (isDim(attr)) {
            return new Color(c.getRed() / 2, c.getGreen() / 2, c.getBlue() / 2);
        }
        return c;
    }

    /**
     * Resolve the effective background Color for rendering.
     * Handles inverse mode.
     */
    public static Color resolveBgColor(int attr) {
        int bgIndex = isInverse(attr) ? getFg(attr) : getBg(attr);
        if (bgIndex < 0 || bgIndex >= COLOR_TABLE.length) {
            bgIndex = DEFAULT_BG;
        }
        return COLOR_TABLE[bgIndex];
    }
}

