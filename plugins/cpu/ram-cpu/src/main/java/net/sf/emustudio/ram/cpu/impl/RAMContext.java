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
package net.sf.emustudio.ram.cpu.impl;

import emulib.plugins.cpu.CPUContext;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;

import java.util.Objects;

public class RAMContext implements CPUContext {
    private final AbstractTapeContext[] tapes;
    private final ContextPool contextPool;

    public RAMContext(ContextPool contextPool) {
        this.contextPool = Objects.requireNonNull(contextPool);
        tapes = new AbstractTapeContext[3];
    }

    public void init(long pluginID, EmulatorEngine engine) throws PluginInitializationException {
        tapes[0] = prepareTape(pluginID, "Registers (storage tape)", true, false, true, 0);
        tapes[1] = prepareTape(pluginID, "Input tape", false, true, true, 1);
        tapes[2] = prepareTape(pluginID, "Output tape", true, true, false, 2);

        engine.loadInput(tapes[1]);
    }

    private AbstractTapeContext prepareTape(long pluginID, String title, boolean clearAfterReset, boolean posVisible,
                                            boolean editable, int index)
        throws PluginInitializationException {

        AbstractTapeContext tape = contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, index);
        if (tape == null) {
            throw new PluginInitializationException("Could not get tape: \"" + title + "\"");
        }
        tape.setBounded(true);
        tape.setEditable(editable);
        tape.setPosVisible(posVisible);
        tape.setClearAtReset(clearAfterReset);
        tape.setTitle(title);
        tape.setDisplayRowNumbers(true);

        return tape;
    }

    public AbstractTapeContext getStorage() {
        return tapes[0];
    }

    public AbstractTapeContext getInput() {
        return tapes[1];
    }

    public AbstractTapeContext getOutput() {
        return tapes[2];
    }


    public void destroy() {
        for (int i = 0; i < 3; i++) {
            tapes[i] = null;
        }
    }

    @Override
    public boolean isInterruptSupported() {
        return false;
    }

    @Override
    public void signalInterrupt(DeviceContext device, int mask) {

    }

    @Override
    public void clearInterrupt(DeviceContext device, int mask) {

    }

    @Override
    public boolean isRawInterruptSupported() {
        return false;
    }

    @Override
    public void signalRawInterrupt(DeviceContext device, byte[] data) {

    }

    @Override
    public int getCPUFrequency() {
        return 0;
    }
}
