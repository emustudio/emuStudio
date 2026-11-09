/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.plugins.cpu.*;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.util.Locale;
import java.util.Objects;

public final class RamDecoder implements Decoder {
    private final RamMemoryContext memory;

    public RamDecoder(RamMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DecodedInstruction decode(int address) throws InvalidInstructionException {
        RamInstruction instruction = memory.read(address);
        if (instruction == null) throw new InvalidInstructionException("No instruction at " + address);
        RamInstruction.Opcode opcode = instruction.getOpcode();
        RamValue value = instruction.getOperand();
        String operand = value == null ? "" : value.getStringRepresentation();
        String mnemonic = String.format("%s %s%s", opcode.name().toLowerCase(Locale.ROOT),
                instruction.getDirection().value(), operand.toUpperCase(Locale.ROOT));
        String encoding = opcode.name() + " " + instruction.getDirection().name();
        if (value != null) encoding += " " + operand;
        Integer target = instruction.getLabel() == null ? null : instruction.getLabel().getAddress();
        if (target != null) encoding += " @" + target;
        return DecodedInstruction.of(address, 1, mnemonic, encoding);
    }
}
