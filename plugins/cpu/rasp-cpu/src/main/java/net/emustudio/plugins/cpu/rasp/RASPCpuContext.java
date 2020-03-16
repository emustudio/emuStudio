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
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RASPCpuContext implements CPUContext {
    private final List<AbstractTapeContext> tapes = new ArrayList<>(); //[0] -  input tape, [1] - output tape
    private final ContextPool contextPool;

    public RASPCpuContext(ContextPool contextPool) {
        this.contextPool = Objects.requireNonNull(contextPool);
    }

    public void init(long pluginID) throws PluginInitializationException {
        AbstractTapeContext inputTape = contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 0);
        //tape will be bounded by left (can't move beyond zeroth position)
        inputTape.setBounded(true);
        //user will be able to change the values on the output tape
        inputTape.setEditable(true);
        //the position will be highlighted by blue in the GUI
        inputTape.setHighlightHeadPosition(true);
        //we don't the input tape to be cleared when emulator is reset, so we do not have to provide inputs again after reset
        inputTape.setClearAtReset(false);
        inputTape.setTitle("Input tape");
        tapes.add(inputTape);

        AbstractTapeContext outputTape = contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 1);
        outputTape.setBounded(true);
        //we don't want the user to be able to change the values at the output tape
        outputTape.setEditable(false);
        outputTape.setHighlightHeadPosition(true);
        //outputs should be cleared at reset
        outputTape.setClearAtReset(true);
        outputTape.setTitle("Output tape");
        tapes.add(outputTape);
    }

    /**
     * Get the input tape.
     *
     * @return the input tape
     */
    public AbstractTapeContext getInputTape() {
        return tapes.get(0);
    }

    /**
     * Get the output tape.
     *
     * @return the output tape
     */
    public AbstractTapeContext getOutputTape() {
        return tapes.get(1);
    }

    /**
     * Destroy the tapes.
     */
    public void destroy() {
        tapes.clear();
    }

    @Override
    public boolean isRawInterruptSupported() {
        return false;
    }

    @Override
    public void signalRawInterrupt(DeviceContext dc, byte[] bytes) {
    }

    @Override
    public boolean isInterruptSupported() {
        return false;
    }

    @Override
    public void signalInterrupt(DeviceContext dc, int i) {
    }

    @Override
    public void clearInterrupt(DeviceContext dc, int i) {
    }

    @Override
    public int getCPUFrequency() {
        return 0;
    }
}
