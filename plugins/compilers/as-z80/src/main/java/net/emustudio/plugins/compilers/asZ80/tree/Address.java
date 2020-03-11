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

import net.emustudio.plugins.compilers.asZ80.treeAbstract.Expression;
import net.emustudio.plugins.compilers.asZ80.Namespace;

public class Address extends Expression {

    public void setAddress(int address) {
        this.value = address;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int eval(Namespace env, int curr_addr) {
        this.setAddress(curr_addr);
        return curr_addr;
    }
}
