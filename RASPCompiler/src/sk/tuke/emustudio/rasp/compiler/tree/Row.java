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
public class Row implements ASTNode{

    private final Label label;
    private final Statement statement;

    public Row(Label label, Statement statement) {
        this.label = label;
        this.statement = statement;
    }

    public Row(Label label) {
        this(label, null);
    }

    @Override
    public void accept(ASTVisitor visitor) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
