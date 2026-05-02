/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.plugins.cpu.ram.api.RamCpuContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;

import java.util.Objects;
import java.util.Optional;

public class RamCpuContextImpl implements RamCpuContext {
    private final ContextPool contextPool;
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;
    private AbstractTapeContext storageTape;

    public RamCpuContextImpl(ContextPool contextPool) {
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
    public boolean isPassedCyclesSupported() {
        return false;
    }

    @Override
    public void addPassedCyclesListener(PassedCyclesListener passedCyclesListener) {

    }

    @Override
    public void removePassedCyclesListener(PassedCyclesListener passedCyclesListener) {

    }
}
