/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2006-2023  Peter Jakubčo
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

package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.interaction.debugger.BreakpointColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerTable;
import net.emustudio.emulib.runtime.interaction.debugger.MnemoColumn;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.rasp.gui.LabelDebugColumn;
import net.emustudio.plugins.cpu.rasp.gui.RaspDisassembler;
import net.emustudio.plugins.cpu.rasp.gui.RaspStatusPanel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Random Access Stored Program (RASP)"
)
public class CpuImpl extends AbstractCPU {
    private final static Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final RaspCpuContextImpl context = new RaspCpuContextImpl();
    private final ContextPool contextPool;

    private EmulatorEngine engine;
    private RaspMemoryContext memory;
    private RaspDisassembler disassembler;
    private RaspStatusPanel gui;

    private boolean debugTableInitialized = false;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.contextPool = applicationApi.getContextPool();
        try {
            contextPool.register(pluginID, context, CPUContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException ex) {
            LOGGER.error("Could not register RASP CPU context", ex);
            applicationApi.getDialogs().showError(
                    "Could not register RASP CPU context. Please see log file for more details", getTitle()
            );
        }
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, RaspMemoryContext.class);
        disassembler = new RaspDisassembler(memory);
        context.init(pluginID, contextPool);
        engine = new EmulatorEngine(memory, context.getInputTape(), context.getOutputTape());
    }

    @Override
    public JPanel getStatusPanel() {
        if (!debugTableInitialized) {
            DebuggerTable debugTable = applicationApi.getDebuggerTable();
            if (debugTable != null) {
                ArrayList<DebuggerColumn<?>> debugColumns = new ArrayList<>();
                debugColumns.add(new BreakpointColumn(this));
                debugColumns.add(new LabelDebugColumn(memory));
                debugColumns.add(new MnemoColumn(disassembler));
                debugTable.setDebuggerColumns(debugColumns);
            }
            debugTableInitialized = true;
        }
        if (gui == null) {
            gui = new RaspStatusPanel(this, context.getInputTape(), context.getOutputTape());
        }
        return gui;
    }

    @Override
    protected void resetInternal(int location) {
        engine.reset(location);
    }

    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    @Override
    public RunState call() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (isBreakpointSet(engine.IP.get())) {
                    throw new Breakpoint();
                }
                RunState tmpRunState = stepInternal();

                /*if RunState.STATE_STOPPED_BREAK is returned, it means that
                 the instruction was successful so just go on executing next instruction.
                 If HALT was executed or something else was returned, just return it*/
                if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                    return tmpRunState;
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.debug("Unexpected error", ex);
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (Breakpoint breakpoint) {
                return RunState.STATE_STOPPED_BREAK;
            } catch (Exception ex) {
                LOGGER.debug("Unexpected error", ex);
                return RunState.STATE_STOPPED_BAD_INSTR;
            }
        }
        return RunState.STATE_STOPPED_NORMAL;
    }

    @Override
    protected RunState stepInternal() throws IOException {
        return engine.step();
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    @Override
    public int getInstructionLocation() {
        return engine.IP.get();
    }

    @Override
    public boolean setInstructionLocation(int location) {
        return engine.setInstructionLocation(location);
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
        return "RASP machine emulator";
    }

    /**
     * Get current value of the accumulator (memory cell at address [0]).
     *
     * @return current value of the accumulator (memory cell at address [0])
     */
    public int getACC() {
        return memory.read(0);
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.rasp.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
