package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.exceptions.AlreadyDeclaredException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class NameSpace {

    private final Map<String, Node> declarations = new HashMap<>();
    private final Map<String, Node> macros = new HashMap<>();

    public void addDeclaration(String id, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);

        if (declarations.containsKey(idLower)) {
            throw new AlreadyDeclaredException(node.line, node.column, id);
        }
        declarations.put(idLower, node);
    }

    public void addMacro(String id, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);

        if (macros.containsKey(idLower)) {
            throw new AlreadyDeclaredException(node.line, node.column, id);
        }
        macros.put(idLower, node);
    }

    public Optional<Node> getDeclaration(String id) {
        return Optional.ofNullable(declarations.get(id.toLowerCase(Locale.ENGLISH)));
    }

    public Optional<Node> getMacro(String id) {
        return Optional.ofNullable(macros.get(id.toLowerCase(Locale.ENGLISH)));
    }
}
