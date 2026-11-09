/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.plugin.cpu.*;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;

public final class RaspDecoder implements Decoder {
    private final RaspMemoryContext memory;

    public RaspDecoder(RaspMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DecodedInstruction decode(int address) throws InvalidInstructionException {
        int opcode = memory.read(address);
        Optional<String> mnemonic = memory.disassembleMnemo(opcode);
        if (mnemonic.isEmpty()) throw new InvalidInstructionException("Invalid opcode " + opcode);
        int length = opcode == HALT ? 1 : 2;
        Integer value = length == 2 ? memory.read(address + 1) : null;
        boolean jump = opcode == JMP || opcode == JZ || opcode == JGTZ;
        String operand = value == null ? "" : String.valueOf(value);
        if (jump) operand = memory.getLabel(value).map(RaspLabel::getLabel).orElse(operand);
        String text = (mnemonic.get().toLowerCase(Locale.ROOT) + " " + operand.toUpperCase(Locale.ROOT)).trim();
        String encoding = Integer.toHexString(opcode);
        if (value != null) encoding += " " + Integer.toHexString(value);
        return DecodedInstruction.of(address, length, text, encoding);
    }
}
