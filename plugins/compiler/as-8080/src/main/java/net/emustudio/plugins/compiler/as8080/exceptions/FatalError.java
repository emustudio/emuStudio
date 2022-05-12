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
package net.emustudio.plugins.compiler.as8080.exceptions;

import net.emustudio.plugins.compiler.as8080.CompileError;

public class FatalError extends CompileException {

    public FatalError(int line, int column, String why) {
        super(line, column, "Fatal error (cannot continue): " + why);
    }

    public static void now(int line, int column, String why) {
        throw new FatalError(line, column, why);
    }

    public static void now(CompileError error) {
        now(error.line, error.column, error.msg);
    }
}
