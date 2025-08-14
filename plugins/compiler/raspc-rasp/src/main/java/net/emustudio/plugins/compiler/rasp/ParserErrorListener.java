/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.plugins.compiler.rasp.exceptions.SyntaxErrorException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ParserErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {

        if (e == null) {
            throw new SyntaxErrorException(line, charPositionInLine, msg);
        } else {
            throw new SyntaxErrorException(line, charPositionInLine, msg, e);
        }
    }
}
