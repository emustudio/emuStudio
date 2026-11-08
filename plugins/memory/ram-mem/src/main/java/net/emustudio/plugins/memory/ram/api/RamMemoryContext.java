/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.api;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.jcip.annotations.ThreadSafe;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

@ThreadSafe
public interface RamMemoryContext extends MemoryContext<RamInstruction> {

    @Override
    default Class<RamInstruction> getCellTypeClass() {
        return RamInstruction.class;
    }

    Optional<RamLabel> getLabel(int address);

    void setLabels(List<RamLabel> labels);

    void setInputs(List<RamValue> inputs);

    /** Immutable list of the program's configured input values. */
    List<RamValue> getInputs();

    static void serialize(Path filename, Collection<? extends RamLabel> programLabels,
                          Map<Integer, RamInstruction> programMemory, List<? extends RamValue> inputs) throws IOException {
        Map<Integer, String> labels = new HashMap<>();
        for (RamLabel label : programLabels) {
            labels.put(label.getAddress(), label.getLabel());
        }

        OutputStream file = new FileOutputStream(filename.toFile());
        OutputStream buffer = new BufferedOutputStream(file);
        try (ObjectOutput output = new ObjectOutputStream(buffer)) {
            output.writeObject(labels);
            output.writeObject(inputs);
            output.writeObject(programMemory);
        }
    }

}
