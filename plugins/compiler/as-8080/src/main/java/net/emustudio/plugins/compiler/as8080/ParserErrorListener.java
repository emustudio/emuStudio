/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.exceptions.SyntaxErrorException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.Objects;

class ParserErrorListener extends BaseErrorListener {
    private final String fileName;

    ParserErrorListener(String fileName) {
        this.fileName = Objects.requireNonNull(fileName);
    }

    // TODO: parse message expected tokens to token categories
    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {

        if (e == null) {
            throw new SyntaxErrorException(new SourceCodePosition(line, charPositionInLine, fileName), msg);
        } else {
            throw new SyntaxErrorException(new SourceCodePosition(line, charPositionInLine, fileName), msg, e);
        }
    }
}
