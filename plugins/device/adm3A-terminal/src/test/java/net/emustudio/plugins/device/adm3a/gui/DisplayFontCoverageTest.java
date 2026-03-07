/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.emulib.runtime.ui.GUI;
import org.junit.Test;

import java.awt.Font;

import static org.junit.Assert.assertEquals;

public class DisplayFontCoverageTest {

    @Test
    public void testOriginalFontSupportsRepresentativeLatin1Characters() {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path,
                DisplayFontCoverageTest.class,
                DisplayFont.FONT_ORIGINAL.fontSize
        );

        String sample = "ÀÄÇÑØßàäçñøÿ£©±¿";
        assertEquals(-1, font.canDisplayUpTo(sample));
    }
}
