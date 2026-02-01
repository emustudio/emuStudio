/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;


import java.awt.*;

import static net.emustudio.emulib.runtime.ui.GUI.loadFontResource;

public class Constants {

    public static final Font FONT_CODE = loadFontResource(
            "/net/emustudio/application/gui/FiraCode-Regular.ttf", Constants.class, 13);

    public static final Color DEBUGTABLE_COLOR_CURRENT_INSTRUCTION = Color.RED;

    public static final Color TOKEN_COMMENT = new Color(0, 128, 0);
    public static final Color TOKEN_RESERVED = Color.BLACK;
    public static final Color TOKEN_IDENTIFIER = Color.BLACK;
    public static final Color TOKEN_LITERAL = new Color(0, 0, 128);
    public static final Color TOKEN_LABEL = new Color(0, 128, 128);
    public static final Color TOKEN_REGISTER = new Color(128, 0, 0);
    public static final Color TOKEN_PREPROCESSOR = new Color(80, 80, 80);
    public static final Color TOKEN_SEPARATOR = Color.BLACK;
    public static final Color TOKEN_OPERATOR = new Color(0, 0, 128);
    public static final Color TOKEN_ERROR = Color.RED;
}
