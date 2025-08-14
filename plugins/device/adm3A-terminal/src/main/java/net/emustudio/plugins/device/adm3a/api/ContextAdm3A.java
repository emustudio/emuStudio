/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.api;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.adm3a.TerminalSettings;
import net.jcip.annotations.ThreadSafe;

import java.util.Objects;
import java.util.function.Supplier;

@ThreadSafe
public class ContextAdm3A implements DeviceContext<Byte> {
    private final Supplier<Boolean> isHalfDuplex;

    private volatile DeviceContext<Byte> externalDevice;
    private volatile Display display = Display.DUMMY; // is never null; and this context is not an owner of display

    public ContextAdm3A(Supplier<Boolean> isHalfDuplex) {
        this.isHalfDuplex = Objects.requireNonNull(isHalfDuplex);
    }

    public void setExternalDevice(DeviceContext<Byte> externalDevice) {
        this.externalDevice = Objects.requireNonNull(externalDevice);
    }

    public void setDisplay(Display display) {
        this.display = Objects.requireNonNull(display);
    }

    public void reset() {
        display.reset();
    }

    @Override
    public Byte readData() {
        // All data is immediately sent to externalDevice. We're not buffering anything.
        return 0;
    }

    @Override
    public void writeData(Byte data) {
        display.write(data);
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    public void onKeyFromKeyboard(byte key) {
        DeviceContext<Byte> device = externalDevice;
        if (device != null) {
            device.writeData(key);
        }
        if (isHalfDuplex.get()) {
            writeData(key);
        }
    }

    @Override
    public String toString() {
        return "LSI ADM-3A terminal";
    }
}
