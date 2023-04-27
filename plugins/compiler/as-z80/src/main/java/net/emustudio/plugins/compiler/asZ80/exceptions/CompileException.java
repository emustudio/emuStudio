/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.asZ80.exceptions;

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
