package net.emustudio.plugins.compiler.as8080;

import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.As8080Lexer.*;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTokenTypes;

public class LexerTest {

    @Test
    public void testParseError() {
        assertTokenTypes("B &", REG_B, WS, ERROR, EOF);
        assertTokenTypes("0x 9o 22b", ERROR, WS, ERROR, WS, ERROR, EOF);
    }

    @Test
    public void testParseHexNumbers() {
        assertTokenTypes(
            "0x5F 0X5F 5fh 5fH 5Fh 0xG",
            LIT_HEXNUMBER_1, WS, LIT_HEXNUMBER_1, WS, LIT_HEXNUMBER_1, WS, LIT_HEXNUMBER_1, WS, LIT_HEXNUMBER_1, WS, ERROR, EOF
        );
        assertTokenTypes("-5h -0xFF", LIT_HEXNUMBER_1, WS, LIT_HEXNUMBER_1, EOF);
    }

//    @Test
//    public void testParseReservedWords() {
//        assertTokenTypesForCaseVariations("jmp", SSEMLexer.JMP, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("jrp", SSEMLexer.JPR, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("jpr", SSEMLexer.JPR, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("jmr", SSEMLexer.JPR, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("ldn", SSEMLexer.LDN, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("sto", SSEMLexer.STO, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("sub", SSEMLexer.SUB, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("cmp", SSEMLexer.CMP, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("skn", SSEMLexer.CMP, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("stp", SSEMLexer.STP, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("hlt", SSEMLexer.STP, SSEMLexer.EOF);
//    }
//
//    @Test
//    public void testParsePreprocessor() {
//        assertTokenTypesForCaseVariations("start", SSEMLexer.START, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("num", SSEMLexer.NUM, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("bnum", SSEMLexer.BNUM, SSEMLexer.EOF);
//        assertTokenTypesForCaseVariations("bins", SSEMLexer.BNUM, SSEMLexer.EOF);
//    }
//
//    @Test
//    public void testParseWhitespaces() {
//        assertTokenTypes(" ", SSEMLexer.WS, SSEMLexer.EOF);
//        assertTokenTypes("\t", SSEMLexer.WS, SSEMLexer.EOF);
//        assertTokenTypes("\n", SSEMLexer.EOL, SSEMLexer.EOF);
//        assertTokenTypes("", SSEMLexer.EOF);
//    }
//
//    @Test
//    public void testParseComments() {
//        assertTokenTypes("-- comment baybe", SSEMLexer.COMMENT, SSEMLexer.EOF);
//        assertTokenTypes("# comment baybe", SSEMLexer.COMMENT, SSEMLexer.EOF);
//        assertTokenTypes("// comment baybe", SSEMLexer.COMMENT, SSEMLexer.EOF);
//        assertTokenTypes("; comment baybe", SSEMLexer.COMMENT, SSEMLexer.EOF);
//    }
//
//    @Test
//    public void testLiterals() {
//        assertTokenTypes("10", SSEMLexer.NUMBER, SSEMLexer.EOF);
//        assertTokenTypes("0xAF", SSEMLexer.HEXNUMBER, SSEMLexer.EOF);
//        assertTokenTypes("BINS 1010", SSEMLexer.BNUM, SSEMLexer.BWS, SSEMLexer.BinaryNumber, SSEMLexer.EOF);
//    }
}
