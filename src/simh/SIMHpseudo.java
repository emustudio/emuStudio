/*
 * SIMHpseudo.java
 *
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

import interfaces.C17E8D62E685AD7E54C209C30482E3C00C8C56ECC;
import interfaces.C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8;
import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.device.SimpleDevice;
import plugins.memory.IMemoryContext;
import runtime.Context;
import runtime.StaticDialogs;

/**
 * SIMH emulator's pseudo device
 * 
 * @author vbmacher
 */
public class SIMHpseudo extends SimpleDevice {

    private PseudoContext context;
    private C17E8D62E685AD7E54C209C30482E3C00C8C56ECC cpu;
    private C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8 mem;

    public SIMHpseudo(Long pluginID) {
        super(pluginID);
        context = new PseudoContext();
    }

    @Override
    public boolean initialize(ISettingsHandler sHandler) {
        cpu = (C17E8D62E685AD7E54C209C30482E3C00C8C56ECC)
                Context.getInstance().getCPUContext(pluginID,
                C17E8D62E685AD7E54C209C30482E3C00C8C56ECC.class);
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
                + "\u00A9 Copyright 2007-2010, Peter Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Some of the images taken from SIMH emulator contain Z80 or 8080"
                + " programs that communicate with the SIMH pseudo device "
                + "via port 0xfe. The version of the interface is: SIMH003";
    }

    @Override
    public String getVersion() {
        return "0.12b";
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
