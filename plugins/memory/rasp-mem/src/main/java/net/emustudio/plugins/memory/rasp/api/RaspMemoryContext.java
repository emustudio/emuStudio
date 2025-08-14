/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
