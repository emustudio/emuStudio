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
