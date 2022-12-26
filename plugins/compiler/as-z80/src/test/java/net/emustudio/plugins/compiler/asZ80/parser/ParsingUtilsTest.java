/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.asZ80.parser;

import net.emustudio.plugins.compiler.asZ80.ParsingUtils;
import net.emustudio.plugins.compiler.asZ80.Utils;
import org.antlr.v4.runtime.Token;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTokenTypes;
import static net.emustudio.plugins.compiler.asZ80.Utils.getTokens;
import static org.junit.Assert.assertEquals;

public class ParsingUtilsTest {

    @Test
    public void testParseLitString() {
        List<Token> tokens = getTokens("'te\"x\"t1' \"te'x't2\"");
        Utils.assertTokenTypes(tokens, LIT_STRING_1, WS, LIT_STRING_2, EOF);
        Assert.assertEquals("te\"x\"t1", ParsingUtils.parseLitString(tokens.get(0)));
        assertEquals("te'x't2", ParsingUtils.parseLitString(tokens.get(2)));
    }

    @Test
    public void testParseLitHex1() {
        List<Token> tokens = getTokens("0x22F 0XAA55");
        assertTokenTypes(tokens, LIT_HEXNUMBER_1, WS, LIT_HEXNUMBER_1, EOF);
        assertEquals(0x22F, ParsingUtils.parseLitHex1(tokens.get(0)));
        assertEquals(0xAA55, ParsingUtils.parseLitHex1(tokens.get(2)));
    }

    @Test
    public void testParseLitHex2() {
        List<Token> tokens = getTokens("022Fh AA55H");
        assertTokenTypes(tokens, LIT_HEXNUMBER_2, WS, LIT_HEXNUMBER_2, EOF);
        assertEquals(0x22F, ParsingUtils.parseLitHex2(tokens.get(0)));
        assertEquals(0xAA55, ParsingUtils.parseLitHex2(tokens.get(2)));
    }

    @Test
    public void testParseLitOct() {
        List<Token> tokens = getTokens("22q 55O 77Q 001o");
        assertTokenTypes(
                tokens,
                LIT_OCTNUMBER, WS, LIT_OCTNUMBER, WS, LIT_OCTNUMBER, WS, LIT_OCTNUMBER, EOF
        );
        assertEquals(18, ParsingUtils.parseLitOct(tokens.get(0)));
        assertEquals(45, ParsingUtils.parseLitOct(tokens.get(2)));
        assertEquals(63, ParsingUtils.parseLitOct(tokens.get(4)));
        assertEquals(1, ParsingUtils.parseLitOct(tokens.get(6)));
    }

    @Test
    public void testParseLitDec() {
        List<Token> tokens = getTokens("22 55 00");
        assertTokenTypes(
                tokens,
                LIT_NUMBER, WS, LIT_NUMBER, WS, LIT_NUMBER, EOF
        );
        assertEquals(22, ParsingUtils.parseLitDec(tokens.get(0)));
        assertEquals(55, ParsingUtils.parseLitDec(tokens.get(2)));
        assertEquals(0, ParsingUtils.parseLitDec(tokens.get(4)));
    }

    @Test
    public void testParseLitBin() {
        List<Token> tokens = getTokens("000b 0101101b 111b");
        assertTokenTypes(
                tokens,
                LIT_BINNUMBER, WS, LIT_BINNUMBER, WS, LIT_BINNUMBER, EOF
        );
        assertEquals(0, ParsingUtils.parseLitBin(tokens.get(0)));
        assertEquals(45, ParsingUtils.parseLitBin(tokens.get(2)));
        assertEquals(7, ParsingUtils.parseLitBin(tokens.get(4)));
    }
}
