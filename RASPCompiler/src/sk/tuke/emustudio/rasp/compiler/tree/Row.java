/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler.tree;

import sk.tuke.emustudio.rasp.compiler.CompilerOutput;

/**
 *
 * @author miso
 */
public class Row {

    private Label label;
    private Statement statement;

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


}
