/**
 * RAMContext.java
 * 
 *   KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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
package ramcpu.impl;

import interfaces.C50E67F515A7C87A67947F8FB0F82558196BE0AC7;
import emulib.plugins.cpu.ICPUContext;
import emulib.plugins.device.IDeviceContext;

import emulib.runtime.Context;
import emulib.runtime.StaticDialogs;

public class RAMContext implements ICPUContext {

    private RAM cpu;
    private C50E67F515A7C87A67947F8FB0F82558196BE0AC7[] tapes;

    public RAMContext(RAM cpu) {
        this.cpu = cpu;
        tapes = new C50E67F515A7C87A67947F8FB0F82558196BE0AC7[3];
    }

    public boolean init(long pluginID) {
        try {
            tapes[0] = (C50E67F515A7C87A67947F8FB0F82558196BE0AC7)
                    Context.getInstance().getDeviceContext(pluginID,
                    C50E67F515A7C87A67947F8FB0F82558196BE0AC7.class, 0);
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

            tapes[1] = (C50E67F515A7C87A67947F8FB0F82558196BE0AC7)
                    Context.getInstance().getDeviceContext(pluginID,
                    C50E67F515A7C87A67947F8FB0F82558196BE0AC7.class, 1);
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

            tapes[2] = (C50E67F515A7C87A67947F8FB0F82558196BE0AC7)
                    Context.getInstance().getDeviceContext(pluginID,
                    C50E67F515A7C87A67947F8FB0F82558196BE0AC7.class, 2);
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

    @Override
    public String getID() {
        return "ram-cpu-context";
    }

    public C50E67F515A7C87A67947F8FB0F82558196BE0AC7 getStorage() {
        return tapes[0];
    }

    public C50E67F515A7C87A67947F8FB0F82558196BE0AC7 getInput() {
        return tapes[1];
    }

    public C50E67F515A7C87A67947F8FB0F82558196BE0AC7 getOutput() {
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
    public void setInterrupt(IDeviceContext device, int mask) {

    }

    @Override
    public void clearInterrupt(IDeviceContext device, int mask) {
        
    }
}
