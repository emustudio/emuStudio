/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import org.fife.ui.rsyntaxtextarea.Token;
import org.junit.Test;

import javax.swing.text.Segment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RTokenMakerWrapperTest {

    @Test
    public void wrapperTokenizesUsingWrappedTokenMakerImplementation() {
        RTokenMakerWrapper wrapper = new RTokenMakerWrapper(new CompilerStub(new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.RESERVED, 0, "MOV"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 3, "")
        )));

        Token token = wrapper.getTokenList(segment("MOV"), Token.NULL, 0);

        assertEquals(Token.RESERVED_WORD, token.getType());
        assertEquals("MOV", token.getLexeme());
        assertTrue(wrapper instanceof RTokenMaker);
    }

    private static Segment segment(String text) {
        return new Segment(text.toCharArray(), 0, text.length());
    }

    private static net.emustudio.emulib.plugins.compiler.Token token(int type, int offset, String text) {
        return new net.emustudio.emulib.plugins.compiler.Token() {
            @Override
            public int getType() {
                return type;
            }

            @Override
            public int getOffset() {
                return offset;
            }

            @Override
            public String getText() {
                return text;
            }
        };
    }

}
