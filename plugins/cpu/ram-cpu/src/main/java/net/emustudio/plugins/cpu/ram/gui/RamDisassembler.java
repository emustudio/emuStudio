/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.util.Objects;

public class RamDisassembler implements Disassembler {
    private final RamMemoryContext memory;

    public RamDisassembler(RamMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DisassembledInstruction disassemble(int memLocation) {
        RamInstruction in = memory.read(memLocation);
        if (in == null) {
            return new DisassembledInstruction(memLocation, "", "");
        }
        RamValue operandValue = in.getOperand();
        String mnemo = String.format("%s %s%s",
                in.getOpcode().toString().toLowerCase(), in.getDirection().value(),
                operandValue != null ? operandValue.getStringRepresentation().toUpperCase() : "");
        return new DisassembledInstruction(memLocation, mnemo, "");
    }

    @Override
    public int getNextInstructionPosition(int memoryPosition) throws IndexOutOfBoundsException {
        return memoryPosition + 1;
    }
}
