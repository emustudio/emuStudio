/*
 * InstrData.java
 *
 * Created on Piatok, 2007, september 21, 11:09
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package as_z80.treeZ80Abstract;

/**
 *
 * @author vbmacher
 */

public abstract class InstrData extends Statement {

    /** Creates a new instance of InstrData */
    public InstrData(int line, int column) {
        super(line, column);
    }

    public boolean isPseudo() { return false; }
}
