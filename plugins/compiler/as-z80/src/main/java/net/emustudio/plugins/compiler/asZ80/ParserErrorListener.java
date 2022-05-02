package net.emustudio.plugins.compiler.asZ80;

import net.emustudio.plugins.compiler.asZ80.exceptions.SyntaxErrorException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

class ParserErrorListener extends BaseErrorListener {
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
            throw new SyntaxErrorException(line, charPositionInLine, msg);
        } else {
            throw new SyntaxErrorException(line, charPositionInLine, msg, e);
        }
    }
}
