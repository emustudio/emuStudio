/*
 * Instruction.java
 *
 * Created on Štvrtok, 2008, august 14, 12:46
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80Abstract;

/**
 *
 * @author vbmacher
 */
public abstract class Instruction extends InstrData {
    protected int opcode;
    
    public Instruction(int opcode, int line, int column) {
        super(line,column);
        this.opcode = opcode;
    }

    public int getSize() { 
        return Expression.getSize(opcode);
    }
}
