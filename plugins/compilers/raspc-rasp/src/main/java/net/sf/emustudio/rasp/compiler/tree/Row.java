/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.emustudio.rasp.compiler.tree;

import net.sf.emustudio.rasp.compiler.Statement;

/**
 *
 * @author miso
 */
public class Row extends AbstractTreeNode {

    private final Label label;
    private final Statement statement;

    public Row(Label label, Statement statement) {
        this.statement = statement;
        this.label = label;
    }

    public Label getLabel() {
        return label;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public void pass() throws Exception {
        /*pass() only for statement, label was already passed in translateLabels()
         method in Program node
         */
        statement.pass();
    }

}
