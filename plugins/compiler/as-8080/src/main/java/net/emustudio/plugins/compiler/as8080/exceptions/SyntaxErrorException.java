/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.exceptions;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;

public class SyntaxErrorException extends CompileException {
    public SyntaxErrorException(SourceCodePosition position, String message) {
        super(position, message);
    }

    public SyntaxErrorException(SourceCodePosition position, String message, Throwable cause) {
        super(position, message, cause);
    }
}
