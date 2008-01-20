/*
 * OpCode.java
 *
 * Created on Sobota, 2007, september 22, 9:15
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080Abstract;

/**
 *
 * @author vbmacher
 */
public abstract class OpCodeNode extends CodeNode {
    protected String mnemo;
    
    protected String getRegMnemo(byte reg) {
        switch (reg) {
            case 0: return "b";
            case 1: return "c";
            case 2: return "d";
            case 3: return "e";
            case 4: return "h";
            case 5: return "l";
            case 6: return "m";
            case 7: return "a";
        }
        return "";
    }
    
    protected String getRegpairMnemo(byte regpair, boolean psw) {
        switch (regpair) {
            case 0: return "bc";
            case 1: return "de";
            case 2: return "hl";
            case 3: if (psw == false) return "sp";
                    else return "psw";
        }
        return "";
    }
    
    /** Creates a new instance of OpCode */
    public OpCodeNode(String mnemo, int line, int column) {
        super(line,column);
        this.mnemo = mnemo;
    }
    
}
