/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.intel8080.api.FrequencyUpdater;
import net.emustudio.plugins.cpu.zilogZ80.gui.StatusPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Zilog Z80 CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private static final String PRINT_CODE = "printCode";
    private static final String PRINT_CODE_USE_CACHE = "printCodeUseCache";

    private final ScheduledExecutorService frequencyScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<Future> frequencyUpdaterFuture = new AtomicReference<>();

    private final ContextZ80Impl context = new ContextZ80Impl();
    private final InitializerZ80 initializer;

    private StatusPanel statusPanel;
    private Disassembler disassembler;
    private EmulatorEngine engine;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        try {
            applicationApi.getContextPool().register(pluginID, context, Context8080.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register Z80 CPU context", e);
            applicationApi.getDialogs().showError(
                    "Could not register Z80 CPU context. Please see log file for more details.", getTitle()
            );
        }

        initializer = new InitializerZ80(
                this, pluginID, applicationApi.getContextPool(), settings, context
        );
    }

    @Override
    public int getInstructionLocation() {
        return engine.PC;
    }

    @Override
    public boolean setInstructionLocation(int location) {
        if (location < 0) {
            return false;
        }
        engine.PC = location & 0xFFFF;
        return true;
    }

    @Override
    public void initialize() throws PluginInitializationException {
        initializer.initialize();
        disassembler = initializer.getDisassembler();
        engine = initializer.getEngine();
        context.setEngine(engine);
        statusPanel = new StatusPanel(this, context, initializer.shouldDumpInstructions());
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public JPanel getStatusPanel() {
        return statusPanel;
    }

    private void stopFrequencyUpdater() {
        Future tmpFuture;

        do {
            tmpFuture = frequencyUpdaterFuture.get();
            if (tmpFuture != null) {
                tmpFuture.cancel(false);
            }
        } while (!frequencyUpdaterFuture.compareAndSet(tmpFuture, null));
    }

    private void startFrequencyUpdater() {
        Future tmpFuture;
        Future newFuture = frequencyScheduler.scheduleAtFixedRate(new FrequencyUpdater(engine), 0, 1, TimeUnit.SECONDS);

        do {
            tmpFuture = frequencyUpdaterFuture.get();
            if (tmpFuture != null) {
                tmpFuture.cancel(false);
            }
        } while (!frequencyUpdaterFuture.compareAndSet(tmpFuture, newFuture));
    }

    @Override
    public RunState call() {
        try {
            startFrequencyUpdater();
            return engine.run(this);
        } finally {
            stopFrequencyUpdater();
        }
    }

    @Override
    protected void resetInternal(int startPos) {
        engine.reset(startPos);
        stopFrequencyUpdater();
    }

    @Override
    public void pause() {
        super.pause();
        stopFrequencyUpdater();
    }

    @Override
    public void stop() {
        super.stop();
        stopFrequencyUpdater();
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
    protected void destroyInternal() {
        context.clearDevices();
        initializer.destroy();
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
        return "Emulator of Zilog Z80 CPU";
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.zilogZ80.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
