/*
 * SIMHpseudo.java
 *
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2012 Peter Jakubčo <pjakubco@gmail.com>
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
package simh;

import interfaces.C8E98DC5AF7BF51D571C03B7C96324B3066A092EA;
import interfaces.C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8;
import emulib.plugins.ISettingsHandler;
import emulib.plugins.device.SimpleDevice;
import emulib.runtime.Context;
import emulib.runtime.StaticDialogs;

/**
 * SIMH emulator's pseudo device
 * 
 * @author vbmacher
 */
public class SIMHpseudo extends SimpleDevice {

    private PseudoContext context;
    private C8E98DC5AF7BF51D571C03B7C96324B3066A092EA cpu;
    private C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8 mem;

    public SIMHpseudo(Long pluginID) {
        super(pluginID);
        context = new PseudoContext();
    }

    @Override
    public boolean initialize(ISettingsHandler sHandler) {
        cpu = (C8E98DC5AF7BF51D571C03B7C96324B3066A092EA)
                Context.getInstance().getCPUContext(pluginID,
                C8E98DC5AF7BF51D571C03B7C96324B3066A092EA.class);
        if (cpu == null) {
            StaticDialogs.showErrorMessage("SIMH-pseudo device has to be attached"
                    + " to a CPU");
            return false;
        }

        mem = (C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8)
                Context.getInstance().getMemoryContext(pluginID,
                C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8.class);
        if (mem == null) {
            StaticDialogs.showErrorMessage("SIMH-pseudo device has to be attached"
                    + " to a Memory");
            return false;
        }
        context.init(mem);

        // attach IO port
        if (this.cpu.attachDevice(context, 0xFE) == false) {
            StaticDialogs.showErrorMessage("Error: SIMH device can't be"
                    + " attached to CPU (maybe there is a hardware conflict)");
            return false;
        }
        reset();
        return true;
    }

    @Override
    public void showGUI() {
        StaticDialogs.showMessage("GUI not supported");
    }

    @Override
    public void reset() {
        context.reset();
    }

    @Override
    public String getTitle() {
        return "SIMH pseudo device";
    }

    @Override
    public String getCopyright() {
        return "Copyright (c) 2002-2007, Peter Schorn\n"
                + "\u00A9 Copyright 2007-2012, Peter Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Some of the images taken from SIMH emulator contain Z80 or 8080"
                + " programs that communicate with the SIMH pseudo device "
                + "via port 0xfe. The version of the interface is: SIMH003";
    }

    @Override
    public String getVersion() {
        return "0.13.1-SNAPSHOT";
    }

    @Override
    public void destroy() {
        this.context = null;
    }

    @Override
    public void showSettings() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
}
