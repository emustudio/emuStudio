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
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;

import java.util.Objects;
import java.util.Optional;

public class Program extends Node {
    private final NameSpace env;
    private String filename;

    public Program(int line, int column, NameSpace env) {
        super(line, column);
        this.env = Objects.requireNonNull(env);
    }

    public Program(NameSpace env) {
        this(0, 0, env);
    }

    public Program() {
        this(new NameSpace());
    }

    public void setFileName(String filename) {
        this.filename = filename;
    }

    public Optional<String> getFileName() {
        return Optional.ofNullable(filename);
    }


    public NameSpace env() {
        return env;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        Program program = new Program(line, column, env);
        program.setFileName(filename);
        return program;
    }
}
