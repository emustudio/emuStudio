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
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.compiler.ram.ParsingUtils;
import net.emustudio.plugins.compiler.ram.exceptions.CompileException;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class Program {
    private final static Set<RamInstruction.Opcode> nonNegative = Set.of(
            RamInstruction.Opcode.READ, RamInstruction.Opcode.WRITE,
            RamInstruction.Opcode.STORE, RamInstruction.Opcode.LOAD, RamInstruction.Opcode.ADD,
            RamInstruction.Opcode.SUB, RamInstruction.Opcode.MUL, RamInstruction.Opcode.DIV);
    private final static Set<RamInstruction.Direction> directions = Set.of(
            RamInstruction.Direction.DIRECT, RamInstruction.Direction.INDIRECT);


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
                    .filter(v -> v.getType() == RamValue.Type.ID)
                    .map(RamValue::getStringValue)
                    .flatMap(this::getLabel)
                    .ifPresent(instruction::setLabel);
        }
    }

    public void check() {
        for (Instruction instruction : instructions) {
            if (nonNegative.contains(instruction.getOpcode()) && directions.contains(instruction.getDirection())) {
                Optional<RamValue> error = instruction
                        .getOperand()
                        .filter(op -> op.getType() == RamValue.Type.NUMBER)
                        .filter(op -> op.getNumberValue() < 0);
                if (error.isPresent()) {
                    throw new CompileException(instruction.line, instruction.column, "Register number cannot be negative");
                }
            }
        }
    }

    public void loadIntoMemory(RamMemoryContext memory) {
        memory.setLabels(new ArrayList<>(labels.values()));
        memory.setInputs(new ArrayList<>(inputs));
        for (RamInstruction instruction : instructions) {
            memory.write(instruction.getAddress(), instruction);
        }
    }

    public void saveToFile(Path filename) throws IOException {
        Map<Integer, RamInstruction> programMemory = new HashMap<>();
        for (RamInstruction instruction : instructions) {
            programMemory.put(instruction.getAddress(), instruction);
        }
        RamMemoryContext.serialize(filename, new RamMemoryContext.RamMemory(
                this.labels.values(), programMemory, inputs
        ));
    }

    private Optional<Label> getLabel(String name) {
        String labelNorm = ParsingUtils.normalizeId(name);
        return Optional.ofNullable(labels.get(labelNorm));
    }
}
