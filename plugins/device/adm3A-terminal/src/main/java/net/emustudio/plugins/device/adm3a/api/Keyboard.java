package net.emustudio.plugins.device.adm3a.api;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class Keyboard implements AutoCloseable {
    protected List<Consumer<Byte>> onKeyHandlers = new CopyOnWriteArrayList<>();

    public abstract void process();

    public void addOnKeyHandler(Consumer<Byte> onKeyHandler) {
        onKeyHandlers.add(Objects.requireNonNull(onKeyHandler));
    }

    protected void notifyOnKey(byte key) {
        onKeyHandlers.forEach(c -> c.accept(key));
    }

    public void close() {
        onKeyHandlers.clear();
    }
}
