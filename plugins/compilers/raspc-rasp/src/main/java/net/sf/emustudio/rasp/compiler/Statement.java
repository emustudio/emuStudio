/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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

package net.sf.emustudio.rasp.compiler;

import net.sf.emustudio.rasp.compiler.tree.AbstractTreeNode;
import net.sf.emustudio.rasp.memory.memoryitems.NumberMemoryItem;
import net.sf.emustudio.rasp.memory.memoryitems.RASPInstructionImpl;

/**
 *
 * @author miso
 */
public class Statement extends AbstractTreeNode{

    private final RASPInstructionImpl instruction;
    private final Integer operand;
    private final String labelOperand;

    public Statement(RASPInstructionImpl instruction, Integer operand) {
        this.instruction = instruction;
        this.operand = operand;
        this.labelOperand = null;
    }

    public Statement(RASPInstructionImpl instruction, String labelOperand) {
        this.instruction = instruction;
        this.labelOperand = labelOperand.toUpperCase();
        this.operand = null;
    }

    public RASPInstructionImpl getInstruction() {
        return instruction;
    }

    public Integer getOperand() {
        return operand;
    }

    public String getLabelOperand() {
        return labelOperand;
    }

    @Override
    public void pass() throws Exception{
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
