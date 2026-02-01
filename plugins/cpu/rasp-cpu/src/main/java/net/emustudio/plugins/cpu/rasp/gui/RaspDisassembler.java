/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;

public class RaspDisassembler implements Disassembler {
    private final RaspMemoryContext memory;

    public RaspDisassembler(RaspMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public DisassembledInstruction disassemble(int address) {
        int opcode = memory.read(address);
        Optional<String> mnemo = memory.disassembleMnemo(opcode);
        String operand = "";

        if (mnemo.isPresent() && opcode != HALT) {
            boolean isJump = (opcode == JMP) || (opcode == JZ) || (opcode == JGTZ);
            int intOperand = memory.read(address + 1);
            String strOperand = String.valueOf(intOperand);
            operand = isJump ? memory.getLabel(intOperand).map(RaspLabel::getLabel).orElse(strOperand) : strOperand;
        }

        String strMnemo = String.format("%s %s", mnemo.orElse("").toLowerCase(), operand.toUpperCase()).trim();
        return new DisassembledInstruction(address, strMnemo, Integer.toHexString(opcode));
    }

    @Override
    public int getNextInstructionPosition(int address) throws IndexOutOfBoundsException {
        int opcode = memory.read(address);
        if (!memory.isInstruction(opcode) || opcode == HALT) {
            return address + 1;
        }
        return address + 2;
    }
}
