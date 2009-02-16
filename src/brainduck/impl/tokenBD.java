/*
 * token8080.java
 *
 * Created on Pondelok, 2007, august 20, 15:41
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package brainduck.impl;

import java_cup.runtime.Symbol;
import plugins.compiler.IToken;

/**
 *
 * @author vbmacher
 */
public class tokenBD extends Symbol implements IToken,symBD {
    public final static int ERROR_UNKNOWN_TOKEN = 0xA05;

    private String text; // hodnota tokenu
    private int row;     // číslo riadka
    private int col;     // číslo stĺpca
    private int offset;  // pozícia tokenu
    private int length;  // dĺžka tokenu
    private int type;    // typ tokenu
 
    public tokenBD(int ID, int type, String text, 
    		int line, int column, int offset, Object val) {
        super(ID,val);
        this.type = type;
        this.text = text;
        this.row = line;
        this.col = column;
        this.offset = offset;
        this.length = (text==null)?0:text.length();
    }

    public int getID() { return super.sym; }
    public int getType() { return type; }

    public String getText() { return text; }
    public String getErrorString() {
        switch (super.sym) {
            case ERROR_UNKNOWN_TOKEN: return "Unknown token";
        }
        return "";
    }
    public int getLine() { return row; }
    public int getColumn() { return col; }
    public int getOffset() { return offset; }
    public int getLength() { return length; }
}
