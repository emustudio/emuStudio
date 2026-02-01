/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;

import java.util.Objects;

public class CompileException extends RuntimeException {
    public final SourceCodePosition position;

    public CompileException(SourceCodePosition position, String message) {
        super(message);
        this.position = Objects.requireNonNull(position);
    }

    @Override
    public String toString() {
        return position + " " + super.getMessage();
    }
}
