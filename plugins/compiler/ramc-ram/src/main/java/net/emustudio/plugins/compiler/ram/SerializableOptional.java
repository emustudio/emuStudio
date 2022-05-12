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
package net.emustudio.plugins.compiler.ram;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class SerializableOptional<T> implements Serializable {
    public final T value;

    public SerializableOptional(T value) {
        this.value = value;
    }

    public Optional<T> opt() {
        return Optional.ofNullable(value);
    }

    public static <T> SerializableOptional<T> empty() {
        return new SerializableOptional<>(null);
    }

    public static <T> SerializableOptional<T> ofNullable(T value) {
        return new SerializableOptional<>(value);
    }

    public static <T> SerializableOptional<T> fromOpt(Optional<T> valueOpt) {
        return new SerializableOptional<>(valueOpt.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SerializableOptional<?> that = (SerializableOptional<?>) o;

        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
