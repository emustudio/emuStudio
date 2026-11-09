/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.cpu.ram.gui.LabelDebugColumn;
import net.emustudio.plugins.cpu.ram.gui.RamDisassembler;
import net.emustudio.plugins.cpu.ram.gui.RamStatusPanel;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Random Access Machine (RAM)",
        description = "Emulator of abstract RAM machine"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugins.debugger.Debugging {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final RamCpuContextImpl context;

    private EmulatorEngine engine;
    private RamMemoryContext memory;
    private RamDisassembler disassembler;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        context = new RamCpuContextImpl(applicationApi.getContextPool());
        applicationApi.getContextPool().register(pluginID, context, CPUContext.class);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().get(pluginID, RamMemoryContext.class);
        disassembler = new RamDisassembler(memory);
        context.init(pluginID);
        engine = new EmulatorEngine(context.getInputTape(), context.getOutputTape(), context.getStorageTape(), memory);
    }

    @Override
    public List<DebuggerColumn<?>> getDebuggerColumns() {
        return List.of(new LabelDebugColumn(memory), DebuggerColumn.mnemo(disassembler));
    }

    @Override
    public JPanel getStatusPanel() {
        if (applicationApi.getGUI() == null) {
            return null;
        }
        return new RamStatusPanel(this, context.getInputTape(), context.getOutputTape(), applicationApi.getGUI(), applicationApi.getExecutionState());
    }

    @Override
    public boolean setInstructionLocation(int location) {
        return engine.setInstructionLocation(location);
    }

    @Override
    public int getInstructionLocation() {
        return engine.IP.get();
    }

    @Override
    public int getAddressSpaceSize() {
        // Program memory grows with the loaded program; addresses are not bounded by its current size.
        return Integer.MAX_VALUE;
    }

    public TapeSymbol getR0() {
        AbstractTapeContext storage = context.getStorageTape();
        if (storage == null) {
            return TapeSymbol.EMPTY;
        }
        return storage.getSymbolAt(0).orElse(TapeSymbol.EMPTY);
    }

    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    @Override
    public void reset(int location) {
        engine.reset(location);
    }

    @Override
    public RunState execute() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RunState tmpRunState = step();
                if (engine.debugStopped) return RunState.STATE_STOPPED_BREAK;
                if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                    return tmpRunState;
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.debug("Unexpected error", ex);
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (Exception ex) {
                LOGGER.debug("Unexpected error", ex);
                return RunState.STATE_STOPPED_BAD_INSTR;
            }
        }
        return RunState.STATE_RUNNING;
    }

    @Override
    public RunState step() throws IOException {
        return engine.step();
    }

    @Override
    public Expression compile(String source) { return engine.debugger.compile(source); }

    @Override
    public void onBoundary(Runnable callback) { engine.debugger.onBoundary(callback); }

    @Override
    public AutoCloseable observe(Observer observer, boolean capture) { return engine.debugger.observe(observer, capture); }

    public JPanel debuggingEditor() { return engine.debugger.editor(applicationApi.getExecutionState()::update); }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

}
