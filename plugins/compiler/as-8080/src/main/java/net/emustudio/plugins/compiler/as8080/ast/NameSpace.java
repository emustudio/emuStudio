package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.CompileError;

import java.util.*;

public class NameSpace {
    private final List<CompileError> errors = new ArrayList<>();

    public void error(CompileError error) {
        errors.add(Objects.requireNonNull(error));
    }

    public boolean hasError(int errorCode) {
        return errors.stream().anyMatch(e -> e.errorCode == errorCode);
    }

    public boolean hasNoErrors() {
        return errors.isEmpty();
    }

    @Override
    public String toString() {
        return "NameSpace{" +
            "errors=" + errors +
            '}';
    }
}
