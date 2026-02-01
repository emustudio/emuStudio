/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.util.Objects;

public class LabelDebugColumn implements DebuggerColumn<String> {

    private final RaspMemoryContext memory;

    public LabelDebugColumn(RaspMemoryContext memory) {
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
    public void setValue(int position, Object value) {

    }

    @Override
    public String getValue(int position) {
        return memory.getLabel(position).map(RaspLabel::getLabel).orElse("");
    }
}
