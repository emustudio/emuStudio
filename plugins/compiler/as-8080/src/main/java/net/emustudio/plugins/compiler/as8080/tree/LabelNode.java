/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.as8080.tree;

public class LabelNode {
    private final String name;
    private Integer address;
    private final int line;
    private final int column;

    public LabelNode(String name, int line, int column) {
        this.name = name;
        this.address = null;

        this.line = line;
        this.column = column;
    }

    void setAddress(Integer address) {
        this.address = address;
    }

    Integer getAddress() {
        return this.address;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "LabelNode{" +
            "name='" + name + '\'' +
            ", address=" + address +
            ", line=" + line +
            ", column=" + column +
            '}';
    }
}
