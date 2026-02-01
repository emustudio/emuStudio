/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.api;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class Keyboard implements AutoCloseable {
    private final List<Consumer<Byte>> onKeyHandlers = new CopyOnWriteArrayList<>();
    private final List<Consumer<Boolean>> onInputRequestHandlers = new CopyOnWriteArrayList<>();

    /**
     * Automatic processing of input if this keyboard is capable of doing so.
     */
    public abstract void process();

    public void addOnKeyHandler(Consumer<Byte> onKeyHandler) {
        onKeyHandlers.add(Objects.requireNonNull(onKeyHandler));
    }

    public void addInputRequestHandler(Consumer<Boolean> onInputRequest) {
        onInputRequestHandlers.add(onInputRequest);
    }

    public void inputRequested(boolean reuquested) {
        onInputRequestHandlers.forEach(c -> c.accept(reuquested));
    }

    protected void notifyOnKey(byte key) {
        onKeyHandlers.forEach(c -> c.accept(key));
    }

    public void close() {
        onKeyHandlers.clear();
    }
}
