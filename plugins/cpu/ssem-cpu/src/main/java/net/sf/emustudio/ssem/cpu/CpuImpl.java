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
package net.sf.emustudio.ssem.cpu;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.ContextPool;

import javax.swing.*;

@PluginType(
    type = PLUGIN_TYPE.CPU,
    title = "SSEM CPU",
    copyright = "\u00A9 Copyright 2016, Peter Jakubčo",
    description = "Emulator of SSEM CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {

    public CpuImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
    }

    @Override
    protected void destroyInternal() {

    }

    @Override
    protected RunState stepInternal() throws Exception {
        return null;
    }

    @Override
    public JPanel getStatusPanel() {
        return null;
    }

    @Override
    public int getInstructionPosition() {
        return 0;
    }

    @Override
    public boolean setInstructionPosition(int i) {
        return false;
    }

    @Override
    public Disassembler getDisassembler() {
        return null;
    }

    @Override
    public void initialize(SettingsManager settingsManager) throws PluginInitializationException {

    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public RunState call() throws Exception {
        return null;
    }
}
