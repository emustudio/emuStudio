/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.emulib.runtime.interaction.GuiUtils;

import java.awt.*;

public class GuiUtilsAdm3A {

    public static Font loadFont(DisplayFont displayFont) {
        return GuiUtils.loadFontResource(displayFont.path, GuiUtilsAdm3A.class, displayFont.fontSize);
    }
}
