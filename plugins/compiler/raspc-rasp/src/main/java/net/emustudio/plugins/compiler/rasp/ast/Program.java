/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.rasp.ast;

import net.emustudio.plugins.compiler.rasp.ParsingUtils;
import net.emustudio.plugins.compiler.rasp.exceptions.CompileException;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryCell;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.io.*;
import java.util.*;

public class Program {
    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<String, Label> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();

    public void add(Instruction instruction) {
        this.instructions.add(instruction);
    }

    public void add(Label label) {
        String labelNorm = ParsingUtils.normalizeId(label.getLabel());
        if (labels.containsKey(labelNorm)) {
            throw new CompileException(label.line, label.column, "Label is already defined!");
        }
        this.labels.put(labelNorm, label);
    }

    public void add(int input) {
        this.inputs.add(input);
    }

    public Map<Integer, RaspMemoryCell> compile() {
        Map<Integer, RaspMemoryCell> compiled = new HashMap<>();

        for (Instruction instruction : instructions) {
            RaspMemoryCell instrCell = new RaspMemoryCellImpl(true, instruction.opcode, instruction.address);
            compiled.put(instrCell.getAddress(), instrCell);
            instruction.operand.ifPresent(o -> {
                RaspMemoryCell cell = new RaspMemoryCellImpl(false, o, instruction.address + 1);
                compiled.put(cell.getAddress(), cell);
            });
            instruction.id.ifPresent(id -> {
                Optional<Label> label = getLabel(id);
                if (label.isEmpty()) {
                    throw new CompileException(instruction.line, instruction.column, "Label is not defined");
                }
                RaspMemoryCell cell = new RaspMemoryCellImpl(false, label.get().getAddress(), instruction.address + 1);
                compiled.put(cell.getAddress(), cell);
            });
        }
        return compiled;
    }

    public void loadIntoMemory(RaspMemoryContext memory, Map<Integer, RaspMemoryCell> compiled) {
        memory.setLabels(new ArrayList<>(labels.values()));
        memory.setInputs(new ArrayList<>(inputs));
        for (RaspMemoryCell cell : compiled.values()) {
            memory.write(cell.getAddress(), cell);
        }
    }

    public void saveToFile(String filename, Map<Integer, RaspMemoryCell> compiled) throws IOException {
        Map<Integer, String> labels = new HashMap<>();
        for (Label label : this.labels.values()) {
            labels.put(label.getAddress(), label.getLabel());
        }

        OutputStream file = new FileOutputStream(filename);
        OutputStream buffer = new BufferedOutputStream(file);
        try (ObjectOutput output = new ObjectOutputStream(buffer)) {
            output.writeObject(getProgramLocation(compiled));
            output.writeObject(labels);
            output.writeObject(inputs);
            output.writeObject(compiled);
        }
    }

    public int getProgramLocation(Map<Integer, RaspMemoryCell> compiled) {
        return compiled.keySet().stream().min(Comparator.naturalOrder()).orElse(0);
    }

    private Optional<Label> getLabel(String name) {
        String labelNorm = ParsingUtils.normalizeId(name);
        return Optional.ofNullable(labels.get(labelNorm));
    }

    public static class RaspMemoryCellImpl implements RaspMemoryCell {
        private final boolean isInstruction;
        private final int value;
        private final int address;

        public RaspMemoryCellImpl(boolean isInstruction, int value, int address) {
            this.isInstruction = isInstruction;
            this.value = value;
            this.address = address;
        }

        @Override
        public boolean isInstruction() {
            return isInstruction;
        }

        @Override
        public int getAddress() {
            return address;
        }

        @Override
        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RaspMemoryCellImpl that = (RaspMemoryCellImpl) o;
            return isInstruction == that.isInstruction && value == that.value && address == that.address;
        }

        @Override
        public int hashCode() {
            return Objects.hash(isInstruction, value, address);
        }
    }
}
