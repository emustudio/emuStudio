/*
 * emu8Lexer.java
 *
 * Created on Nedeľa, 2007, august 12, 13:29
 *
 * KEEP IT SIMPLE STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * This interface is implemented by all lexer analysators
 *
 */
package plugins.compiler;

/**
 *
 * @author vbmacher
 */
public interface ILexer {
    public IToken getSymbol() throws java.io.IOException ;

    /**
     * @param reader The new input.
     * @param yyline The line number of the first token.
     * @param yychar The position (relative to the start of the stream) of the first token.
     * @param yycolumn The position (relative to the line) of the first token.
     * @throws IOException if an IOExecption occurs while switching readers.
     */
    public void reset(java.io.Reader reader, int yyline, int yychar, int yycolumn) throws java.io.IOException; 
    public void reset(); 
}
