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
