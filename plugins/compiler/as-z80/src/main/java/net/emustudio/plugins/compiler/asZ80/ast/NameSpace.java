/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.plugins.compiler.asZ80.CompileError;

import java.util.*;

public class NameSpace {
    private final List<CompileError> errors = new ArrayList<>();
    private final Map<String, Evaluated> definitions = new HashMap<>();

    public void error(CompileError error) {
        errors.add(Objects.requireNonNull(error));
    }

    public boolean hasError(int errorCode) {
        return errors.stream().anyMatch(e -> e.errorCode == errorCode);
    }

    public boolean hasNoErrors() {
        return errors.isEmpty();
    }

    public void put(String id, Evaluated value) {
        definitions.put(id, value);
    }

    public void remove(String id) {
        definitions.remove(id);
    }

    public Evaluated get(String id) {
        return definitions.get(id);
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
