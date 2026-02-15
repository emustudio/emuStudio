/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import java.awt.*;

import net.emustudio.emulib.runtime.ui.GUI;

public class GuiUtilsAdm3A {

    public static Font loadFont(DisplayFont displayFont) {
        return GUI.loadFont(displayFont.path, GuiUtilsAdm3A.class, displayFont.fontSize);
    }
}
