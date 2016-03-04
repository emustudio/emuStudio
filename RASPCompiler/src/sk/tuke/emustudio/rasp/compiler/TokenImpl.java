/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler;

import emulib.plugins.compiler.Token;
import java_cup.runtime.ComplexSymbolFactory;

/**
 *
 * @author miso
 */
public class TokenImpl extends ComplexSymbolFactory.ComplexSymbol implements Token, Symbols {

    /**
     * The type of the Token (RESERVED, PREPROCESSOR...)
     *
     */
    private final int type;

    /**
     * 0-based starting offset of token position
     */
    private final int offset;

    public TokenImpl(int id, int type, String text, int line, int column, int offset) {
        super(text, id, new ComplexSymbolFactory.Location(line, column), new ComplexSymbolFactory.Location(line, column));
        this.type = type;
        this.offset = offset;
    }

    public TokenImpl(int id, int type, String text, int line, int column, int offset, Object value) {
        super(text, id, new ComplexSymbolFactory.Location(line, column), new ComplexSymbolFactory.Location(line, column), value);
        this.type = type;
        this.offset = offset;
    }

    @Override
    public int getID() {
        return super.sym;
    }

    @Override
    public int getType() {
        return this.type;
    }

    @Override
    public int getLine() {
        return super.getLeft().getLine();
    }

    @Override
    public int getColumn() {
        return super.getLeft().getColumn();
    }

    @Override
    public int getOffset() {
        return this.offset;
    }

    @Override
    public int getLength() {
        return getName().length();
    }

    @Override
    public String getErrorString() {
        return "Unknown token";
    }

    @Override
    public String getText() {
        return getName();
    }

    @Override
    public boolean isInitialLexicalState() {
        return true;
    }

}
