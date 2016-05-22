/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler.tree;

/**
 *
 * @author miso
 */
public class Row implements AbstractTreeNode {

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
    public void pass() {
        /*pass() only for statement, label was already passed in translateLabels()
         method in Program node
         */
        statement.pass();
    }

}
