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
package net.emustudio.plugins.device.abstracttape.api;

import net.jcip.annotations.Immutable;

import java.util.Objects;

import static net.emustudio.plugins.device.abstracttape.api.TapeSymbol.Type.NUMBER;
import static net.emustudio.plugins.device.abstracttape.api.TapeSymbol.Type.STRING;

@SuppressWarnings("unused")
@Immutable
public class TapeSymbol {
    public final static TapeSymbol EMPTY = new TapeSymbol("");

    public final int number;
    public final String string;
    public final Type type;

    public TapeSymbol(String string) {
        this.string = Objects.requireNonNullElse(string, "");
        this.number = 0;
        this.type = STRING;
    }

    public TapeSymbol(int number) {
        this.number = number;
        this.string = null;
        this.type = NUMBER;
    }

    public static TapeSymbol guess(String s) {
        try {
            int value = Integer.decode(s);
            return new TapeSymbol(value);
        } catch (NumberFormatException e) {
            return new TapeSymbol(s);
        }
    }

    public static TapeSymbol fromInt(int number) {
        return new TapeSymbol(number);
    }

    public static TapeSymbol fromString(String s) {
        return new TapeSymbol(s);
    }

    @Override
    public String toString() {
        return (type == NUMBER) ? String.valueOf(number) : string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TapeSymbol that = (TapeSymbol) o;
        return number == that.number && Objects.equals(string, that.string) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, string, type);
    }

    public enum Type {
        NUMBER, STRING
    }
}
