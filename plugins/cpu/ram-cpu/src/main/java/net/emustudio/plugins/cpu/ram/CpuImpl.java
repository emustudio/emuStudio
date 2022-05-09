/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.interaction.debugger.BreakpointColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerTable;
import net.emustudio.emulib.runtime.interaction.debugger.MnemoColumn;
import net.emustudio.plugins.cpu.ram.gui.LabelDebugColumn;
import net.emustudio.plugins.cpu.ram.gui.RAMDisassembler;
import net.emustudio.plugins.cpu.ram.gui.RAMStatusPanel;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
    type = PLUGIN_TYPE.CPU,
    title = "Random Access Machine (RAM)"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final RAMCpuContextImpl context;

    private EmulatorEngine engine;
    private RAMMemoryContext memory;
    private RAMDisassembler disassembler;
    private boolean debugTableInitialized = false;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        context = new RAMCpuContextImpl(applicationApi.getContextPool());
        try {
            applicationApi.getContextPool().register(pluginID, context, CPUContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register RAM CPU context", e);
            applicationApi.getDialogs().showError(
                "Could not register RAM CPU Context. Please see log file for details.", super.getTitle()
            );
        }
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "Emulator of abstract RAM machine";
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, RAMMemoryContext.class);
        disassembler = new RAMDisassembler(memory);
        context.init(pluginID);
        engine = new EmulatorEngine(context.getInputTape(), context.getOutputTape(), context.getStorageTape(), memory);
    }

    @Override
    public JPanel getStatusPanel() {
        if (!debugTableInitialized) {
            DebuggerTable debugTable = applicationApi.getDebuggerTable();
            if (debugTable != null) {
                debugTable.setDebuggerColumns(Arrays.asList(
                    new BreakpointColumn(this), new LabelDebugColumn(memory), new MnemoColumn(disassembler)
                ));
            }
            debugTableInitialized = true;
        }
        return new RAMStatusPanel(this, context.getInputTape(), context.getOutputTape());
    }

    @Override
    public boolean setInstructionLocation(int location) {
        return engine.setInstructionLocation(location);
    }

    @Override
    public int getInstructionLocation() {
        return engine.IP;
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
    public void resetInternal(int location) {
        engine.reset(location);
    }

    @Override
    public RunState call() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (isBreakpointSet(engine.IP)) {
                    throw new Breakpoint();
                }
                RunState tmpRunState = stepInternal();
                if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                    return tmpRunState;
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.debug("Unexpected error", ex);
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (Breakpoint breakpoint) {
                return RunState.STATE_STOPPED_BREAK;
            } catch (Exception ex) {
                LOGGER.debug("Unexpected error while reading/writing to the tape", ex);
                return RunState.STATE_STOPPED_BAD_INSTR;
            }
        }
        return RunState.STATE_STOPPED_NORMAL;
    }

    @Override
    public RunState stepInternal() {
        return engine.step();
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.ram.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
