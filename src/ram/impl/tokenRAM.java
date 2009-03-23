/*
 * tokenRAM.java
 *
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package ram.impl;

import java_cup.runtime.Symbol;
import plugins.compiler.IToken;

/**
 *
 * @author vbmacher
 */
public class tokenRAM extends Symbol implements IToken,symRAM {
    public final static int ERROR_UNKNOWN_TOKEN = 0xA05;

    private String text; // hodnota tokenu
    private int row;     // číslo riadka
    private int col;     // číslo stĺpca
    private int offset;  // pozícia tokenu
    private int length;  // dĺžka tokenu
    private int type;    // typ tokenu
 
    public tokenRAM(int ID, int type, String text, 
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
