/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

public class CompileException extends RuntimeException {
    public final int line;
    public final int column;

    public CompileException(int line, int column, String message) {
        super("[" + line + "," + column + "] " + message);

        this.column = column;
        this.line = line;
    }

    public CompileException(int line, int column, String message, Throwable cause) {
        super("[" + line + "," + column + "] " + message, cause);

        this.column = column;
        this.line = line;
    }
}
