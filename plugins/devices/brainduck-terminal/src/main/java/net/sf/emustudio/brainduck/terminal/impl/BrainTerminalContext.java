/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.brainduck.terminal.impl;

import emulib.plugins.device.DeviceContext;
import net.jcip.annotations.ThreadSafe;
import net.sf.emustudio.brainduck.terminal.io.IOProvider;
import net.sf.emustudio.brainduck.terminal.io.InputProvider;
import net.sf.emustudio.brainduck.terminal.io.OutputProvider;

import java.io.IOException;
import java.util.Objects;

@ThreadSafe
class BrainTerminalContext implements DeviceContext<Short>, IOProvider {
    private volatile InputProvider inputProvider = InputProvider.DUMMY;
    private volatile OutputProvider outputProvider = OutputProvider.DUMMY;

    void setInputProvider(InputProvider inputProvider) {
        this.inputProvider = Objects.requireNonNull(inputProvider);
    }

    void setOutputProvider(OutputProvider outputProvider) {
        this.outputProvider = Objects.requireNonNull(outputProvider);
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    @Override
    public Short read() {
        InputProvider tmpInputProvider = inputProvider;
        if (tmpInputProvider != null) {
            return (short)(tmpInputProvider.read() & 0xFF);
        }
        return InputProvider.EOF;
    }

    @Override
    public void write(Short val) {
        OutputProvider tmpOutputProvider = outputProvider;
        if (tmpOutputProvider != null) {
            tmpOutputProvider.write(val);
        }
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
