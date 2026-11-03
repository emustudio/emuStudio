/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RamDisassembler implements Disassembler {
    private final RamMemoryContext memory;

    public RamDisassembler(RamMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DisassembledInstruction disassemble(int memLocation) {
        return render(memLocation, memory.read(memLocation));
    }

    private static DisassembledInstruction render(int memLocation, RamInstruction in) {
        if (in == null) {
            return new DisassembledInstruction(memLocation, "", "");
        }
        RamValue operandValue = in.getOperand();
        String mnemo = String.format("%s %s%s",
                in.getOpcode().toString().toLowerCase(), in.getDirection().value(),
                operandValue != null ? operandValue.getStringRepresentation().toUpperCase() : "");
        String encoding = in.getOpcode().name() + " " + in.getDirection().name();
        if (operandValue != null) encoding += " " + operandValue.getStringRepresentation();
        if (in.getLabel() != null) encoding += " @" + in.getLabel().getAddress();
        return new DisassembledInstruction(memLocation, mnemo, encoding);
    }

    @Override
    public Optional<Disassembler> capture(int bank) {
        if (bank != 0) throw new IllegalArgumentException("RAM has one memory bank");
        RamMemoryContext.RamMemory captured = memory.getSnapshot();
        Map<Integer, DisassembledInstruction> rendered = new HashMap<>();
        // RAM cells, operands and labels are interfaces without an immutability guarantee.
        // Freeze every decoder-visible value into immutable strings; retain no guest objects.
        captured.programMemory.forEach((address, instruction) -> rendered.put(address, render(address, instruction)));
        Map<Integer, DisassembledInstruction> instructions = Map.copyOf(rendered);
        return Optional.of(new Disassembler() {
            @Override public DisassembledInstruction disassemble(int address) {
                DisassembledInstruction instruction = instructions.get(address);
                return instruction == null ? new DisassembledInstruction(address, "", "") : instruction;
            }
            @Override public int getNextInstructionPosition(int address) { return address + 1; }
        });
    }

    @Override
    public int getNextInstructionPosition(int memoryPosition) throws IndexOutOfBoundsException {
        return memoryPosition + 1;
    }
}
