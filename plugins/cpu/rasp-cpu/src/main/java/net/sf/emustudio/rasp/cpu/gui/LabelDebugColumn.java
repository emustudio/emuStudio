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

package net.sf.emustudio.rasp.cpu.gui;

import emulib.plugins.cpu.AbstractDebugColumn;
import net.sf.emustudio.rasp.memory.RASPMemoryContext;

import java.util.Objects;

/**
 * Debug column with labels, borrowed from RAM memory pluin (Copyright (C)
 * 2009-2012 Peter Jakubčo).
 */
public class LabelDebugColumn extends AbstractDebugColumn {

    private final RASPMemoryContext memory;

    /**
     * Constructor.
     *
     * @param memory memory to read labels from
     */
    public LabelDebugColumn(RASPMemoryContext memory) {
        super("LABEL", String.class, false);
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public void setDebugValue(int i, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Get label at given address from the memory.
     *
     * @param position the position in the memory
     * @return label at given address from the memory
     */
    @Override
    public Object getDebugValue(int position) {
        return memory.getLabel(position);
    }

}
