/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.device.brainduck.terminal;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.brainduck.terminal.io.IOProvider;
import net.emustudio.plugins.device.brainduck.terminal.io.InputProvider;
import net.emustudio.plugins.device.brainduck.terminal.io.OutputProvider;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.util.Objects;

@ThreadSafe
class BrainTerminalContext implements DeviceContext<Byte>, IOProvider {
    private volatile InputProvider inputProvider = InputProvider.DUMMY;
    private volatile OutputProvider outputProvider = OutputProvider.DUMMY;

    void setInputProvider(InputProvider inputProvider) {
        this.inputProvider = Objects.requireNonNull(inputProvider);
    }

    void setOutputProvider(OutputProvider outputProvider) {
        this.outputProvider = Objects.requireNonNull(outputProvider);
    }

    @Override
    public Byte readData() {
        InputProvider tmpInputProvider = inputProvider;
        if (tmpInputProvider != null) {
            return tmpInputProvider.read();
        }
        return InputProvider.EOF;
    }

    @Override
    public void writeData(Byte data) {
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.write(data & 0xFF);
        }
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    @Override
    public void reset() {
        InputProvider tmpInputProvider = inputProvider;
        if (tmpInputProvider != null) {
            tmpInputProvider.reset();
        }
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.reset();
        }
    }

    @Override
    public void close() throws IOException {
        InputProvider tmpInputProvider = inputProvider;
        if (tmpInputProvider != null) {
            tmpInputProvider.close();
        }
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.close();
        }
    }

    void showGUI() {
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.showGUI();
        }
    }
}
