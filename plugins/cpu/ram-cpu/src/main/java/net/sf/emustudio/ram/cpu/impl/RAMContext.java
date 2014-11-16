/*
 * Copyright (C) 2009-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
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

import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.CPUContext;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;

import java.util.Objects;

public class RAMContext implements CPUContext {
    private final EmulatorImpl cpu;
    private final AbstractTapeContext[] tapes;
    private final ContextPool contextPool;

    public RAMContext(EmulatorImpl cpu, ContextPool contextPool) {
        this.cpu = Objects.requireNonNull(cpu);
        this.contextPool = Objects.requireNonNull(contextPool);
        tapes = new AbstractTapeContext[3];
    }

    public void init(long pluginID) throws PluginInitializationException {
        try {
            tapes[0] = (AbstractTapeContext)
                    contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 0);
            if (tapes[0] == null) {
                throw new PluginInitializationException(
                        cpu, "Could not get the Registers (storage tape)"
                );
            }
            tapes[0].setBounded(true);
            tapes[0].setEditable(true);
            tapes[0].setPosVisible(false);
            tapes[0].setClearAtReset(true);
            tapes[0].setTitle("Registers (storage tape)");
            tapes[0].setDisplayRowNumbers(true);

            tapes[1] = (AbstractTapeContext)
                    contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 1);
            if (tapes[1] == null) {
                throw new PluginInitializationException(
                        cpu, "Could not get the Input tape"
                );
            }
            tapes[1].setBounded(true);
            tapes[1].setEditable(true);
            tapes[1].setPosVisible(true);
            tapes[1].setClearAtReset(false);
            tapes[1].setTitle("Input tape");
            cpu.loadTape(tapes[1]);

            tapes[2] = (AbstractTapeContext)contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 2);
            if (tapes[2] == null) {
                throw new PluginInitializationException(cpu, "Could not get the Output tape");
            }
            tapes[2].setBounded(true);
            tapes[2].setEditable(false);
            tapes[2].setPosVisible(true);
            tapes[2].setClearAtReset(true);
            tapes[2].setTitle("Output tape");
        } catch (PluginInitializationException e) {
            throw e;
        } catch (ContextNotFoundException | InvalidContextException e) {
            throw new PluginInitializationException(cpu, "One or more tapes needs to be connected to the CPU!", e);
        }
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

    /**
     * Method checks if the machine contains correct all 3 tapes
     *  - input
     *  - output
     *  - storage
     * @return true if yes, false otherwise
     */
    public boolean checkTapes() {
        for (int i = 0; i < 3; i++) {
            if (tapes[i] == null) {
                return false;
            }
        }
        return true;
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
