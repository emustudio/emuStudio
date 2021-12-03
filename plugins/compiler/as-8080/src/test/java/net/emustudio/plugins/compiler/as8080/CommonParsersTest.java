package net.emustudio.plugins.compiler.as8080;

import org.antlr.v4.runtime.Token;
import org.junit.Test;

import java.util.List;

import static net.emustudio.plugins.compiler.as8080.Utils.assertTokenTypes;
import static net.emustudio.plugins.compiler.as8080.Utils.getTokens;
import static org.junit.Assert.assertEquals;

public class CommonParsersTest {

    @Test
    public void testParseLitString() {
        List<Token> tokens = getTokens("'te\"x\"t1' \"te'x't2\"");
        assertTokenTypes(tokens, As8080Parser.LIT_STRING_1, As8080Parser.LIT_STRING_2, As8080Parser.EOF);
        assertEquals("te\"x\"t1", CommonParsers.parseLitString(tokens.get(0)));
        assertEquals("te'x't2", CommonParsers.parseLitString(tokens.get(1)));
    }

    @Test
    public void testParseLitHex1() {
        List<Token> tokens = getTokens("-0x22F 0XAA55");
        assertTokenTypes(tokens, As8080Parser.LIT_HEXNUMBER_1, As8080Parser.LIT_HEXNUMBER_1, As8080Parser.EOF);
        assertEquals(-0x22F, CommonParsers.parseLitHex1(tokens.get(0)));
        assertEquals(0xAA55, CommonParsers.parseLitHex1(tokens.get(1)));
    }

    @Test
    public void testParseLitHex2() {
        List<Token> tokens = getTokens("-022Fh AA55H");
        assertTokenTypes(tokens, As8080Parser.LIT_HEXNUMBER_2, As8080Parser.LIT_HEXNUMBER_2, As8080Parser.EOF);
        assertEquals(-0x22F, CommonParsers.parseLitHex2(tokens.get(0)));
        assertEquals(0xAA55, CommonParsers.parseLitHex2(tokens.get(1)));
    }

    @Test
    public void testParseLitOct() {
        List<Token> tokens = getTokens("-22q 55O 77Q -001o");
        assertTokenTypes(
            tokens,
            As8080Parser.LIT_OCTNUMBER, As8080Parser.LIT_OCTNUMBER, As8080Parser.LIT_OCTNUMBER,
            As8080Parser.LIT_OCTNUMBER, As8080Parser.EOF
        );
        assertEquals(-18, CommonParsers.parseLitOct(tokens.get(0)));
        assertEquals(45, CommonParsers.parseLitOct(tokens.get(1)));
        assertEquals(63, CommonParsers.parseLitOct(tokens.get(2)));
        assertEquals(-1, CommonParsers.parseLitOct(tokens.get(3)));
    }

    @Test
    public void testParseLitDec() {
        List<Token> tokens = getTokens("-22 55 00");
        assertTokenTypes(
            tokens,
            As8080Parser.LIT_NUMBER, As8080Parser.LIT_NUMBER, As8080Parser.LIT_NUMBER, As8080Parser.EOF
        );
        assertEquals(-22, CommonParsers.parseLitDec(tokens.get(0)));
        assertEquals(55, CommonParsers.parseLitDec(tokens.get(1)));
        assertEquals(0, CommonParsers.parseLitDec(tokens.get(2)));
    }

    @Test
    public void testParseLitBin() {
        List<Token> tokens = getTokens("000b 0101101b 111b");
        assertTokenTypes(
            tokens,
            As8080Parser.LIT_BINNUMBER, As8080Parser.LIT_BINNUMBER, As8080Parser.LIT_BINNUMBER, As8080Parser.EOF
        );
        assertEquals(0, CommonParsers.parseLitBin(tokens.get(0)));
        assertEquals(45, CommonParsers.parseLitBin(tokens.get(1)));
        assertEquals(7, CommonParsers.parseLitBin(tokens.get(2)));
    }
}
