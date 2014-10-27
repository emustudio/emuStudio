/*
 * Row.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
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
package net.sf.emustudio.ram.compiler.tree;

import java.util.List;
import net.sf.emustudio.ram.compiler.impl.CompiledCode;
import net.sf.emustudio.ram.compiler.impl.CompilerEnvironment;

public class Row {
    private RAMInstructionImpl stat;
    private Label label;

    public Row(RAMInstructionImpl stat, Label label) {
        this.stat = stat;
        if (label != null) {
            this.label = label;
            CompilerEnvironment.addLabel(label);
        }
    }

    public Row(Label label) {
        this.stat = null;
        if (label != null) {
            this.label = label;
            CompilerEnvironment.addLabel(label);
        }
    }

    public Row(List<String> inputs) {
        this.stat = null;
        this.label = null;
        CompilerEnvironment.addInputs(inputs);
    }

    public int pass1(int addr_start) throws Exception {
        if (label != null) {
            label.pass1(addr_start);
        }
        if (stat != null) {
            return addr_start + 1;
        } else {
            return addr_start;
        }
    }

    public void pass2(CompiledCode hex) throws Exception {
        if (stat != null) {
            if (stat.pass2()) {
                hex.addCode(stat);
            } else {
                throw new Exception("Undefined label: '" + stat.getOperandLabel() + "'");
            }
        }
    }
}
