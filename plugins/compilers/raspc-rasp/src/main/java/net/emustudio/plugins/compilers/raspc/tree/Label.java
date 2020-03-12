/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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
package net.emustudio.plugins.compilers.raspc.tree;

import net.emustudio.plugins.compilers.raspc.CompilerOutput;

public class Label extends AbstractTreeNode {

    private final String value;
    private int address;

    public int getAddress() {
        return address;
    }

    public String getValue() {
        return value;
    }

    public Label(String value) {
        this.value = value.toUpperCase();
    }

    public void setAddress(int address) {
        this.address = address;
    }

    @Override
    public void pass() {
        CompilerOutput.getInstance().addLabel(this);
    }
}
