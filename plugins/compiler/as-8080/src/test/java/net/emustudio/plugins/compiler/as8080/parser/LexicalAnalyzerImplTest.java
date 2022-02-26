package net.emustudio.plugins.compiler.as8080.parser;

import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.As8080Lexer.*;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTokenTypes;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTokenTypesIgnoreCase;

public class LexicalAnalyzerImplTest {

    @Test
    public void testParseEols() {
        assertTokenTypes("\n\n\n\n\n", EOL, EOL, EOL, EOL, EOL, EOF);
    }

    @Test
    public void testParseError1() {
        assertTokenTypes("B !", REG_B, ERROR, EOF);
    }

    @Test
    public void testParseError2() {
        assertTokenTypes("0x 9o 22b", ERROR, ERROR, ERROR, EOF);
    }

    @Test
    public void testParseHex1() {
        assertTokenTypes(
            "0x1 0x0 -0x5f -0xFffF 0x1BC",
            LIT_HEXNUMBER_1, LIT_HEXNUMBER_1, OP_SUBTRACT, LIT_HEXNUMBER_1, OP_SUBTRACT, LIT_HEXNUMBER_1, LIT_HEXNUMBER_1, EOF
        );
    }

    @Test
    public void testParseHex2() {
        assertTokenTypes(
            "1h 0h -5Fh -FFFFh 1BCh 5h -5h",
            LIT_HEXNUMBER_2, LIT_HEXNUMBER_2, OP_SUBTRACT, LIT_HEXNUMBER_2, OP_SUBTRACT, LIT_HEXNUMBER_2, LIT_HEXNUMBER_2,
            LIT_HEXNUMBER_2, OP_SUBTRACT, LIT_HEXNUMBER_2, EOF
        );
    }

    @Test
    public void testParseDecimal() {
        assertTokenTypes(
            "0 1 -2 3 -4 5 66 999",
            LIT_NUMBER, LIT_NUMBER, OP_SUBTRACT, LIT_NUMBER, LIT_NUMBER, OP_SUBTRACT, LIT_NUMBER, LIT_NUMBER, LIT_NUMBER,
            LIT_NUMBER, EOF
        );
    }

    @Test
    public void testParseOctal() {
        assertTokenTypes("-6o 7q 11q -345O", OP_SUBTRACT, LIT_OCTNUMBER, LIT_OCTNUMBER, LIT_OCTNUMBER, OP_SUBTRACT, LIT_OCTNUMBER, EOF);
    }

    @Test
    public void testParseBinary() {
        assertTokenTypes("10101010101010101010110b", LIT_BINNUMBER, EOF);
    }

    @Test
    public void testParseString1() {
        assertTokenTypes("'' 'sss'", LIT_STRING_1, LIT_STRING_1, EOF);
        assertTokenTypes("'\nsss'", LIT_STRING_1, EOF);
    }

    @Test
    public void testParseString2() {
        assertTokenTypes("\"\" \"sss\"", LIT_STRING_2, LIT_STRING_2, EOF);
        assertTokenTypes("\"\nsss\"", LIT_STRING_2, EOF);
    }

    @Test
    public void testParseString1Error() {
        assertTokenTypes("'open", ERROR, EOF);
    }

    @Test
    public void testParseString2Error() {
        assertTokenTypes("\"open", ERROR, EOF);
    }

    @Test
    public void testParseComment1() {
        assertTokenTypes("// comment fun1", EOF);
        assertTokenTypes("# comment fun1", EOF);
        assertTokenTypes("; comment fun1", EOF);
    }

    @Test
    public void testParseComment2() {
        assertTokenTypes("/*\n*\n* comment fun1\n\n*/", EOF);
    }

