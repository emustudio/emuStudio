package net.emustudio.plugins.compiler.asZ80;

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

    public static String parseLabel(Token token) {
        String rawText = token.getText();
        return rawText.substring(0, rawText.length() - 1);
    }

    public static String normalizeId(String id) {
        return id.toLowerCase(Locale.ENGLISH);
    }
}
