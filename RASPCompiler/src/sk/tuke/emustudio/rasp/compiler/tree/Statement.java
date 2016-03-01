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
public class Statement implements ASTNode{

    private final RASPInstructionImpl instruction;
    private final Integer operand;
    private final String labelOperand;

    public Statement(RASPInstructionImpl instruction, Integer operand) {
        this.instruction = instruction;
        this.operand = operand;
        this.labelOperand = null;
    }

    public Statement(RASPInstructionImpl instruction, String labelOperand) {
        this.instruction = instruction;
        this.labelOperand = labelOperand;
        this.operand = null;
    }

    @Override
    public void accept(ASTVisitor visitor) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
