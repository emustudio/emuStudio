/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;

import java.util.Objects;

public class LabelDebugColumn implements DebuggerColumn<String> {
    private final RamMemoryContext memory;

    public LabelDebugColumn(RamMemoryContext memory) {
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
        return memory.getLabel(location).map(RamLabel::getLabel).orElse("");
    }
}
