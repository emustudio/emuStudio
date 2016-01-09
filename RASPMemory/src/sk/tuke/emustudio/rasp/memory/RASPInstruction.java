/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package sk.tuke.emustudio.rasp.memory;

import emulib.plugins.compiler.CompilerContext;

/**
 *
 * @author miso
 */
public interface RASPInstruction extends CompilerContext{
    
    public final static int READ = 1;
	public final static int WRITE = 2;
	public final static int LOAD = 3;
	public final static int STORE = 4;
	public final static int ADD = 5;
	public final static int SUB = 6;
	public final static int MUL = 7;
	public final static int DIV = 8;
	public final static int JMP = 9;
	public final static int JZ = 10;
	public final static int JGTZ = 11;
	public final static int HALT = 12;
    
}
