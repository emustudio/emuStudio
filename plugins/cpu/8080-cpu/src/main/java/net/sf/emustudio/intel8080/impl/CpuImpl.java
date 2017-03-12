/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.intel8080.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import net.sf.emustudio.intel8080.api.ExtendedContext;
import net.sf.emustudio.intel8080.api.FrequencyUpdater;
import net.sf.emustudio.intel8080.gui.StatusPanel;

@PluginType(
        type = PLUGIN_TYPE.CPU,
        title = "Intel 8080 CPU",
        copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
        description = "Emulator of Intel 8080 CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
    public static final String PRINT_CODE = "printCode";
    public static final String PRINT_CODE_USE_CACHE = "printCodeUseCache";

    private final ScheduledExecutorService frequencyScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<Future> frequencyUpdaterFuture = new AtomicReference<>();

    private final ContextPool contextPool;
    private final ContextImpl context = new ContextImpl();

    private EmulatorEngine engine;
    private StatusPanel statusPanel;
    private Disassembler disassembler;

    public CpuImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        try {
            contextPool.register(pluginID, context, ExtendedContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register CPU Context", super.getTitle());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.intel8080.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        InitializerFor8080 initializer = new InitializerFor8080(this, getPluginID(), contextPool, settings, context);
        initializer.initialize();

        engine = initializer.getEngine();
        disassembler = initializer.getDisassembler();
        statusPanel = new StatusPanel(this, context, initializer.shouldDumpInstructions());
    }

    @Override
    protected void destroyInternal() {
        context.clearDevices();
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public void resetInternal(int startPos) {
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
    public Disassembler getDisassembler() {
        return disassembler;
    }

    @Override
    public int getInstructionPosition() {
        return engine.PC;
    }

    @Override
    public boolean setInstructionPosition(int position) {
        if (position < 0) {
            return false;
        }
        engine.PC = position & 0xFFFF;
        return true;
    }

}
