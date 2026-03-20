/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Decoder;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.debugger.BreakpointColumn;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerTable;
import net.emustudio.emulib.runtime.ui.debugger.MnemoColumn;
import net.emustudio.emulib.runtime.ui.debugger.OpcodeColumn;
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
        Class<?> cellTypeClass = memory.getCellTypeClass();
        if (cellTypeClass != Byte.class) {
            throw new PluginInitializationException(
                    "Unexpected memory cell type. Expected Byte but was: " + cellTypeClass
            );
        }
        Decoder decoder = new DecoderImpl(memory);
        disassembler = new DisassemblerImpl(memory, decoder);
        engine = new EmulatorEngine(memory, this::isBreakpointSet);

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

        if (applicationApi.getGUI() == null) {
            return null;
        }
        return new CpuPanel(this, engine, memory, applicationApi.getGUI());
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
