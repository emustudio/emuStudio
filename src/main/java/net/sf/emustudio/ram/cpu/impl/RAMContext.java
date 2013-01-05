/*
 * RAMContext.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
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

import emulib.plugins.cpu.CPUContext;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;

public class RAMContext implements CPUContext {
    private EmulatorImpl cpu;
    private AbstractTapeContext[] tapes;

    public RAMContext(EmulatorImpl cpu) {
        this.cpu = cpu;
        tapes = new AbstractTapeContext[3];
    }

    public boolean init(long pluginID) {
        try {
            tapes[0] = (AbstractTapeContext)
                    ContextPool.getInstance().getDeviceContext(pluginID,
                    AbstractTapeContext.class, 0);
            if (tapes[0] == null) {
                StaticDialogs.showErrorMessage("Could not get the Registers"
                        + " (storage tape)");
                return false;
            }
            tapes[0].setBounded(true);
            tapes[0].setEditable(true);
            tapes[0].setPosVisible(false);
            tapes[0].setClearAtReset(true);
            tapes[0].setTitle("Registers (storage tape)");

            tapes[1] = (AbstractTapeContext)
                    ContextPool.getInstance().getDeviceContext(pluginID,
                    AbstractTapeContext.class, 1);
            if (tapes[1] == null) {
                StaticDialogs.showErrorMessage("Could not get the Input tape");
                return false;
            }
            tapes[1].setBounded(true);
            tapes[1].setEditable(true);
            tapes[1].setPosVisible(true);
            tapes[1].setClearAtReset(false);
            tapes[1].setTitle("Input tape");
            cpu.loadTape(tapes[1]);

            tapes[2] = (AbstractTapeContext)
                    ContextPool.getInstance().getDeviceContext(pluginID,
                    AbstractTapeContext.class, 2);
            if (tapes[2] == null) {
                StaticDialogs.showErrorMessage("Could not get the Output tape");
                return false;
            }
            tapes[2].setBounded(true);
            tapes[2].setEditable(false);
            tapes[2].setPosVisible(true);
            tapes[2].setClearAtReset(true);
            tapes[2].setTitle("Output tape");
        } catch (IndexOutOfBoundsException e) {
            StaticDialogs.showErrorMessage("One or more tapes needs to"
                    + " be connected to the CPU!");
            return false;
        }
        return true;
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
