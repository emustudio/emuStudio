/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import java.awt.*;

import static net.emustudio.emulib.runtime.ui.GUI.loadFontResource;

public class GuiUtilsAdm3A {

    public static Font loadFont(DisplayFont displayFont) {
        return loadFontResource(displayFont.path, GuiUtilsAdm3A.class, displayFont.fontSize);
    }
}
