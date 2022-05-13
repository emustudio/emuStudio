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
package net.emustudio.plugins.compiler.ssem;

import org.antlr.v4.runtime.Token;

import java.util.function.Function;

public class CompilerChecks {

    public static void checkStartLineDefined(boolean defined, Position pos, int startLine) {
        if (defined) {
            throw new CompileException(pos.line, pos.column, "Start line is already defined (at line " + startLine + ")!");
        }
    }

    public static void checkLineOutOfBounds(Position pos, int line) {
        if (line < 0 || line > 31) {
            throw new CompileException(pos.line, pos.column, "Line number is out of bounds <0;31>: " + line);
        }
    }

    public static void checkDuplicateLineDefinition(boolean duplicate, Position pos, int line) {
        if (duplicate) {
            throw new CompileException(pos.line, pos.column, "Duplicate line definition: " + line);
        }
    }

    public static void checkUnknownInstruction(boolean unknown, Position pos) {
        if (unknown) {
            throw new CompileException(pos.line, pos.column, "Unrecognized instruction");
        }
    }

    public static void checkOperandOutOfBounds(Position pos, int tokenType, long operand) {
        if (tokenType != SSEMLexer.BNUM && tokenType != SSEMLexer.NUM && (operand < 0 || operand > 31)) {
            throw new CompileException(
                pos.line, pos.column, "Operand must be between <0, 31>; it was " + operand
            );
        }
    }

    public static <T extends Number> T checkedParseNumber(Token token, Function<Token, T> parser) {
        try {
            return parser.apply(token);
        } catch (NumberFormatException e) {
            throw new CompileException(
                token.getLine(), token.getCharPositionInLine(), "Could not parse number: " + token.getText()
            );
        }
    }
}
