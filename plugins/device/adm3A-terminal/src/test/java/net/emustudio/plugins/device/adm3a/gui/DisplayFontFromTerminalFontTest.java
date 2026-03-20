/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.plugins.device.adm3a.TerminalSettings;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class DisplayFontFromTerminalFontTest {

    @Test
    public void testFromTerminalFontOriginal() {
        assertSame(DisplayFont.FONT_ORIGINAL, DisplayFont.fromTerminalFont(TerminalSettings.TerminalFont.ORIGINAL));
    }

    @Test
    public void testFromTerminalFontModern() {
        assertSame(DisplayFont.FONT_MODERN, DisplayFont.fromTerminalFont(TerminalSettings.TerminalFont.MODERN));
    }
}

