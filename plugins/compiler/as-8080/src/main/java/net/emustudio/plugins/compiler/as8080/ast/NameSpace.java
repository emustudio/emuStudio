package net.emustudio.plugins.compiler.as8080.ast;

import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NameSpace {

    private final Map<String, Token> labels = new HashMap<>();

    public void addLabel(Token token) {
        labels.putIfAbsent(token.getText().toLowerCase(Locale.ENGLISH), token);
    }

}
