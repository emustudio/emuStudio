/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Caches rendered disassembly for debugger rows.
 * <p>
 * Some generated disassemblers keep mutable state in decoded operands, so repeated rendering of the same location can
 * produce different mnemonics. The debugger only needs the rendered text to stay stable until memory changes, which
 * makes a small address-based cache sufficient here.
 */
final class DisassemblyCache {
    private final Disassembler disassembler;
    private final Map<Integer, DisassembledInstruction> instructions = new HashMap<>();

    DisassemblyCache(Disassembler disassembler) {
        this.disassembler = Objects.requireNonNull(disassembler);
    }

    synchronized DisassembledInstruction get(int location) throws InvalidInstructionException {
        DisassembledInstruction instruction = instructions.get(location);
        if (instruction == null) {
            instruction = disassembler.disassemble(location);
            instructions.put(location, instruction);
        }
        return instruction;
    }

    synchronized void clear() {
        instructions.clear();
    }
}
