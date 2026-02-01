/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import org.antlr.v4.runtime.Token;

public class Position {

    public static SourceCodePosition of(String fileName, Token token) {
        return new SourceCodePosition(token.getLine(), token.getCharPositionInLine(), fileName);
    }
}
