package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.exceptions.used.SyntaxErrorException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

class ParserErrorListener extends BaseErrorListener {
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
