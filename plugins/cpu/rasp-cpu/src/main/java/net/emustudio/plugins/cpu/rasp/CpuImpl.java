/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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
import net.emustudio.emulib.runtime.*;
import net.emustudio.emulib.runtime.interaction.debugger.BreakpointColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerTable;
import net.emustudio.emulib.runtime.interaction.debugger.MnemoColumn;
import net.emustudio.plugins.cpu.rasp.gui.LabelDebugColumn;
import net.emustudio.plugins.cpu.rasp.gui.RASPCpuStatusPanel;
import net.emustudio.plugins.cpu.rasp.gui.RASPDisassembler;
import net.emustudio.plugins.memory.rasp.NumberMemoryItem;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPInstruction;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

@PluginRoot(
    type = PLUGIN_TYPE.CPU,
    title = "Random Access Stored Program (RASP) machine"
)
public class CpuImpl extends AbstractCPU {
    private final static Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final RASPCpuContext context;

    private EmulatorEngine engine;
    private RASPMemoryContext memory;
    private RASPDisassembler disassembler;
    private RASPCpuStatusPanel gui;

    private boolean debugTableInitialized = false;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        ContextPool contextPool = applicationApi.getContextPool();
        this.context = new RASPCpuContext(contextPool);
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
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, RASPMemoryContext.class);
        engine = new EmulatorEngine(context, memory, applicationApi.getDialogs());
        disassembler = new RASPDisassembler(memory);
        context.init(pluginID);
        engine.loadInputs();
    }

    @Override
    public JPanel getStatusPanel() {
        //also initialize debug table
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
            gui = new RASPCpuStatusPanel(this);
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
    public RunState call() throws Exception {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (isBreakpointSet(engine.IP)) {
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
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (IOException ex) {
                LOGGER.error("Unexpected error while reading/writing to the tape", ex);
                return RunState.STATE_STOPPED_BAD_INSTR;
            } catch (Breakpoint breakpoint) {
                return RunState.STATE_STOPPED_BREAK;
            }
        }
        return RunState.STATE_STOPPED_NORMAL;
    }

    @Override
    protected RunState stepInternal() throws Exception {
        return engine.step();
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    @Override
    public int getInstructionLocation() {
        return engine.IP;
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
        MemoryItem memoryItem = memory.read(0);
        if (memoryItem instanceof NumberMemoryItem) {
            return ((NumberMemoryItem) memoryItem).getValue();
        } else if (memoryItem instanceof RASPInstruction) {
            return ((RASPInstruction) memoryItem).getCode();
        } else {
            return 0;
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.rasp.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
