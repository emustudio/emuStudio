/**
 * CompiledFileHandler.java
 *
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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
package ramc_ram.compiled;

import interfaces.C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E;
import interfaces.C8E258161A30C508D5E8ED07CE943EEF7408CA508;

import java.util.ArrayList;

import ramc_ram.tree.Label;

/**
 *
 * @author vbmacher
 */
public class CompiledFileHandler {

    private ArrayList<C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E> program;

    public CompiledFileHandler() {
        program = new ArrayList<C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E>();
    }

    public void addCode(C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E code) {
        program.add(code);
    }

    /**
     * Method is similar to generateHex() method in that way, that
     * compiled program is also transformed into chunk of bytes, but
     * not to hex file but to the operating memory.
     * 
     * @param mem context of operating memory
     */
    public boolean loadIntoMemory(C8E258161A30C508D5E8ED07CE943EEF7408CA508 mem) {
        C8E258161A30C508D5E8ED07CE943EEF7408CA508 rmem = mem;
        // load labels
        Label[] labels = CompilerEnvironment.getLabels();
        for (int i = 0; i < labels.length; i++) {
            rmem.addLabel(labels[i].getAddress(), labels[i].getValue());
        }

        // load input tape
        rmem.addInputs(CompilerEnvironment.getInputs());

        // load program
        for (int i = 0; i < program.size(); i++) {
            rmem.write(i, program.get(i));
        }

        // TODO: beware!
        CompilerEnvironment.clear();
        return true;
    }
}
