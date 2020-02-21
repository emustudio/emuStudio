/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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

package net.sf.emustudio.rasp.memory;

import emulib.annotations.ContextType;
import emulib.plugins.memory.MemoryContext;
import net.sf.emustudio.rasp.memory.memoryitems.MemoryItem;

import java.util.List;

/**
 * Context of the RASP memory.
 *
 * @author miso
 */
@ContextType
public interface RASPMemoryContext extends MemoryContext<MemoryItem> {

    /**
     * Adds label to memory's set of labels.
     *
     * @param pos   adress which label refers to
     * @param label the string reprezentation of the label
     */
    public void addLabel(int pos, String label);

    /**
     * Returns string reprezentation of the label at given address.
     *
     * @param pos the memory address
     * @return string reprezentation of the label at given address
     */
    public String getLabel(int pos);

    void setProgramStart(Integer programStart);

    /**
     * Returns string representation of the label at given address, but if there
     * is no label for given address, just returns string representation of the
     * address.
     *
     * @param address the address
     * @return string representation of the label at given address, if there is
     * any
     */
    public String addressToLabelString(int address);

    //from compiler
    void addInputs(List<Integer> input);

    // for CPU
    List<Integer> getInputs();

}
