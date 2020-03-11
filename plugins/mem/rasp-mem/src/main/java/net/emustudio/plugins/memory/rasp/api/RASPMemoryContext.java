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

package net.emustudio.plugins.memory.rasp.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.util.List;

/**
 * Context of the RASP memory.
 */
@PluginContext
public interface RASPMemoryContext extends MemoryContext<MemoryItem> {

    /**
     * Adds label to memory's set of labels.
     *
     * @param pos   adress which label refers to
     * @param label the string reprezentation of the label
     */
    void addLabel(int pos, String label);

    /**
     * Returns string reprezentation of the label at given address.
     *
     * @param pos the memory address
     * @return string reprezentation of the label at given address
     */
    String getLabel(int pos);

    void setProgramLocation(Integer programLocation);

    /**
     * Returns string representation of the label at given address, but if there
     * is no label for given address, just returns string representation of the
     * address.
     *
     * @param address the address
     * @return string representation of the label at given address, if there is
     * any
     */
    String addressToLabelString(int address);

    //from compiler
    void addInputs(List<Integer> input);

    // for CPU
    List<Integer> getInputs();
}
