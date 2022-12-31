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
package net.emustudio.plugins.device.zxspectrum.display;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.zxspectrum.display.io.IOProvider;
import net.emustudio.plugins.device.zxspectrum.display.io.OutputProvider;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.util.Objects;

@ThreadSafe
class ZxSpectrumDisplayContext implements DeviceContext<Byte>, IOProvider {
    private volatile OutputProvider outputProvider = OutputProvider.DUMMY;

    void setOutputProvider(OutputProvider outputProvider) {
        this.outputProvider = Objects.requireNonNull(outputProvider);
    }

    @Override
    public Byte readData() {
        return 0;
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
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.reset();
        }
    }

    @Override
    public void close() throws IOException {
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
