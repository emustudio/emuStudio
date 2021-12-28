package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.exceptions.CompileException;
import org.antlr.v4.runtime.Token;

import java.util.Locale;

public class ParsingUtils {

    public static String parseLitString(Token token) {
        // LIT_STRING_1: '\'' ~[']* '\'';
        // LIT_STRING_2: '"' ~["]* '"';
        String text = token.getText();
        return text.substring(1, text.length() - 1);
    }

    public static int parseLitHex1(Token token) {
        // LIT_HEXNUMBER_1: [\-]? '0' X [0-9a-fA-F]+;
        return Integer.decode(token.getText());
    }

    public static int parseLitHex2(Token token) {
        // LIT_HEXNUMBER_2: [\-]? [0-9a-fA-F]+ H;
        String rawText = token.getText();
        return Integer.parseInt(rawText.substring(0, rawText.length() - 1), 16);
    }

    public static int parseLitOct(Token token) {
        // LIT_OCTNUMBER: [\-]? [0-7]+ [oOqQ];
        String rawText = token.getText();
        return Integer.parseInt(rawText.substring(0, rawText.length() - 1), 8);
    }

    public static int parseLitDec(Token token) {
        // LIT_NUMBER: [\-]? [0-9]+ D?
        String rawText = token.getText();
        if (rawText.endsWith("d") || rawText.endsWith("D")) {
            return Integer.parseInt(rawText.substring(0, rawText.length() - 1), 10);
        } else {
            return Integer.parseInt(rawText, 10);
        }
    }

    public static int parseLitBin(Token token) {
        // LIT_BINNUMBER: [01]+ B;
        String rawText = token.getText();
        return Integer.parseInt(rawText.substring(0, rawText.length() - 1), 2);
    }

    public static int parseOpcode(Token token) {
        switch (token.getText().toLowerCase(Locale.ENGLISH)) {
            case "mvi":
                return 6;
            case "lxi":
                return 1;
            case "lda":
                return 0x3A;
            case "sta":
                return 0x32;
            case "lhld":
                return 0x2A;
            case "shld":
                return 0x22;
            case "adi":
                return 0xC6;
            case "aci":
                return 0xCE;
            case "sui":
                return 0xD6;
            case "sbi":
                return 0xDE;
            case "ani":
                return 0xE6;
            case "ori":
                return 0xF6;
            case "xri":
                return 0xEE;
            case "cpi":
                return 0xFE;
            case "jmp":
                return 0xC3;
            case "jc":
                return 0xDA;
            case "jnc":
                return 0xD2;
            case "jz":
                return 0xCA;
            case "jnz":
                return 0xC2;
            case "jm":
                return 0xFA;
            case "jp":
                return 0xF2;
            case "jpe":
                return 0xEA;
            case "jpo":
                return 0xE2;
            case "call":
                return 0xCD;
            case "cc":
                return 0xDC;
            case "cnc":
                return 0xD4;
            case "cnz":
                return 0xC4;
            case "cm":
                return 0xFC;
            case "cp":
                return 0xF4;
            case "cpe":
                return 0xEC;
            case "cpo":
                return 0xE4;
            case "in":
                return 0xDB;
            case "out":
                return 0xD3;
            case "stc":
                return 0x37;
            case "cmc":
                return 0x3F;
            case "cma":
                return 0x2F;
            case "daa":
                return 0x27;
            case "nop":
                return 0;
            case "rlc":
                return 7;
            case "rrc":
                return 0xF;
            case "ral":
                return 0x17;
            case "rar":
                return 0x1F;
            case "xchg":
                return 0xEB;
            case "xthl":
                return 0xE3;
            case "sphl":
                return 0xF9;
            case "pchl":
                return 0xE9;
            case "ret":
                return 0xC9;
            case "rc":
                return 0xD8;
            case "rnc":
                return 0xD0;
            case "rz":
                return 0xC8;
            case "rnz":
                return 0xC0;
            case "rm":
                return 0xF8;
            case "rp":
                return 0xF0;
            case "rpe":
                return 0xE8;
            case "rpo":
                return 0xE0;
            case "ei":
                return 0xFB;
            case "di":
                return 0xF3;
            case "hlt":
                return 0x76;
            case "inr":
                return 4;
            case "dcr":
                return 5;
            case "add":
                return 0x80;
            case "adc":
                return 0x88;
            case "sub":
                return 0x90;
            case "sbb":
                return 0x98;
            case "ana":
                return 0xA0;
            case "xra":
                return 0xA8;
            case "ora":
                return 0xB0;
            case "cmp":
                return 0xB8;
            case "mov":
                return 0x40;
            case "stax":
                return 2;
            case "ldax":
                return 0xA;
            case "push":
                return 0xC5;
            case "pop":
                return 0xC1;
            case "dad":
                return 9;
            case "inx":
                return 3;
            case "dcx":
                return 0xB;
            case "rst":
                return 0xC7;
        }
        throw new CompileException(token.getCharPositionInLine(), token.getLine(), "Unknown instruction opcode: " + token.getText());
    }

    public static int parseReg(Token token) {
        switch (token.getText().toLowerCase(Locale.ENGLISH)) {
            case "a":
                return 7;
            case "b":
                return 0;
            case "c":
                return 1;
            case "d":
                return 2;
            case "e":
                return 3;
            case "h":
                return 4;
            case "l":
                return 5;
            case "m":
                return 6;
        }
        throw new CompileException(token.getCharPositionInLine(), token.getLine(), "Unknown register: " + token.getText());
    }

    public static int parseRegPair(Token token) {
        switch (token.getText().toLowerCase(Locale.ENGLISH)) {
            case "b":
                return 0;
            case "d":
                return 1;
            case "h":
                return 2;
            case "psw":
            case "sp":
                return 3;
        }
        throw new CompileException(token.getCharPositionInLine(), token.getLine(), "Unknown register pair: " + token.getText());
    }

    public static String parseLabel(Token token) {
        String rawText = token.getText();
        return rawText.substring(0, rawText.length() - 1);
    }

    public static String normalizeId(String id) {
        return id.toLowerCase(Locale.ENGLISH);
    }
}
