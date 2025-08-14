/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;

public class Evaluated extends Node {
    public final int value;
    public final boolean isAddress;

    public Evaluated(SourceCodePosition position, int value, boolean isAddress) {
        super(position);
        this.value = value;
        this.isAddress = isAddress;
    }

    public Evaluated(SourceCodePosition position, int value) {
        this(position, value, false);
    }

    @Override
    protected Node mkCopy() {
        return new Evaluated(position, value, isAddress);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Evaluated evaluated = (Evaluated) o;
        return value == evaluated.value;
    }

    @Override
    protected String toStringShallow() {
        return "Evaluated(" + value + ")";
    }
}
