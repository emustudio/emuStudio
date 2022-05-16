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
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.plugins.cpu.ram.api.RAMCpuContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;

import java.util.Objects;
import java.util.Optional;

public class RAMCpuContextImpl implements RAMCpuContext {
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;
    private AbstractTapeContext storageTape;
    private final ContextPool contextPool;

    public RAMCpuContextImpl(ContextPool contextPool) {
        this.contextPool = Objects.requireNonNull(contextPool);
    }

    public void init(long pluginID) throws PluginInitializationException {
        storageTape = setupTape(pluginID, "Storage", true, false, true, 0);
        inputTape = setupTape(pluginID, "Input tape", false, true, true, 1);
        outputTape = setupTape(pluginID, "Output tape", true, true, false, 2);
    }

    private AbstractTapeContext setupTape(long pluginID, String title, boolean clearAfterReset, boolean posVisible,
                                          boolean editable, int index)
        throws PluginInitializationException {

        AbstractTapeContext tape = contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, index);
        if (tape == null) {
            throw new PluginInitializationException("Could not get tape: \"" + title + "\"");
        }
        tape.setLeftBounded(true);
        tape.setEditable(editable);
        tape.setHighlightHeadPosition(posVisible);
        tape.setClearAtReset(clearAfterReset);
        tape.setTitle(title);
        tape.setShowPositions(true);

        return tape;
    }

    public AbstractTapeContext getStorageTape() {
        return storageTape;
    }

    public AbstractTapeContext getInputTape() {
        return inputTape;
    }

    public AbstractTapeContext getOutputTape() {
        return outputTape;
    }


    public void destroy() {
        Optional.ofNullable(inputTape).ifPresent(AbstractTapeContext::clear);
        Optional.ofNullable(storageTape).ifPresent(AbstractTapeContext::clear);
        Optional.ofNullable(outputTape).ifPresent(AbstractTapeContext::clear);
    }

    @Override
    public boolean isInterruptSupported() {
        return false;
    }

    @Override
    public void signalInterrupt(DeviceContext device, int mask) {

    }

    @Override
    public void clearInterrupt(DeviceContext device, int mask) {

    }

    @Override
    public boolean isRawInterruptSupported() {
        return false;
    }

    @Override
    public void signalRawInterrupt(DeviceContext device, byte[] data) {

    }

    @Override
    public int getCPUFrequency() {
        return 0;
    }
}