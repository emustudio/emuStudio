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
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.memory.ram.api.RamLabel;

public class Label implements RamLabel {
    public final int line;
    public final int column;

    private final String label;
    private final int address;

    public Label(int line, int column, String text, int address) {
        this.line = line;
        this.column = column;
        this.label = text.toUpperCase();
        this.address = address;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Label label1 = (Label) o;

        if (address != label1.address) return false;
        return label.equals(label1.label);
    }

    @Override
    public int hashCode() {
        int result = label.hashCode();
        result = 31 * result + address;
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "label='" + label + '\'' +
                ", address=" + address +
                '}';
    }
}
