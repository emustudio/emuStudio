/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.brainduck.brainc.tree;

import emulib.runtime.HEXFileManager;
import java.util.ArrayList;
import java.util.List;

public class Program {

    private List<Instruction> instructions;

    public Program() {
        instructions = new ArrayList<>();
    }

    public void add(Instruction instruction) {
        if (instruction != null) {
            instructions.add(instruction);
        }
    }

    public int firstPass(int addressStart) throws Exception {
        for (Instruction instruction : instructions) {
            addressStart = instruction.firstPass(addressStart);
        }
        return addressStart;
    }

    public void secondPass(HEXFileManager hex) throws Exception {
        for (Instruction instruction : instructions) {
            instruction.secondPass(hex);
        }
    }
}
