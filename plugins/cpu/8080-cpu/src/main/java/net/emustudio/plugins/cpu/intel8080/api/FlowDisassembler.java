/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.api;

import static net.emustudio.emulib.plugins.cpu.InstructionFlow.Kind.*;

import net.emustudio.emulib.plugins.cpu.*;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.util.*;

/** Opcode-based 8080/Z80 flow adapter. Generated disassembly remains owned by Edigen. */
public final class FlowDisassembler implements Disassembler {
    private final Disassembler delegate;
    private final MemoryContext<Byte> memory;
    private final boolean z80;

    public FlowDisassembler(Disassembler delegate, MemoryContext<Byte> memory, boolean z80) {
        this.delegate = delegate;
        this.memory = memory;
        this.z80 = z80;
    }

    @Override
    public DisassembledInstruction disassemble(int at) throws InvalidInstructionException {
        return delegate.disassemble(at);
    }

    @Override
    public int getNextInstructionPosition(int at) {
        return delegate.getNextInstructionPosition(at);
    }

    private int byteAt(int at) {
        return memory.readBank(at & 65535, memory.getSelectedBank()) & 255;
    }

    @Override
    public Optional<InstructionFlow> getFlow(int at) {
        int op = byteAt(at), operand = at + 1, next = getNextInstructionPosition(at) & 65535;
        if (z80 && (op == 0xDD || op == 0xFD)) {
            // Repeated index prefixes are legal; bound decoding to prevent a prefix-only image
            // loop.
            int prefixes = 0;
            do {
                if (++prefixes > 16) return Optional.empty();
                op = byteAt(operand++);
            } while (op == 0xDD || op == 0xFD);
        }
        InstructionFlow.Kind kind = LINEAR;
        boolean fall = true;
        List<Integer> targets = List.of();
        if (z80 && (op == 0x18 || op == 0x10 || (op & 0xE7) == 0x20)) {
            kind = JUMP;
            fall = op != 0x18;
            targets = List.of((next + (byte) byteAt(operand)) & 65535);
        } else if (op == 0xC3 || (op & 0xC7) == 0xC2 || op == 0xCD || (op & 0xC7) == 0xC4) {
            kind = op == 0xCD || (op & 0xC7) == 0xC4 ? CALL : JUMP;
            fall = kind == CALL || op != 0xC3;
            targets = List.of(byteAt(operand) | (byteAt(operand + 1) << 8));
        } else if ((op & 0xC7) == 0xC7) {
            kind = CALL;
            targets = List.of(op & 0x38);
        } else if (op == 0xE9) {
            kind = JUMP;
            fall = false;
        } else if (op == 0xC9 || (op & 0xC7) == 0xC0) {
            kind = RETURN;
            fall = op != 0xC9;
        } else if (z80 && op == 0xED && (byteAt(operand) & 0xC7) == 0x45) {
            kind = RETURN;
            fall = false;
        } else if (op == 0x76) {
            kind = STOP;
            fall = false;
        }
        return Optional.of(new InstructionFlow(kind, next, fall, targets));
    }
}
