package net.emustudio.plugins.memory.ram.api;

import java.io.Serializable;

/**
 * The "Value" is a polymorphic value.
 * It has the type defined in compile time, but it can be integer or a String.
 */
public interface RAMValue extends Serializable {

    /**
     * Value type
     */
    enum Type {
        NUMBER, STRING, ID
    }

    /**
     * Whether this value is an integer number
     *
     * @return true if the value is a number
     */
    Type getType();

    /**
     * Get integer interpretation of this value
     *
     * @return integer interpretation of this value
     * @throws RuntimeException if the value is not a number
     */
    int getNumberValue();

    /**
     * Get String interpretation of this value
     *
     * @return string interpretation of this value
     */
    String getStringValue();

    /**
     * Get String representation of this value.
     * It might be useful for displaying the value regardless of type.
     *
     * @return string representation of this value
     */
    String getStringRepresentation();
}
