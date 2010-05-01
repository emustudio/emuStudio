/*
 * PseudoNode.java
 *
 * Created on Piatok, 2007, september 21, 11:17
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package as_8080.tree8080Abstract;


/**
 *
 * @author vbmacher
 */
/*
 * pseudocodes like: set,equ
 * are treated as local if theyre set in a macro. (see LabelNode class)
 */
public abstract class PseudoNode extends CodePseudoNode {
    @Override
    public boolean isPseudo() { return true; }
    /** Creates a new instance of PseudoNode */
    public PseudoNode(int line, int column) {
        super(line,column);
    }
    
    public abstract String getName();
    
}
