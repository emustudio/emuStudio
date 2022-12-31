/*
 * This file is part of emuStudio.
 *
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
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.brainduck.gui.DecoderImpl;
import net.emustudio.plugins.cpu.brainduck.gui.DisassemblerImpl;
import net.emustudio.plugins.cpu.brainduck.gui.StatusPanel;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(type = PLUGIN_TYPE.CPU, title = "BrainDuck CPU")
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final BrainCPUContextImpl context = new BrainCPUContextImpl();

    private Disassembler disassembler;
    private ByteMemoryContext memory;
    private EmulatorEngine engine;
    private volatile boolean optimize;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        try {
            applicationApi.getContextPool().register(pluginID, context, BrainCPUContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register CPU context", e);
            applicationApi.getDialogs().showError("Could not register CPU context. Please see log file for details", getTitle());
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
        return "BrainDuck CPU emulator";
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, ByteMemoryContext.class);

        disassembler = new DisassemblerImpl(memory, new DecoderImpl(memory));
        engine = new EmulatorEngine(memory, context, new Profiler(memory));
    }

    @Override
    public int getInstructionLocation() {
        return engine.IP;
    }

    @Override
    public boolean setInstructionLocation(int location) {
        if (location < 0) {
            return false;
        }
        engine.IP = location;
        return true;
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public JPanel getStatusPanel() {
        return new StatusPanel(memory, this);
    }


    @Override
    public void resetInternal(int adr) {
        engine.reset(adr);
        LOGGER.debug("Register P was reset to " + engine.P);
    }

    @Override
    public RunState call() {
        optimize = true;
        memory.setMemoryNotificationsEnabled(false);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isBreakpointSet(engine.IP)) {
                        throw new Breakpoint();
                    }
                    RunState tmpRunState = stepInternal();
                    if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                        return tmpRunState;
                    }
                } catch (IndexOutOfBoundsException e) {
                    LOGGER.debug("Unexpected error", e);
                    return RunState.STATE_STOPPED_ADDR_FALLOUT;
                } catch (IOException e) {
                    LOGGER.error("Unexpected error", e);
                    return RunState.STATE_STOPPED_BAD_INSTR;
                } catch (Breakpoint er) {
                    return RunState.STATE_STOPPED_BREAK;
                }
            }
        } finally {
            optimize = false;
            memory.setMemoryNotificationsEnabled(true);
        }
        return RunState.STATE_STOPPED_NORMAL; // cannot be in finally block! it can rewrite breakpoint
    }

    @Override
    protected void destroyInternal() {
        context.detachDevice();
    }

    @Override
    protected RunState stepInternal() throws IOException {
        return engine.step(optimize);
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.brainduck.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
