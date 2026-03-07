/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

public class TerminalWindowTest {

    @Test
    public void testComputeCanvasBoundsKeepsOriginalLayoutForNinePixelCells() {
        Rectangle bounds = TerminalWindow.computeCanvasBounds(DisplayFont.FONT_ORIGINAL, 80, 9);
        assertEquals(new Rectangle(150, 170, 725, 530), bounds);
    }

    @Test
    public void testComputeCanvasBoundsExpandsCanvasForTenPixelCells() {
        Rectangle bounds = TerminalWindow.computeCanvasBounds(DisplayFont.FONT_ORIGINAL, 80, 10);
        assertEquals(new Rectangle(110, 170, 805, 530), bounds);
    }
}
