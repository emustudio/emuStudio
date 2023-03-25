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
