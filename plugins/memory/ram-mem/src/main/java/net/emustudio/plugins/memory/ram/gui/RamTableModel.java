/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class RamTableModel extends AbstractTableModel {
    private final RamMemoryContext memory;

    RamTableModel(RamMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return memory.getSize();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return String.valueOf(rowIndex);
            case 1:
                return memory.getLabel(rowIndex).map(RamLabel::getLabel).orElse("");
            case 2:
                RamInstruction i = memory.read(rowIndex);
                RamValue operandValue = i.getOperand();
                String operand = operandValue != null ? operandValue.getStringRepresentation().toUpperCase() : "";
                return i.getOpcode().toString().toLowerCase() + " " + operand;
        }
        return "";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int col) {
        switch (col) {
            case 0:
                return "Address";
            case 1:
                return "Label";
            case 2:
                return "Instruction";
        }
        return "";
    }

}
