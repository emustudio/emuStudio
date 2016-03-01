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
public class Code implements ASTNode{

    private final int programStart;
    private final Program program;

    public Code(int programStart, Program program) {
        this.programStart = programStart;
        this.program = program;
    }

    @Override
    public void accept(ASTVisitor visitor) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
