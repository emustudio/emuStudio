/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.compiler.ram.ParsingUtils;
import net.emustudio.plugins.compiler.ram.exceptions.CompileException;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMValue;

import java.io.*;
import java.util.*;

public class Program {
    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<String, Label> labels = new HashMap<>();
    private final List<Value> inputs = new ArrayList<>();

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

    public void add(Value input) {
        this.inputs.add(input);
    }

    public void assignLabels() {
        for (Instruction instruction : instructions) {
            instruction
                .getOperand()
                .filter(v -> v.getType() == RAMValue.Type.ID)
                .map(RAMValue::getStringValue)
                .flatMap(this::getLabel)
                .ifPresent(instruction::setLabel);
        }
    }

    public void loadIntoMemory(RAMMemoryContext memory) {
        memory.setLabels(new ArrayList<>(labels.values()));
        memory.setInputs(new ArrayList<>(inputs));
        for (RAMInstruction instruction : instructions) {
            memory.write(instruction.getAddress(), instruction);
        }
    }

    public void saveToFile(String filename) throws IOException {
        Map<Integer, String> labels = new HashMap<>();
        for (Label label : this.labels.values()) {
            labels.put(label.getAddress(), label.getLabel());
        }

        OutputStream file = new FileOutputStream(filename);
        OutputStream buffer = new BufferedOutputStream(file);
        try (ObjectOutput output = new ObjectOutputStream(buffer)) {
            output.writeObject(labels);
            output.writeObject(inputs);
            output.writeObject(instructions);
        }
    }

    private Optional<Label> getLabel(String name) {
        String labelNorm = ParsingUtils.normalizeId(name);
        return Optional.ofNullable(labels.get(labelNorm));
    }
}