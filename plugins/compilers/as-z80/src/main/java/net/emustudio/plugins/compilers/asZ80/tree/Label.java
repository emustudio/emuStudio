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
package net.emustudio.plugins.compilers.asZ80.tree;

import java.util.Objects;

public class Label {
    private final String name;
    private final int line;
    private final int column;

    private Integer address;

    public Label(String name, int line, int column) {
        this.name = Objects.requireNonNull(name);
        this.address = null;

        this.line = line;
        this.column = column;
    }

    public void setAddress(Integer address) {
        this.address = address;
    }

    public Integer getAddress() {
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
}
