/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
