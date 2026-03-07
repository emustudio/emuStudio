/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import org.junit.Test;

import java.awt.RenderingHints;

import static org.junit.Assert.assertEquals;

public class DisplayFontTest {

    @Test
    public void testOriginalFontUsesCrispRenderingHints() {
        assertEquals(RenderingHints.VALUE_TEXT_ANTIALIAS_ON, DisplayFont.FONT_ORIGINAL.textAntiAliasing);
        assertEquals(RenderingHints.VALUE_ANTIALIAS_ON, DisplayFont.FONT_ORIGINAL.antiAliasing);
        assertEquals(RenderingHints.VALUE_FRACTIONALMETRICS_OFF, DisplayFont.FONT_ORIGINAL.fractionalMetrics);
    }

    @Test
    public void testModernFontKeepsFractionalMetrics() {
        assertEquals(RenderingHints.VALUE_TEXT_ANTIALIAS_ON, DisplayFont.FONT_MODERN.textAntiAliasing);
        assertEquals(RenderingHints.VALUE_ANTIALIAS_ON, DisplayFont.FONT_MODERN.antiAliasing);
        assertEquals(RenderingHints.VALUE_FRACTIONALMETRICS_ON, DisplayFont.FONT_MODERN.fractionalMetrics);
    }
}
