/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Decoder;
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
    private MemoryContext<Byte> memory;
    private Decoder disassembler;
    private EmulatorEngine engine;

    private AutomaticEmulation automaticEmulation;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemory(pluginID, Byte.class);
        disassembler = new DecoderImpl(memory);
        engine = new EmulatorEngine(memory);
        engine.debugger.onBoundary(() -> applicationApi.getExecutionState().processUpdates());

        if (settings.getBoolean(PluginSettings.EMUSTUDIO_AUTO).orElse(false)) {
            automaticEmulation = new AutomaticEmulation(applicationApi.getExecutionState(), engine, memory);
        }
    }

    @Override
    protected void destroyInternal() {
        if (automaticEmulation != null) {
            automaticEmulation.destroy();
        }
    }

    @Override
    public RunState step() {
        RunState result = engine.step();
        if (result == RunState.STATE_RUNNING) {
            return RunState.STATE_STOPPED_BREAK;
        } else {
            return result;
        }
    }

    @Override
    public List<DebuggerColumn<?>> getDebuggerColumns(Decoder instructions) {
        return List.of(
                DebuggerColumn.text("line", location -> String.format("%04X", location / 4)),
                DebuggerColumn.mnemo(instructions),
                DebuggerColumn.opcode(instructions)
        );
    }

    @Override
    public JPanel getStatusPanel() {
        if (applicationApi.getGUI() == null) {
            return null;
        }
        return new CpuPanel(engine, memory, applicationApi.getGUI(), applicationApi.getExecutionState());
    }

    @Override
    public int getInstructionLocation() {
        return Math.max(0, engine.CI.get() + 4);
    }

    @Override
    public int getAddressSpaceSize() {
        return memory.getSize();
    }

    @Override
    public boolean setInstructionLocation(int location) {
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
    public AutoCloseable observe(Observer observer, Capture capture) { return engine.debugger.observe(observer, capture); }

    public JPanel debuggingEditor() { return engine.debugger.editor(applicationApi.getExecutionState()::update); }

    @Override
    public Decoder getDecoder() {
        return disassembler;
    }

    @Override
    public RunState execute() {
        return engine.run();
    }

    @Override
    public void reset(int startPos) {
        engine.reset(startPos);
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

}
