/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

public class CompileException extends RuntimeException {
    final int line;
    final int column;

    public CompileException(int line, int column, String message) {
        super(message);
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return "line " + line + ":" + column + " " + super.getMessage();
    }
}
