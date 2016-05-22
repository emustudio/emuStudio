/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler.tree;

import java.util.ArrayList;
import java.util.List;
import sk.tuke.emustudio.rasp.compiler.CompilerOutput;

/**
 *
 * @author miso
 */
public class Program implements AbstractTreeNode{

    private final List<Row> rows = new ArrayList<>();

    public void addRow(Row r) {
        rows.add(r);
    }

    @Override
    public void pass() {
        translateLabels();

        for (Row row : rows) {
            row.pass();
        }
    }

    private void translateLabels() {
        int programStart = CompilerOutput.getInstance().getProgramStart();
        for (Row row : rows) {
            Label label = row.getLabel();
            if (label != null) {
                label.setAddress(programStart + rows.indexOf(row) * 2);      
                label.pass();
            }
        }
    }

}
