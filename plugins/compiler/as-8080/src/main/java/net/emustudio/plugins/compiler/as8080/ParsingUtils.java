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
package net.emustudio.plugins.compiler.as8080;

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
