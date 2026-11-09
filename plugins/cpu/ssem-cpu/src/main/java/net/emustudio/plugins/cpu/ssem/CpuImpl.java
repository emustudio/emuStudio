/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;


import net.emustudio.emulib.plugins.cpu.CPUContext;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.cpu.ssem.gui.CpuPanel;

import javax.swing.*;
import java.util.List;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "SSEM CPU",
        description = "Emulator of SSEM machine"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugins.debugger.Debugging {
    private final CpuContextImpl context = new CpuContextImpl();
    private MemoryContext<Byte> memory;
    private Disassembler disassembler;
    private EmulatorEngine engine;

    private AutomaticEmulation automaticEmulation;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        applicationApi.getContextPool().register(pluginID, context, CPUContext.class);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemory(pluginID, Byte.class);
        disassembler = new DisassemblerImpl(memory, new DecoderImpl(memory));
        engine = new EmulatorEngine(memory);
        context.setEngine(engine);

        if (settings.getBoolean(PluginSettings.EMUSTUDIO_AUTO).orElse(false)) {
            automaticEmulation = new AutomaticEmulation(context, engine, memory);
        }
    }

    @Override
    protected void destroyInternal() {
        if (automaticEmulation != null) {
            automaticEmulation.destroy();
        }
    }

    private RunState stepInternal() {
        RunState result = engine.step();
        if (result == RunState.STATE_RUNNING) {
            return RunState.STATE_STOPPED_BREAK;
        } else {
            return result;
        }
    }

    @Override
    public List<DebuggerColumn<?>> getDebuggerColumns() {
        return List.of(
                DebuggerColumn.text("line", location -> String.format("%04X", location / 4)),
                DebuggerColumn.mnemo(disassembler),
                DebuggerColumn.opcode(disassembler)
        );
    }

    @Override
    public JPanel getStatusPanel() {
        if (applicationApi.getGUI() == null) {
            return null;
        }
        return new CpuPanel(engine, memory, applicationApi.getGUI(), context, this::internalStateChanged);
    }

    @Override
    public int getAddressSpaceSize() {
        return memory.getSize();
    }

    private boolean setInstructionLocationInternal(int location) {
        int memSize = memory.getSize();
        if (location < 0 || location >= memSize) {
            throw new IllegalArgumentException("Instruction position can be in <0," + memSize / 4 + ">, but was: " + location);
        }
        engine.CI.set(Math.max(0, location * 4 - 4));
        return true;
    }

    @Override
    public Expression compile(String source) { return engine.debugger.compile(source); }

    @Override
    public AutoCloseable observe(Observer observer, boolean capture) { return engine.debugger.observe(observer, capture); }

    public JPanel debuggingEditor() { return engine.debugger.editor(this::internalStateChanged); }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    private RunState executeInternal() {
        return engine.run();
    }

    private void resetInternal(int startPos) {
        engine.reset(startPos);
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    private void publishState(RunState state) {
        context.publishState(state, context.getInstructionLocation(), context.getCpuFrequencyKhz());
    }

    private void internalStateChanged() {
        publishState(context.getRunState());
    }

    @Override
    public RunState execute() {
        RunState result = RunState.STATE_STOPPED_BAD_INSTR;
        publishState(RunState.STATE_RUNNING);
        try {
            result = executeInternal();
            return result;
        } finally {
            publishState(result == RunState.STATE_RUNNING ? RunState.STATE_STOPPED_BREAK : result);
        }
    }

    @Override
    public RunState step() {
        RunState result = RunState.STATE_STOPPED_BAD_INSTR;
        publishState(RunState.STATE_RUNNING);
        try {
            result = stepInternal();
            return result;
        } finally {
            publishState(result == RunState.STATE_RUNNING ? RunState.STATE_STOPPED_BREAK : result);
        }
    }

    @Override
    public void reset(int startPos) {
        resetInternal(startPos);
        publishState(RunState.STATE_STOPPED_BREAK);
    }

    @Override
    public boolean setInstructionLocation(int location) {
        boolean changed = setInstructionLocationInternal(location);
        if (changed) internalStateChanged();
        return changed;
    }
}
