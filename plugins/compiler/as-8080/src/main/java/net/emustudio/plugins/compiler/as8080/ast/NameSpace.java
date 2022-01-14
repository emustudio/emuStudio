package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.CompileError;

import java.util.*;

public class NameSpace {
    private final List<CompileError> errors = new ArrayList<>();
    private final Map<String, Optional<Evaluated>> definitions = new HashMap<>();

    public void error(CompileError error) {
        errors.add(Objects.requireNonNull(error));
    }

    public boolean hasError(int errorCode) {
        return errors.stream().anyMatch(e -> e.errorCode == errorCode);
    }

    public boolean hasNoErrors() {
        return errors.isEmpty();
    }

    public void put(String id, Optional<Evaluated> value) {
        definitions.put(id, value);
    }

    public void remove(String id) {
        definitions.remove(id);
    }

    public Optional<Evaluated> get(String id) {
        return Optional.ofNullable(definitions.get(id)).flatMap(e -> e);
    }

    public List<CompileError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public String toString() {
        return "NameSpace{" +
            "errors=" + errors +
            '}';
    }
}
