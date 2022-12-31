/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

public class Evaluated extends Node {
    public final int value;
    public final boolean isAddress;

    public Evaluated(int line, int column, int value, boolean isAddress) {
        super(line, column);
        this.value = value;
        this.isAddress = isAddress;
    }

    public Evaluated(int line, int column, int value) {
        this(line, column, value, false);
    }

    @Override
    protected Node mkCopy() {
        return new Evaluated(line, column, value, isAddress);
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
