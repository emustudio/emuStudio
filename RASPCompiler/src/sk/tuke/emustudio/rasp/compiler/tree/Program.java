/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sk.tuke.emustudio.rasp.compiler.CompilerOutput;
import sk.tuke.emustudio.rasp.memory.NumberMemoryItem;
import sk.tuke.emustudio.rasp.memory.RASPInstructionImpl;

/**
 *
 * @author miso
 */
public class Program {

    private final List<Row> rows = new ArrayList<Row>();

    public void addRow(Row r) {
        rows.add(r);
    }

    public void pass() {
        Collections.reverse(rows);
        translateLabels();
        System.out.println(CompilerOutput.getInstance().getReversedLabels());

        int programStart = CompilerOutput.getInstance().getProgramStart();
        for (Row row : rows) {
            Statement statement = row.getStatement();
            RASPInstructionImpl instruction = statement.getInstruction();
            //add instruction
            CompilerOutput.getInstance().addMemoryItem(instruction);
            Integer operand = statement.getOperand();
            String labelOperand = statement.getLabelOperand();
            if (operand != null) {
                CompilerOutput.getInstance().addMemoryItem(new NumberMemoryItem(operand));
            } else if (labelOperand != null) {
                //operand is label, so we are working with jump instructions
                int address = CompilerOutput.getInstance().getAddressForLabel(labelOperand);

                CompilerOutput.getInstance().addMemoryItem(new NumberMemoryItem(address));
            }
        }
    }

    private void translateLabels() {
        int programStart = CompilerOutput.getInstance().getProgramStart();
        for (Row row : rows) {
            Label label = row.getLabel();
            if (label != null) {
                label.setAddress(programStart + rows.indexOf(row) * 2);
                CompilerOutput.getInstance().addLabel(label);
            }
        }
    }

}
