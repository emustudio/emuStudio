/*
 * CompiledFileHandler.java
 *
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ram.compiler.impl;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.emustudio.ram.compiler.tree.Label;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

public class CompiledFileHandler {
    private List<RAMInstruction> program;

    public CompiledFileHandler() {
        program = new ArrayList<RAMInstruction>();
    }

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
    public boolean loadIntoMemory(RAMMemoryContext mem) {
        RAMMemoryContext rmem = mem;
        // load labels
        for (Label label : CompilerEnvironment.getLabels()) {
            rmem.addLabel(label.getAddress(), label.getValue());
        }

        // load input tape
        rmem.addInputs(CompilerEnvironment.getInputs());

        // load program
        for (int i = 0; i < program.size(); i++) {
            rmem.write(i, program.get(i));
        }

        return true;
    }

    public boolean serialize(String filename) {
        try {
            OutputStream file = new FileOutputStream(filename);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);

            Map<Integer, String> labels = new HashMap<Integer, String>();
            for (Label label : CompilerEnvironment.getLabels()) {
                labels.put(label.getAddress(), label.getValue());
            }
            output.writeObject(labels);
            output.writeObject(CompilerEnvironment.getInputs());
            output.writeObject(program);

            output.close();
            CompilerEnvironment.clear();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
