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
public interface ASTNode {
    public void accept(ASTVisitor visitor) throws Exception;
}