    @Test
    public void testParseOpcodes() {
        assertTokenTypesIgnoreCase("STC", OPCODE_STC, EOF);
        assertTokenTypesIgnoreCase("CMC", OPCODE_CMC, EOF);
        assertTokenTypesIgnoreCase("CMA", OPCODE_CMA, EOF);
        assertTokenTypesIgnoreCase("DAA", OPCODE_DAA, EOF);
        assertTokenTypesIgnoreCase("NOP", OPCODE_NOP, EOF);
        assertTokenTypesIgnoreCase("RLC", OPCODE_RLC, EOF);
        assertTokenTypesIgnoreCase("RRC", OPCODE_RRC, EOF);
        assertTokenTypesIgnoreCase("RAL", OPCODE_RAL, EOF);
        assertTokenTypesIgnoreCase("RAR", OPCODE_RAR, EOF);
        assertTokenTypesIgnoreCase("XCHG", OPCODE_XCHG, EOF);
        assertTokenTypesIgnoreCase("XTHL", OPCODE_XTHL, EOF);
        assertTokenTypesIgnoreCase("SPHL", OPCODE_SPHL, EOF);
        assertTokenTypesIgnoreCase("PCHL", OPCODE_PCHL, EOF);
        assertTokenTypesIgnoreCase("RET", OPCODE_RET, EOF);
        assertTokenTypesIgnoreCase("RC", OPCODE_RC, EOF);
        assertTokenTypesIgnoreCase("RNC", OPCODE_RNC, EOF);
        assertTokenTypesIgnoreCase("RZ", OPCODE_RZ, EOF);
        assertTokenTypesIgnoreCase("RNZ", OPCODE_RNZ, EOF);
        assertTokenTypesIgnoreCase("RM", OPCODE_RM, EOF);
        assertTokenTypesIgnoreCase("RP", OPCODE_RP, EOF);
        assertTokenTypesIgnoreCase("RPE", OPCODE_RPE, EOF);
        assertTokenTypesIgnoreCase("RPO", OPCODE_RPO, EOF);
        assertTokenTypesIgnoreCase("EI", OPCODE_EI, EOF);
        assertTokenTypesIgnoreCase("DI", OPCODE_DI, EOF);
        assertTokenTypesIgnoreCase("HLT", OPCODE_HLT, EOF);
        assertTokenTypesIgnoreCase("INR", OPCODE_INR, EOF);
        assertTokenTypesIgnoreCase("DCR", OPCODE_DCR, EOF);
        assertTokenTypesIgnoreCase("ADD", OPCODE_ADD, EOF);
        assertTokenTypesIgnoreCase("ADC", OPCODE_ADC, EOF);
        assertTokenTypesIgnoreCase("SUB", OPCODE_SUB, EOF);
        assertTokenTypesIgnoreCase("SBB", OPCODE_SBB, EOF);
        assertTokenTypesIgnoreCase("ANA", OPCODE_ANA, EOF);
        assertTokenTypesIgnoreCase("XRA", OPCODE_XRA, EOF);
        assertTokenTypesIgnoreCase("ORA", OPCODE_ORA, EOF);
        assertTokenTypesIgnoreCase("CMP", OPCODE_CMP, EOF);
        assertTokenTypesIgnoreCase("MOV", OPCODE_MOV, EOF);
        assertTokenTypesIgnoreCase("STAX", OPCODE_STAX, EOF);
        assertTokenTypesIgnoreCase("LDAX", OPCODE_LDAX, EOF);
        assertTokenTypesIgnoreCase("PUSH", OPCODE_PUSH, EOF);
        assertTokenTypesIgnoreCase("POP", OPCODE_POP, EOF);
        assertTokenTypesIgnoreCase("DAD", OPCODE_DAD, EOF);
        assertTokenTypesIgnoreCase("INX", OPCODE_INX, EOF);
        assertTokenTypesIgnoreCase("DCX", OPCODE_DCX, EOF);
        assertTokenTypesIgnoreCase("LXI", OPCODE_LXI, EOF);
        assertTokenTypesIgnoreCase("MVI", OPCODE_MVI, EOF);
        assertTokenTypesIgnoreCase("ADI", OPCODE_ADI, EOF);
        assertTokenTypesIgnoreCase("ACI", OPCODE_ACI, EOF);
        assertTokenTypesIgnoreCase("SUI", OPCODE_SUI, EOF);
        assertTokenTypesIgnoreCase("SBI", OPCODE_SBI, EOF);
        assertTokenTypesIgnoreCase("ANI", OPCODE_ANI, EOF);
        assertTokenTypesIgnoreCase("XRI", OPCODE_XRI, EOF);
        assertTokenTypesIgnoreCase("ORI", OPCODE_ORI, EOF);
        assertTokenTypesIgnoreCase("CPI", OPCODE_CPI, EOF);
        assertTokenTypesIgnoreCase("STA", OPCODE_STA, EOF);
        assertTokenTypesIgnoreCase("LDA", OPCODE_LDA, EOF);
        assertTokenTypesIgnoreCase("SHLD", OPCODE_SHLD, EOF);
        assertTokenTypesIgnoreCase("LHLD", OPCODE_LHLD, EOF);
        assertTokenTypesIgnoreCase("JMP", OPCODE_JMP, EOF);
        assertTokenTypesIgnoreCase("JC", OPCODE_JC, EOF);
        assertTokenTypesIgnoreCase("JNC", OPCODE_JNC, EOF);
        assertTokenTypesIgnoreCase("JZ", OPCODE_JZ, EOF);
        assertTokenTypesIgnoreCase("JNZ", OPCODE_JNZ, EOF);
        assertTokenTypesIgnoreCase("JM", OPCODE_JM, EOF);
        assertTokenTypesIgnoreCase("JP", OPCODE_JP, EOF);
        assertTokenTypesIgnoreCase("JPE", OPCODE_JPE, EOF);
        assertTokenTypesIgnoreCase("JPO", OPCODE_JPO, EOF);
        assertTokenTypesIgnoreCase("CALL", OPCODE_CALL, EOF);
        assertTokenTypesIgnoreCase("CC", OPCODE_CC, EOF);
        assertTokenTypesIgnoreCase("CNC", OPCODE_CNC, EOF);
        assertTokenTypesIgnoreCase("CZ", OPCODE_CZ, EOF);
        assertTokenTypesIgnoreCase("CNZ", OPCODE_CNZ, EOF);
        assertTokenTypesIgnoreCase("CM", OPCODE_CM, EOF);
        assertTokenTypesIgnoreCase("CP", OPCODE_CP, EOF);
        assertTokenTypesIgnoreCase("CPE", OPCODE_CPE, EOF);
        assertTokenTypesIgnoreCase("CPO", OPCODE_CPO, EOF);
        assertTokenTypesIgnoreCase("RST", OPCODE_RST, EOF);
        assertTokenTypesIgnoreCase("IN", OPCODE_IN, EOF);
        assertTokenTypesIgnoreCase("OUT", OPCODE_OUT, EOF);
    }

