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
package net.emustudio.plugins.device.vt100.api;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.jcip.annotations.ThreadSafe;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * VT100 device context.
 * <p>
 * Enables sending data to a connected device, but if a device is not available, buffers the data and makes it
 * available by reading.
 */
@ThreadSafe
public class ContextVt100 implements DeviceContext<Byte> {
    private final Keyboard keyboard;
    private final BlockingQueue<Byte> inputBuffer = new LinkedBlockingDeque<>();
    private final AtomicReference<DeviceContext<Byte>> externalDevice = new AtomicReference<>();
    private volatile Display display = Display.DUMMY;

    public ContextVt100(Keyboard keyboard) {
        this.keyboard = Objects.requireNonNull(keyboard);
        keyboard.addOnKeyHandler(this::onKeyFromKeyboard);
    }

    public void setDisplay(Display display) {
        this.display = Objects.requireNonNull(display);
    }

    public void setExternalDevice(DeviceContext<Byte> device) {
        this.externalDevice.set(device);
    }

    public void reset() {
        display.reset();
    }

    @Override
    public Byte readData() {
        try {
            if (inputBuffer.isEmpty()) {
                keyboard.inputRequested(true);
            }
            return inputBuffer.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } finally {
            keyboard.inputRequested(false);
        }
    }

    @Override
    public void writeData(Byte data) {
        display.write(data);
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    private void onKeyFromKeyboard(byte key) {
        DeviceContext<Byte> device = externalDevice.get();
        if (device == null) {
            inputBuffer.add(key);
        } else {
            device.writeData(key);
        }
    }
}
