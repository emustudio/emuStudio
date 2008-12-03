/*
 * Pseudo.java
 *
 * Created on Štvrtok, 2008, august 14, 9:03
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80Abstract;


/**
 *
 * @author vbmacher
 */
/*
 * pseudocodes var,equ
 * are treated as local if theyre set in a macro. (see Label class)
 */
public abstract class Pseudo extends Statement {

    public Pseudo(int line, int column) {
        super(line,column);
    }
    
    public boolean isPseudo() { return true; }
}
