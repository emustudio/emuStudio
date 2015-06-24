package net.sf.emustudio.intel8080.impl.suite;

@FunctionalInterface
public interface BinaryConsumer<T> {
    void accept(T first, T second);
}
