/*
 * CodeNode.java
 *
 * Created on Piatok, 2007, september 21, 11:09
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080Abstract;

import tree8080.*;

/**
 *
 * @author vbmacher
 */

public abstract class CodeNode extends CodePseudoNode {

    /** Creates a new instance of CodeNode */
    public CodeNode(int line, int column) {
        super(line, column);
    }

    public boolean isPseudo() { return false; }
}
