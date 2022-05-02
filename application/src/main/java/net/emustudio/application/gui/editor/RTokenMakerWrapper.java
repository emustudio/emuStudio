package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.Compiler;
import org.fife.ui.rsyntaxtextarea.OccurrenceMarker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;

import javax.swing.*;
import javax.swing.text.Segment;

public class RTokenMakerWrapper implements TokenMaker {
    private static RTokenMaker WRAPPED;

    public RTokenMakerWrapper(Compiler compiler) {
        WRAPPED = new RTokenMaker(compiler);
    }

    @SuppressWarnings("unused")
    public RTokenMakerWrapper() {

    }

    @Override
    public void addNullToken() {
        WRAPPED.addNullToken();
    }

    @Override
    public void addToken(char[] array, int start, int end, int tokenType, int startOffset) {
        WRAPPED.addToken(array, start, end, tokenType, startOffset);
    }

    @Override
    public int getClosestStandardTokenTypeForInternalType(int type) {
        return WRAPPED.getClosestStandardTokenTypeForInternalType(type);
    }

    @Override
    public boolean getCurlyBracesDenoteCodeBlocks(int languageIndex) {
        return WRAPPED.getCurlyBracesDenoteCodeBlocks(languageIndex);
    }

    @Override
    public int getLastTokenTypeOnLine(Segment text, int initialTokenType) {
        return WRAPPED.getLastTokenTypeOnLine(text, initialTokenType);
    }

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return WRAPPED.getLineCommentStartAndEnd(languageIndex);
    }

    @Override
    public Action getInsertBreakAction() {
        return WRAPPED.getInsertBreakAction();
    }

    @Override
    public boolean getMarkOccurrencesOfTokenType(int type) {
        return WRAPPED.getMarkOccurrencesOfTokenType(type);
    }

    @Override
    public OccurrenceMarker getOccurrenceMarker() {
        return WRAPPED.getOccurrenceMarker();
    }

    @Override
    public boolean getShouldIndentNextLineAfter(Token token) {
        return WRAPPED.getShouldIndentNextLineAfter(token);
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        return WRAPPED.getTokenList(text, initialTokenType, startOffset);
    }

    @Override
    public boolean isIdentifierChar(int languageIndex, char ch) {
        return WRAPPED.isIdentifierChar(languageIndex, ch);
    }

    @Override
    public boolean isMarkupLanguage() {
        return WRAPPED.isMarkupLanguage();
    }
}
