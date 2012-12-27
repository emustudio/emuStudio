/*
 * Statement.java
 *
 * Created on Piatok, 2007, september 21, 8:56
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.treeAbstract;

import emulib.plugins.compiler.HEXFileHandler;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;

public abstract class Statement {
    protected int line;
    protected int column;

    public abstract boolean isPseudo();

    public Statement(int line, int column) {
        // this.mnemo = mnemo;
        this.line = line;
        this.column = column;
    }

    // return size of compiled code in bytes
    public abstract int getSize();

    public abstract void pass1() throws Exception;

    public abstract int pass2(Namespace parentEnv, int addr_start) throws Exception;

    public abstract void pass4(HEXFileHandler hex) throws Exception;
}
