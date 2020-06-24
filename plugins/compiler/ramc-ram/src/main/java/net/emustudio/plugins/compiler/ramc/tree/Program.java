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
package net.emustudio.plugins.compiler.ramc.tree;

import net.emustudio.plugins.compiler.ramc.CompiledCode;

import java.util.ArrayList;
import java.util.List;

public class Program {
    private final List<Row> instructionsList = new ArrayList<>();

    public void addRow(Row node) {
        if (node != null) {
            instructionsList.add(node);
        }
    }

    public void pass1(int addr_start) {
        int curr_addr = addr_start;
        for (Row anInstructionsList : instructionsList) {
            curr_addr = anInstructionsList.pass1(curr_addr);
        }
    }

    public void pass2(CompiledCode hex) throws Exception {
        for (Row anInstructionsList : instructionsList) {
            anInstructionsList.pass2(hex);
        }
    }
}
