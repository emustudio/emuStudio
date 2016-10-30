/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.assembler;

import emulib.plugins.compiler.Token;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class LexerTest {

    LexerImpl lexer(String tokens) {
        return new LexerImpl(new StringReader(tokens));
    }

    @Test
    public void testNumberUpperBoundary() throws Exception {
        LexerImpl lexer = lexer("31");

        TokenImpl token = lexer.next_token();
        assertEquals(Token.LITERAL, token.getType());
        assertEquals(TokenImpl.NUMBER, token.getID());
        assertEquals(31, token.value);
    }

    @Test
    public void testNumberLowerBoundary() throws Exception {
        LexerImpl lexer = lexer("0");

        TokenImpl token = lexer.next_token();
        assertEquals(Token.LITERAL, token.getType());
        assertEquals(TokenImpl.NUMBER, token.getID());
        assertEquals(0, token.value);
    }

    @Test
    public void testNumber() throws Exception {
        LexerImpl lexer = lexer("22");

        TokenImpl token = lexer.next_token();
        assertEquals(Token.LITERAL, token.getType());
        assertEquals(TokenImpl.NUMBER, token.getID());
        assertEquals(22, token.value);
    }

    private void checkInstruction(int id, LexerImpl lexer) throws IOException {
        TokenImpl token = lexer.next_token();
        assertEquals(Token.RESERVED, token.getType());
        assertEquals(id, token.getID());
    }

    private void checkInstructionWithOperand(int id, LexerImpl lexer) throws IOException {
        checkInstruction(id, lexer);

        TokenImpl token = lexer.next_token();
        assertEquals(Token.LITERAL, token.getType());
        assertEquals(TokenImpl.NUMBER, token.getID());
    }

    @Test
    public void testInstructionsWithOperand() throws Exception {
        checkInstructionWithOperand(TokenImpl.JMP, lexer("jmp 12"));
        checkInstructionWithOperand(TokenImpl.JRP, lexer("jrp 12"));
        checkInstructionWithOperand(TokenImpl.LDN, lexer("ldn 12"));
        checkInstructionWithOperand(TokenImpl.STO, lexer("sto 12"));
        checkInstructionWithOperand(TokenImpl.SUB, lexer("sub 12"));
    }

    @Test
    public void testInstructionsWithoutOperand() throws Exception {
        checkInstruction(TokenImpl.CMP, lexer("cmp"));
        checkInstruction(TokenImpl.STP, lexer("stp"));
    }

    @Test
    public void testInstructionInComment() throws Exception {
        LexerImpl lexer = lexer("// cmp");
        TokenImpl token = lexer.next_token();

        assertEquals(TokenImpl.TCOMMENT, token.getID());
        assertEquals(Token.COMMENT, token.getType());

        token = lexer.next_token();
        assertEquals(Token.TEOF, token.getType());
        assertEquals(TokenImpl.EOF, token.getID());
    }
}
