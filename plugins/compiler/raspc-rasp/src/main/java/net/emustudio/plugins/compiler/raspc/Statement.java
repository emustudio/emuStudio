/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.raspc;

import net.emustudio.plugins.compiler.raspc.tree.AbstractTreeNode;
import net.emustudio.plugins.memory.rasp.InstructionImpl;
import net.emustudio.plugins.memory.rasp.NumberMemoryItem;

public class Statement extends AbstractTreeNode {

    private final InstructionImpl instruction;
    private final Integer operand;
    private final String labelOperand;

    public Statement(InstructionImpl instruction, Integer operand) {
        this.instruction = instruction;
        this.operand = operand;
        this.labelOperand = null;
    }

    public Statement(InstructionImpl instruction, String labelOperand) {
        this.instruction = instruction;
        this.labelOperand = labelOperand.toUpperCase();
        this.operand = null;
    }

    public InstructionImpl getInstruction() {
        return instruction;
    }

    public Integer getOperand() {
        return operand;
    }

    public String getLabelOperand() {
        return labelOperand;
    }

    @Override
    public void pass() throws Exception {
        //add instruction
        CompilerOutput.getInstance().addMemoryItem(instruction);
        if (operand != null) {
            CompilerOutput.getInstance().addMemoryItem(new NumberMemoryItem(operand));
        } else if (labelOperand != null) {
            //operand is label, so we are working with jump instructions
            int address = CompilerOutput.getInstance().getAddressForLabel(labelOperand);
            CompilerOutput.getInstance().addMemoryItem(new NumberMemoryItem(address));
        }
    }

}
