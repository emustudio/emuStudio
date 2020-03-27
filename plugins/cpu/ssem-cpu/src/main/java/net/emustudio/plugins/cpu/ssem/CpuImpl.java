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
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Decoder;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.interaction.debugger.BreakpointColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerTable;
import net.emustudio.emulib.runtime.interaction.debugger.MnemoColumn;
import net.emustudio.emulib.runtime.interaction.debugger.OpcodeColumn;
import net.emustudio.plugins.cpu.ssem.gui.CpuPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
    type = PLUGIN_TYPE.CPU,
    title = "SSEM CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
    private final static Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private MemoryContext<Byte> memory;
    private Disassembler disassembler;
    private EmulatorEngine engine;

    private AutomaticEmulation automaticEmulation;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, MemoryContext.class);
        if (memory.getDataType() != Byte.class) {
            throw new PluginInitializationException(
                "Unexpected memory cell type. Expected Byte but was: " + memory.getDataType()
            );
        }
        Decoder decoder = new DecoderImpl(memory);
        disassembler = new DisassemblerImpl(memory, decoder);
        engine = new EmulatorEngine(memory, this);

        if (settings.getBoolean(PluginSettings.EMUSTUDIO_AUTO, false)) {
            automaticEmulation = new AutomaticEmulation(this, engine, memory);
        }
    }

    @Override
    protected void destroyInternal() {
        if (automaticEmulation != null) {
            automaticEmulation.destroy();
        }
    }

    @Override
    protected RunState stepInternal() {
        RunState result = engine.step();
        if (result == RunState.STATE_RUNNING) {
            return RunState.STATE_STOPPED_BREAK;
        } else {
            return result;
        }
    }

    @Override
    public JPanel getStatusPanel() {
        DebuggerTable debugTable = applicationApi.getDebuggerTable();
        if (debugTable != null) {
            debugTable.setDebuggerColumns(Arrays.asList(
                new BreakpointColumn(this), new LineColumn(), new MnemoColumn(disassembler),
                new OpcodeColumn(disassembler)
            ));
        }

        return new CpuPanel(this, engine, memory);
    }

    @Override
    public int getInstructionLocation() {
        return Math.max(0, engine.CI.get() + 4);
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
    public Disassembler getDisassembler() {
        return disassembler;
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
        return "Emulator of SSEM machine";
    }

    @Override
    public RunState call() {
        return engine.run();
    }

    @Override
    protected void resetInternal(int startPos) {
        engine.reset(startPos);
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.ssem.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
