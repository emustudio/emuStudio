/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.util.*;

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
        Integer intOperand = mnemo.isPresent() && opcode != HALT ? memory.read(address + 1) : null;
        String label = intOperand != null && isJump(opcode)
                ? memory.getLabel(intOperand).map(RaspLabel::getLabel).orElse(null) : null;
        return render(address, opcode, mnemo, intOperand, label);
    }

    private static DisassembledInstruction render(int address, int opcode, Optional<String> mnemo,
                                                  Integer intOperand, String label) {
        String operand = "";
        String encoding = Integer.toHexString(opcode);

        if (mnemo.isPresent() && opcode != HALT) {
            encoding += " " + Integer.toHexString(intOperand);
            String strOperand = String.valueOf(intOperand);
            operand = isJump(opcode) && label != null ? label : strOperand;
        }

        String strMnemo = String.format("%s %s", mnemo.orElse("").toLowerCase(), operand.toUpperCase()).trim();
        return new DisassembledInstruction(address, strMnemo, encoding);
    }

    private static boolean isJump(int opcode) { return opcode == JMP || opcode == JZ || opcode == JGTZ; }

    @Override
    public Optional<Disassembler> capture(int bank) {
        if (bank != 0) throw new IllegalArgumentException("RASP has one memory bank");
        RaspMemoryContext.RaspMemory captured = memory.getSnapshot();
        Map<Integer, Integer> cells = captured.programMemory;
        Map<Integer, String> copiedLabels = new HashMap<>();
        for (RaspLabel label : captured.labels) copiedLabels.put(label.getAddress(), label.getLabel());
        Map<Integer, String> labels = Map.copyOf(copiedLabels);
        return Optional.of(new Disassembler() {
            @Override public DisassembledInstruction disassemble(int address) {
                int opcode = cells.getOrDefault(address, 0);
                Optional<String> mnemonic = net.emustudio.plugins.memory.rasp.gui.Disassembler.disassembleMnemo(opcode);
                Integer operand = mnemonic.isPresent() && opcode != HALT ? cells.getOrDefault(address + 1, 0) : null;
                return render(address, opcode, mnemonic, operand, operand == null ? null : labels.get(operand));
            }
            @Override public int getNextInstructionPosition(int address) {
                int opcode = cells.getOrDefault(address, 0);
                return address + (!isInstruction(opcode) || opcode == HALT ? 1 : 2);
            }
        });
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
