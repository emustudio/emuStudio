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
package net.emustudio.plugins.device.brainduck.terminal.api;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.jcip.annotations.ThreadSafe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@ThreadSafe
public class BrainTerminalContext implements DeviceContext<Byte>, AutoCloseable {
    public static final File OUTPUT_FILE_NAME = new File("brainduck-terminal.out");
    public static final File INPUT_FILE_NAME = new File("brainduck-terminal.in");

    private final Keyboard keyboard;
    private final Queue<Byte> inputBuffer = new ConcurrentLinkedQueue<>();
    private volatile OutputProvider display = OutputProvider.DUMMY;

    public BrainTerminalContext(Keyboard keyboard) {
        this.keyboard = Objects.requireNonNull(keyboard);
        keyboard.addOnKeyHandler(this::onKeyFromKeyboard);
    }

    public void setDisplay(OutputProvider display) {
        this.display = display;
    }

    public void reset() {
        OutputProvider tmp = display;
        if (tmp != null) {
            tmp.reset();
        }
    }

    @Override
    public Byte readData() {
        return inputBuffer.poll();
    }

    @Override
    public void writeData(Byte data) {
        OutputProvider tmpOutputProvider = display;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.write(data);
        }
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    @Override
    public void close() throws Exception {
        Keyboard tmpKeyboard = keyboard;
        if (tmpKeyboard != null) {
            tmpKeyboard.close();
        }
        OutputProvider tmpOutputProvider = display;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.close();
        }
    }

    private void onKeyFromKeyboard(byte key) {
        inputBuffer.add(key);
    }
}
