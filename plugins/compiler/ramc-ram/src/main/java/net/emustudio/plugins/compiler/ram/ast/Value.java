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
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.memory.ram.api.RAMValue;

import java.util.Objects;

/**
 * The "Value" is a polymorphic value.
 * It has the type defined in compile time, but it can be integer or a String.
 */
public class Value implements RAMValue {
    public final Type type;
    private final int intValue;
    private final String stringValue;

    public Value(int value) {
        this.intValue = value;
        this.stringValue = null;
        this.type = Type.NUMBER;
    }

    public Value(String value, boolean isID) {
        this.stringValue = Objects.requireNonNull(value);
        this.intValue = 0;
        this.type = isID ? Type.ID : Type.STRING;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public int getNumberValue() {
        return intValue;
    }

    @Override
    public String getStringValue() {
        return stringValue;
    }

    @Override
    public String getStringRepresentation() {
        return (type == Type.NUMBER) ? String.valueOf(intValue) : stringValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Value value = (Value) o;

        if (intValue != value.intValue) return false;
        if (!Objects.equals(stringValue, value.stringValue)) return false;
        return type == value.type;
    }

    @Override
    public int hashCode() {
        int result = intValue;
        result = 31 * result + (stringValue != null ? stringValue.hashCode() : 0);
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return type == Type.NUMBER ? String.valueOf(intValue) : stringValue;
    }
}
