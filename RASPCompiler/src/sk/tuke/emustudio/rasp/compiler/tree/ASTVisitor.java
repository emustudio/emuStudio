/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler.tree;

import sk.tuke.emustudio.rasp.memory.RASPInstructionImpl;

/**
 *
 * @author miso
 */
 public interface ASTVisitor {
     public void visit(RASPInstructionImpl instruction) throws Exception;
}
