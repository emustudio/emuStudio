/*
 * NeedMorePassException.java
 *
 * Created on Štvrtok, 2007, október 11, 11:28
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * this exception is throwed while compiling forward references that are in
 * expressions. Expression with forward reference for label can't be evaulated
 * without knowing a value of the label (its address that label is pointing at).
 * 
 */

package compiler8080;

/**
 *
 * @author vbmacher
 */
public class NeedMorePassException extends Exception {
    private Object obj;
    private int line;
    private int column;
    
    /** Creates a new instance of NeedMorePassException */
    public NeedMorePassException(Object o, int line, int column) {
        this.obj = o;
        this.line = line;
        this.column = column;
    }
    
    public Object getObject() { return obj; }
    public int getLine() { return this.line; }
    public int getColumn() { return this.column; }
}
