/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;

import java.util.Objects;

public class Program extends Node {
    private final NameSpace env;

    public Program(SourceCodePosition position, NameSpace env) {
        super(position);
        this.env = Objects.requireNonNull(env);
    }

    public Program(String fileName, NameSpace env) {
        this(new SourceCodePosition(0, 0, fileName), env);
    }

    public Program(String fileName) {
        this(fileName, new NameSpace());
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
        return new Program(position, env);
    }
}
