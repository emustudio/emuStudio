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
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.runtime.interaction.debugger.DebuggerColumn;
import net.emustudio.plugins.memory.ram.api.RAMLabel;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;

import java.util.Objects;

public class LabelDebugColumn implements DebuggerColumn<String> {
    private final RAMMemoryContext memory;

    public LabelDebugColumn(RAMMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public Class<String> getClassType() {
        return String.class;
    }

    @Override
    public String getTitle() {
        return "label";
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public void setValue(int location, Object o) {

    }

    @Override
    public String getValue(int location) {
        return memory.getLabel(location).map(RAMLabel::getLabel).orElse("");
    }
}
