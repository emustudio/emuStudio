/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.exceptions;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;

import java.util.Objects;

public class CompileException extends RuntimeException {
    public final SourceCodePosition position;

    public CompileException(SourceCodePosition position, String message) {
        super(position + " " + message);
        this.position = Objects.requireNonNull(position);
    }

    public CompileException(SourceCodePosition position, String message, Throwable cause) {
        super(position + " " + message, cause);
        this.position = Objects.requireNonNull(position);
    }
}
