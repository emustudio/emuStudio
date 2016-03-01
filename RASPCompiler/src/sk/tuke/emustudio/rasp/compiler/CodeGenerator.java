/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler;

import sk.tuke.emustudio.rasp.compiler.tree.ASTVisitor;
import sk.tuke.emustudio.rasp.memory.RASPInstructionImpl;

/**
 *
 * @author miso
 */
public class CodeGenerator implements ASTVisitor{

    @Override
    public void visit(RASPInstructionImpl instruction) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
