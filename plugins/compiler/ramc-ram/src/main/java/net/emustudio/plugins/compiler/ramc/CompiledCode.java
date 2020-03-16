/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ramc;

import net.emustudio.plugins.compiler.ramc.tree.Label;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompiledCode {
    private final List<RAMInstruction> program = new ArrayList<>();

    public void addCode(RAMInstruction code) {
        program.add(code);
    }

    /**
     * Method is similar to generateHex() method in that way, that compiled
     * program is also transformed into chunk of bytes, but not to hex file but
     * to the operating memory.
     *
     * @param mem context of operating memory
     */
    public void loadIntoMemory(RAMMemoryContext mem) {
        // load labels
        for (Label label : Namespace.getLabels()) {
            mem.addLabel(label.getAddress(), label.getValue());
        }

        // load input tape
        mem.addInputs(Namespace.getInputs());

        // load program
        for (int i = 0; i < program.size(); i++) {
            mem.write(i, program.get(i));
        }
    }

    public boolean serialize(String filename) {
        try {
            OutputStream file = new FileOutputStream(filename);
            OutputStream buffer = new BufferedOutputStream(file);
            try (ObjectOutput output = new ObjectOutputStream(buffer)) {
                Map<Integer, String> labels = new HashMap<>();
                for (Label label : Namespace.getLabels()) {
                    labels.put(label.getAddress(), label.getValue());
                }
                output.writeObject(labels);
                output.writeObject(Namespace.getInputs());
                output.writeObject(program);
            }
            Namespace.clear();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
