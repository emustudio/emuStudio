/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.api;

import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.plugins.memory.rasp.gui.Disassembler;
import net.jcip.annotations.ThreadSafe;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Context of the RASP memory.
 */
@ThreadSafe
@SuppressWarnings("unused")
public interface RaspMemoryContext extends MemoryContext<Integer> {

    Optional<RaspLabel> getLabel(int address);

    void setLabels(List<RaspLabel> labels);

    void setInputs(List<Integer> inputs);

    /** Immutable list of the program's configured input values. */
    List<Integer> getInputs();

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

    static void serialize(Path filename, int programLocation, Collection<? extends RaspLabel> programLabels,
                          Map<Integer, Integer> programMemory, List<Integer> inputs) throws IOException {
        Map<Integer, String> labels = new HashMap<>();
        for (RaspLabel label : programLabels) {
            labels.put(label.getAddress(), label.getLabel());
        }

        OutputStream file = new FileOutputStream(filename.toFile());
        OutputStream buffer = new BufferedOutputStream(file);
        try (ObjectOutput output = new ObjectOutputStream(buffer)) {
            output.writeObject(programLocation);
            output.writeObject(labels);
            output.writeObject(inputs);
            output.writeObject(programMemory);
        }
    }

}
