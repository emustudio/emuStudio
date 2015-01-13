/*
 * Implementation of CPU emulation
 *
 * Created on Piatok, 2007, oktober 26, 10:45
 *
 * Copyright (C) 2007-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR regs[REG_A] PARTICULAR PURPOSE.  See the
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
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.intel8080.ExtendedContext;
import net.sf.emustudio.intel8080.FrequencyChangedListener;
import net.sf.emustudio.intel8080.gui.DecoderImpl;
import net.sf.emustudio.intel8080.gui.DisassemblerImpl;
import net.sf.emustudio.intel8080.gui.StatusPanel;

import javax.swing.JPanel;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

@PluginType(type = PLUGIN_TYPE.CPU,
title = "Intel 8080 CPU",
copyright = "\u00A9 Copyright 2007-2014, Peter Jakubčo",
description = "Emulator of Intel 8080 CPU")
public class CpuImpl extends AbstractCPU {
    private final ContextImpl context;
    private Disassembler disassembler;
    // cpu speed
    private final java.util.Timer frequencyScheduler;
    private FrequencyUpdater frequencyUpdater;

    private final ContextPool contextPool;
    private final List<FrequencyChangedListener> frequencyChangedListeners = new CopyOnWriteArrayList<>();

    private EmulatorEngine engine;
    private StatusPanel statusPanel;

    /**
     * This class performs runtime frequency calculation and updating.
     *
     * Given: time, executed cycles count Frequency is defined as number of something by some period of time. Hz = 1/s,
     * kHz = 1000/s time has to be in seconds
     *
     * CC ..by.. time[s] XX ..by.. 1 [s] ? --------------- XX:CC = 1:time XX = CC / time [Hz]
     */
    private class FrequencyUpdater extends TimerTask {

        private long startTimeSaved = 0;
        private float frequency;

        @Override
        public void run() {
            double endTime = System.nanoTime();
            double time = endTime - startTimeSaved;
            long executedCycles = engine.getAndResetExecutedCycles();

            if (executedCycles == 0) {
                return;
            }
            frequency = (float) (executedCycles / (time / 1000000.0));
            startTimeSaved = (long) endTime;
            fireFrequencyChanged(frequency);
        }
    }

    public CpuImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        context = new ContextImpl(this);
        try {
            contextPool.register(pluginID, context, ExtendedContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register CPU Context", getTitle());
        }
        frequencyUpdater = new FrequencyUpdater();
        frequencyScheduler = new Timer();
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
    public void initialize(SettingsManager settings) throws PluginInitializationException{
        try {
            MemoryContext<Short> memory = contextPool.getMemoryContext(getPluginID(), MemoryContext.class);

            if (memory.getDataType() != Short.class) {
                throw new PluginInitializationException(
                        this,
                        "Operating memory type is not supported for this kind of CPU."
                );
            }

            // create disassembler and debug columns
            this.disassembler = new DisassemblerImpl(memory, new DecoderImpl(memory));
            this.engine = new EmulatorEngine(memory, context);
            statusPanel = new StatusPanel(this, context);
        } catch (InvalidContextException | ContextNotFoundException e) {
            throw new PluginInitializationException(this, ": Could not get memory context", e);
        }
    }

    @Override
    protected void destroyInternal() {
        context.clearDevices();
        frequencyChangedListeners.clear();
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public void reset(int startPos) {
        super.reset(startPos);
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

    public void addFrequencyChangedListener(FrequencyChangedListener listener) {
        frequencyChangedListeners.add(listener);
    }

    public void removeFrequencyChangedListener(FrequencyChangedListener listener) {
        frequencyChangedListeners.remove(listener);
    }

    private void fireFrequencyChanged(float newFrequency) {
        for (FrequencyChangedListener listener : frequencyChangedListeners) {
            listener.frequencyChanged(newFrequency);
        }
    }

    @Override
    public JPanel getStatusPanel() {
        return statusPanel;
    }

    public int getSliceTime() {
        return engine.checkTimeSlice;
    }

    public void setSliceTime(int t) {
        engine.checkTimeSlice = t;
    }

    private void stopFrequencyUpdater() {
        try {
            frequencyUpdater.cancel();
            frequencyUpdater = new FrequencyUpdater();
        } catch (Exception e) {
        }
    }

    private void runFrequencyUpdater() {
        try {
            frequencyScheduler.purge();
            frequencyScheduler.scheduleAtFixedRate(
                    frequencyUpdater, 0, 2 * engine.checkTimeSlice
            );
        } catch (Exception e) {
        }
    }

    /**
     * Run a CPU execution (thread).
     *
     * Real-time CPU frequency balancing *********************************
     *
     * 1 cycle is performed in 1 periode of CPU frequency. CPU_PERIODE = 1 / CPU_FREQ [micros]
     * cycles_to_execute_per_second = 1000 / CPU_PERIODE
     *
     * cycles_to_execute_per_second = 1000 / (1/CPU_FREQ) cycles_to_execute_per_second = 1000 * CPU_FREQ
     *
     * 1000 s = 1 micros => slice_length (can vary)
     *
     */
    @Override
    public RunState call() {
        try {
            runFrequencyUpdater();
            return engine.run(this);
        } finally {
            stopFrequencyUpdater();
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
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
        engine.PC = position;
        return true;
    }

    @Override
    public void showSettings() {
        // TODO Auto-generated method stub
    }
}
