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
package net.emustudio.plugins.memory.ram.api;

import java.io.Serializable;

/**
 * The "Value" is a polymorphic value.
 * It has the type defined in compile time, but it can be integer or a String.
 */
public interface RamValue extends Serializable {

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

    /**
     * Value type
     */
    enum Type {
        NUMBER, STRING, ID
    }
}
