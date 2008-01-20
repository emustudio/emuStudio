/*
 * IToken.java
 *
 * Created on Nedeľa, 2007, august 12, 13:38
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 * Abstract class for a token
 *
 */
package plugins.compiler;

/**
 *
 * @author vbmacher
 */
public interface IToken {
    public final static int RESERVED = 0x100;
    public final static int PREPROCESSOR = 0x200;
    public final static int REGISTER = 0x300;
    public final static int SEPARATOR = 0x400;
    public final static int OPERATOR = 0x500;
    public final static int COMMENT = 0x600;
    public final static int LITERAL = 0x700;
    public final static int IDENTIFIER = 0x800;
    public final static int LABEL = 0x900;
    public final static int ERROR = 1;
    public final static int TEOF = 0;

    public int getID();
    public int getType();

    public String getText();
    public String getErrorString();
    
    public int getLine();
    public int getColumn();
    public int getCharBegin();
    public int getCharEnd();
}