    @Test
    public void testParsePreprocessor() {
        assertTokenTypesIgnoreCase("ORG", PREP_ORG, EOF);
        assertTokenTypesIgnoreCase("EQU", PREP_EQU, EOF);
        assertTokenTypesIgnoreCase("SET", PREP_SET, EOF);
        assertTokenTypesIgnoreCase("INCLUDE", PREP_INCLUDE, EOF);
        assertTokenTypesIgnoreCase("IF", PREP_IF, EOF);
        assertTokenTypesIgnoreCase("ENDIF", PREP_ENDIF, EOF);
        assertTokenTypesIgnoreCase("MACRO", PREP_MACRO, EOF);
        assertTokenTypesIgnoreCase("ENDM", PREP_ENDM, EOF);
        assertTokenTypesIgnoreCase("DB", PREP_DB, EOF);
        assertTokenTypesIgnoreCase("DW", PREP_DW, EOF);
        assertTokenTypesIgnoreCase("DS", PREP_DS, EOF);
        assertTokenTypesIgnoreCase("$", PREP_ADDR, EOF);
    }

    @Test
    public void testRegisters() {
        assertTokenTypesIgnoreCase("A", REG_A, EOF);
        assertTokenTypesIgnoreCase("B", REG_B, EOF);
        assertTokenTypesIgnoreCase("C", REG_C, EOF);
        assertTokenTypesIgnoreCase("D", REG_D, EOF);
        assertTokenTypesIgnoreCase("E", REG_E, EOF);
        assertTokenTypesIgnoreCase("H", REG_H, EOF);
        assertTokenTypesIgnoreCase("L", REG_L, EOF);
        assertTokenTypesIgnoreCase("M", REG_M, EOF);
        assertTokenTypesIgnoreCase("PSW", REG_PSW, EOF);
        assertTokenTypesIgnoreCase("SP", REG_SP, EOF);
    }

    @Test
    public void testSeparators() {
        assertTokenTypes("(),", SEP_LPAR, SEP_RPAR, SEP_COMMA, EOF);
    }

    @Test
    public void testOperators1() {
        assertTokenTypes("+-*/=", OP_ADD, OP_SUBTRACT, OP_MULTIPLY, OP_DIVIDE, OP_EQUAL, EOF);
    }

    @Test
    public void testOperators2() {
        assertTokenTypesIgnoreCase("MOD", OP_MOD, EOF);
        assertTokenTypesIgnoreCase("SHR", OP_SHR, EOF);
        assertTokenTypesIgnoreCase("SHL", OP_SHL, EOF);
        assertTokenTypesIgnoreCase("NOT", OP_NOT, EOF);
        assertTokenTypesIgnoreCase("AND", OP_AND, EOF);
        assertTokenTypesIgnoreCase("OR", OP_OR, EOF);
        assertTokenTypesIgnoreCase("XOR", OP_XOR, EOF);
    }

    @Test
    public void testIdentifier() {
        assertTokenTypes("u @ ? _", ID_IDENTIFIER, ID_IDENTIFIER, ID_IDENTIFIER, ID_IDENTIFIER, EOF);
        assertTokenTypes("a@ abc ZZ_ H005", ID_IDENTIFIER, ID_IDENTIFIER, ID_IDENTIFIER, ID_IDENTIFIER, EOF);
    }

    @Test
    public void testLabel() {
        assertTokenTypes("u: @: ?: _:", ID_LABEL, ID_LABEL, ID_LABEL, ID_LABEL, EOF);
        assertTokenTypes("a@: abc: ZZ_: H005:", ID_LABEL, ID_LABEL, ID_LABEL, ID_LABEL, EOF);
        assertTokenTypes("a:", ID_LABEL, EOF);
    }

    @Test
    public void testCombineSpaceWithNoSpaceTokens() {
        assertTokenTypes("abc=(XOR,MOD)", ID_IDENTIFIER, OP_EQUAL, SEP_LPAR, OP_XOR, SEP_COMMA, OP_MOD, SEP_RPAR, EOF);
    }
}
