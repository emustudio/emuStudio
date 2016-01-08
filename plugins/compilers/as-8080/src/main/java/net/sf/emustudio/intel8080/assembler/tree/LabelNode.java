/*
 * Copyright (C) 2007-2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.intel8080.assembler.tree;

public class LabelNode {
    private String name;
    private Integer address;
    private int line;
    private int column;

    public LabelNode(String name, int line, int column) {
        this.name = name;
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
