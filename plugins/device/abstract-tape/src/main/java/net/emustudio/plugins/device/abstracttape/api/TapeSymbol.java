/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
