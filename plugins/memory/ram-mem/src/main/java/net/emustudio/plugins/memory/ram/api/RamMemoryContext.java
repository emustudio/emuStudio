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
package net.emustudio.plugins.memory.ram.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

@ThreadSafe
@PluginContext
public interface RamMemoryContext extends MemoryContext<RamInstruction> {

    @Override
    default Class<RamInstruction> getCellTypeClass() {
        return RamInstruction.class;
    }

    Optional<RamLabel> getLabel(int address);

    void setLabels(List<RamLabel> labels);

    void setInputs(List<RamValue> inputs);

    RamMemory getSnapshot();

    static void serialize(Path filename, RamMemory memory) throws IOException {
        Map<Integer, String> labels = new HashMap<>();
        for (RamLabel label : memory.labels) {
            labels.put(label.getAddress(), label.getLabel());
        }

        OutputStream file = new FileOutputStream(filename.toFile());
        OutputStream buffer = new BufferedOutputStream(file);
        try (ObjectOutput output = new ObjectOutputStream(buffer)) {
            output.writeObject(labels);
            output.writeObject(memory.inputs);
            output.writeObject(memory.programMemory);
        }
    }

    @Immutable
    class RamMemory {
        public final List<RamLabel> labels;
        public final Map<Integer, RamInstruction> programMemory;
        public final List<RamValue> inputs;

        public RamMemory(Collection<? extends RamLabel> labels,
                         Map<Integer, RamInstruction> programMemory,
                         List<? extends RamValue> inputs) {
            this.labels = List.copyOf(labels);
            this.programMemory = Map.copyOf(programMemory);
            this.inputs = List.copyOf(inputs);
        }
    }
}
