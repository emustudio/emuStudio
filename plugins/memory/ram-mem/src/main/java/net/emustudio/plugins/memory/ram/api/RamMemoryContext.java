/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
