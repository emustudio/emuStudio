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

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.adm3a.TerminalSettings;
import net.jcip.annotations.ThreadSafe;

import java.util.Objects;

@ThreadSafe
public class ContextAdm3A implements DeviceContext<Byte>, AutoCloseable {
    private final Keyboard keyboard;
    private final TerminalSettings settings;

    private volatile DeviceContext<Byte> externalDevice;
    private volatile OutputProvider display = OutputProvider.DUMMY;

    public ContextAdm3A(Keyboard keyboard, TerminalSettings settings) {
        this.keyboard = Objects.requireNonNull(keyboard);
        this.settings = Objects.requireNonNull(settings);
        keyboard.addOnKeyHandler(this::onKeyFromKeyboard);
    }

    public void setExternalDevice(DeviceContext<Byte> externalDevice) {
        this.externalDevice = Objects.requireNonNull(externalDevice);
    }

    public void setDisplay(OutputProvider outputProvider) {
        this.display = Objects.requireNonNull(outputProvider);
    }

    public void reset() {
        OutputProvider tmp = display;
        if (tmp != null) {
            tmp.reset();
        }
    }

    @Override
    public Byte readData() {
        return 0;
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
        DeviceContext<Byte> device = externalDevice;
        if (device != null) {
            device.writeData(key);
        }
        if (settings.isHalfDuplex()) {
            writeData(key);
        }
    }

    @Override
    public String toString() {
        return "LSI ADM-3A terminal";
    }
}
