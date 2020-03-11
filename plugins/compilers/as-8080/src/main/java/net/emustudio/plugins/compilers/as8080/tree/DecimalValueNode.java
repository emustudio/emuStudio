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
package net.emustudio.plugins.compilers.as8080.tree;

import net.emustudio.plugins.compilers.as8080.Namespace;
import net.emustudio.plugins.compilers.as8080.treeAbstract.ExprNode;

public class DecimalValueNode extends ExprNode {

    public DecimalValueNode(int value) {
        this.value = value;
    }

    public int getSize() {
        if ((value & 0xFF) == value) {
            return 1;
        }
        return 2;
    }

    @Override
    public int eval(Namespace env, int curr_addr) throws Exception {
        return value;
    }
}
