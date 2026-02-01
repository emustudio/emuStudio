/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram.exceptions;

public class SyntaxErrorException extends CompileException {
    public SyntaxErrorException(int line, int column, String message) {
        super(line, column, message);
    }

    public SyntaxErrorException(int line, int column, String message, Throwable cause) {
        super(line, column, message, cause);
    }
}
