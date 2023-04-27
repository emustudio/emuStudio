/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
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

package net.emustudio.plugins.memory.rasp.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.memory.rasp.gui.Disassembler;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Context of the RASP memory.
 */
@ThreadSafe
@PluginContext
@SuppressWarnings("unused")
public interface RaspMemoryContext extends MemoryContext<Integer> {

    Optional<RaspLabel> getLabel(int address);

    void setLabels(List<RaspLabel> labels);

    void setInputs(List<Integer> inputs);

    RaspMemory getSnapshot();

    @Override
    default Class<Integer> getCellTypeClass() {
        return Integer.class;
    }

    default boolean isInstruction(int opcode) {
        return Disassembler.isInstruction(opcode);
    }

    default Optional<String> disassembleMnemo(int opcode) {
        return Disassembler.disassembleMnemo(opcode);
    }

    static void serialize(Path filename, int programLocation, RaspMemory memory) throws IOException {
        Map<Integer, String> labels = new HashMap<>();
        for (RaspLabel label : memory.labels) {
            labels.put(label.getAddress(), label.getLabel());
        }

        OutputStream file = new FileOutputStream(filename.toFile());
        OutputStream buffer = new BufferedOutputStream(file);
        try (ObjectOutput output = new ObjectOutputStream(buffer)) {
            output.writeObject(programLocation);
            output.writeObject(labels);
            output.writeObject(memory.inputs);
            output.writeObject(memory.programMemory);
        }
    }

    @Immutable
    class RaspMemory {
        public final List<RaspLabel> labels;
        public final Map<Integer, Integer> programMemory;
        public final List<Integer> inputs;

        public RaspMemory(Collection<? extends RaspLabel> labels,
                          Map<Integer, Integer> programMemory,
                          List<Integer> inputs) {
            this.labels = List.copyOf(labels);
            this.programMemory = Map.copyOf(programMemory);
            this.inputs = List.copyOf(inputs);
        }
    }
}
