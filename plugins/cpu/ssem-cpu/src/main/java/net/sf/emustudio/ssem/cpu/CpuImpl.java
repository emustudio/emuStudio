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
