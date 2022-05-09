package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.memory.ram.api.RAMValue;

import java.util.Objects;

/**
 * The "Value" is a polymorphic value.
 * It has the type defined in compile time, but it can be integer or a String.
 */
public class Value implements RAMValue {
    private final int intValue;
    private final String stringValue;
    public final Type type;

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
