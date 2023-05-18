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
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.plugins.cpu.rasp.api.RaspCpuContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import java.util.Optional;

public class RaspCpuContextImpl implements RaspCpuContext {
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;

    public void init(long pluginID, ContextPool contextPool) throws PluginInitializationException {
        inputTape = contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 0);
        inputTape.setLeftBounded(true);
        inputTape.setEditable(true);
        inputTape.setHighlightHeadPosition(true);
        inputTape.setClearAtReset(false);
        inputTape.setTitle("Input tape");
        inputTape.setAcceptTypes(TapeSymbol.Type.NUMBER);

        outputTape = contextPool.getDeviceContext(pluginID, AbstractTapeContext.class, 1);
        outputTape.setLeftBounded(true);
        outputTape.setEditable(false);
        outputTape.setHighlightHeadPosition(true);
        outputTape.setClearAtReset(true);
        outputTape.setTitle("Output tape");
        outputTape.setAcceptTypes(TapeSymbol.Type.NUMBER);
    }


    @Override
    public AbstractTapeContext getInputTape() {
        return inputTape;
    }

    @Override
    public AbstractTapeContext getOutputTape() {
        return outputTape;
    }

    public void destroy() {
        Optional.ofNullable(inputTape).ifPresent(AbstractTapeContext::clear);
        Optional.ofNullable(outputTape).ifPresent(AbstractTapeContext::clear);
    }

    @Override
    public boolean passedCyclesSupported() {
        return false;
    }

    @Override
    public void addPassedCyclesListener(PassedCyclesListener passedCyclesListener) {

    }

    @Override
    public void removePassedCyclesListener(PassedCyclesListener passedCyclesListener) {

    }
}
