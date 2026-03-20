/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class VideoAttributeTest {

    @Test
    public void testDefaultAttribute() {
        assertEquals(VideoAttribute.DEFAULT_FG, VideoAttribute.getFg(VideoAttribute.DEFAULT));
        assertEquals(VideoAttribute.DEFAULT_BG, VideoAttribute.getBg(VideoAttribute.DEFAULT));
        assertFalse(VideoAttribute.isBold(VideoAttribute.DEFAULT));
        assertFalse(VideoAttribute.isDim(VideoAttribute.DEFAULT));
        assertFalse(VideoAttribute.isItalic(VideoAttribute.DEFAULT));
        assertFalse(VideoAttribute.isUnderline(VideoAttribute.DEFAULT));
        assertFalse(VideoAttribute.isBlink(VideoAttribute.DEFAULT));
        assertFalse(VideoAttribute.isInverse(VideoAttribute.DEFAULT));
        assertFalse(VideoAttribute.isHidden(VideoAttribute.DEFAULT));
        assertFalse(VideoAttribute.isStrikethrough(VideoAttribute.DEFAULT));
    }

    @Test
    public void testPackAndGetFg() {
        int attr = VideoAttribute.pack(5, 0, 0);
        assertEquals(5, VideoAttribute.getFg(attr));
    }

    @Test
    public void testPackAndGetBg() {
        int attr = VideoAttribute.pack(0, 3, 0);
        assertEquals(3, VideoAttribute.getBg(attr));
    }

    @Test
    public void testWithFg() {
        int attr = VideoAttribute.withFg(VideoAttribute.DEFAULT, 1);
        assertEquals(1, VideoAttribute.getFg(attr));
        assertEquals(VideoAttribute.DEFAULT_BG, VideoAttribute.getBg(attr));
    }

    @Test
    public void testWithBg() {
        int attr = VideoAttribute.withBg(VideoAttribute.DEFAULT, 4);
        assertEquals(4, VideoAttribute.getBg(attr));
        assertEquals(VideoAttribute.DEFAULT_FG, VideoAttribute.getFg(attr));
    }

    @Test
    public void testWithBold() {
        int attr = VideoAttribute.withBold(VideoAttribute.DEFAULT, true);
        assertTrue(VideoAttribute.isBold(attr));
        attr = VideoAttribute.withBold(attr, false);
        assertFalse(VideoAttribute.isBold(attr));
    }

    @Test
    public void testWithDim() {
        int attr = VideoAttribute.withDim(VideoAttribute.DEFAULT, true);
        assertTrue(VideoAttribute.isDim(attr));
    }

    @Test
    public void testWithItalic() {
        int attr = VideoAttribute.withItalic(VideoAttribute.DEFAULT, true);
        assertTrue(VideoAttribute.isItalic(attr));
    }

    @Test
    public void testWithUnderline() {
        int attr = VideoAttribute.withUnderline(VideoAttribute.DEFAULT, true);
        assertTrue(VideoAttribute.isUnderline(attr));
    }

    @Test
    public void testWithBlink() {
        int attr = VideoAttribute.withBlink(VideoAttribute.DEFAULT, true);
        assertTrue(VideoAttribute.isBlink(attr));
    }

    @Test
    public void testWithInverse() {
        int attr = VideoAttribute.withInverse(VideoAttribute.DEFAULT, true);
        assertTrue(VideoAttribute.isInverse(attr));
    }

    @Test
    public void testWithHidden() {
        int attr = VideoAttribute.withHidden(VideoAttribute.DEFAULT, true);
        assertTrue(VideoAttribute.isHidden(attr));
    }

    @Test
    public void testWithStrikethrough() {
        int attr = VideoAttribute.withStrikethrough(VideoAttribute.DEFAULT, true);
        assertTrue(VideoAttribute.isStrikethrough(attr));
    }

    @Test
    public void testMultipleAttributes() {
        int attr = VideoAttribute.DEFAULT;
        attr = VideoAttribute.withFg(attr, 1);   // red
        attr = VideoAttribute.withBg(attr, 4);   // blue bg
        attr = VideoAttribute.withBold(attr, true);
        attr = VideoAttribute.withUnderline(attr, true);

        assertEquals(1, VideoAttribute.getFg(attr));
        assertEquals(4, VideoAttribute.getBg(attr));
        assertTrue(VideoAttribute.isBold(attr));
        assertTrue(VideoAttribute.isUnderline(attr));
        assertFalse(VideoAttribute.isItalic(attr));
    }

    @Test
    public void testResolveFgColorDefault() {
        Color fg = VideoAttribute.resolveFgColor(VideoAttribute.DEFAULT);
        assertEquals(VideoAttribute.COLOR_TABLE[VideoAttribute.DEFAULT_FG], fg);
    }

    @Test
    public void testResolveBgColorDefault() {
        Color bg = VideoAttribute.resolveBgColor(VideoAttribute.DEFAULT);
        assertEquals(VideoAttribute.COLOR_TABLE[VideoAttribute.DEFAULT_BG], bg);
    }

    @Test
    public void testResolveFgColorRed() {
        int attr = VideoAttribute.withFg(VideoAttribute.DEFAULT, 1);
        Color fg = VideoAttribute.resolveFgColor(attr);
        assertEquals(VideoAttribute.COLOR_TABLE[1], fg);
    }

    @Test
    public void testResolveFgColorBoldMappsToBright() {
        int attr = VideoAttribute.withFg(VideoAttribute.DEFAULT, 1); // red
        attr = VideoAttribute.withBold(attr, true);
        Color fg = VideoAttribute.resolveFgColor(attr);
        assertEquals(VideoAttribute.COLOR_TABLE[9], fg); // bright red
    }

    @Test
    public void testResolveFgColorBoldDoesNotMapBrightColors() {
        int attr = VideoAttribute.withFg(VideoAttribute.DEFAULT, 9); // bright red (already bright)
        attr = VideoAttribute.withBold(attr, true);
        Color fg = VideoAttribute.resolveFgColor(attr);
        assertEquals(VideoAttribute.COLOR_TABLE[9], fg); // stays bright red
    }

    @Test
    public void testResolveInverse() {
        int attr = VideoAttribute.withFg(VideoAttribute.DEFAULT, 1); // red fg
        attr = VideoAttribute.withBg(attr, 4);                       // blue bg
        attr = VideoAttribute.withInverse(attr, true);
        Color fg = VideoAttribute.resolveFgColor(attr);   // should be blue (from bg)
        Color bg = VideoAttribute.resolveBgColor(attr);   // should be red (from fg)
        assertEquals(VideoAttribute.COLOR_TABLE[4], fg);
        assertEquals(VideoAttribute.COLOR_TABLE[1], bg);
    }

    @Test
    public void testResolveDim() {
        int attr = VideoAttribute.withFg(VideoAttribute.DEFAULT, 7); // white
        attr = VideoAttribute.withDim(attr, true);
        Color fg = VideoAttribute.resolveFgColor(attr);
        Color normalWhite = VideoAttribute.COLOR_TABLE[7];
        assertEquals(normalWhite.getRed() / 2, fg.getRed());
        assertEquals(normalWhite.getGreen() / 2, fg.getGreen());
        assertEquals(normalWhite.getBlue() / 2, fg.getBlue());
    }

    @Test
    public void testColorTableHas16Entries() {
        assertEquals(16, VideoAttribute.COLOR_TABLE.length);
    }

    @Test
    public void testColorTableBlackIsBlack() {
        assertEquals(new Color(0, 0, 0), VideoAttribute.COLOR_TABLE[0]);
    }

    @Test
    public void testColorTableBrightWhite() {
        assertEquals(new Color(255, 255, 255), VideoAttribute.COLOR_TABLE[15]);
    }
}

