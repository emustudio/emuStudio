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
package net.emustudio.plugins.compiler.ramc.tree;

import java.io.Serializable;

public class Label implements Serializable {
    private int address;
    private String value;
    private boolean evaluated = false;

    public Label(String text) {
        this.value = text.toUpperCase();
    }

    int pass1(int addr) {
        this.address = addr;
        this.evaluated = true;
        return addr;
    }

    public int getAddress() {
        if (!evaluated) {
            throw new IndexOutOfBoundsException();
        }
        return address;
    }

    public String getValue() {
        return value;
    }

}
