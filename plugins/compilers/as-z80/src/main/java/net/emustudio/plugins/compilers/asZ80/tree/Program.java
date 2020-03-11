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

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compilers.asZ80.exceptions.NeedMorePassException;
import net.emustudio.plugins.compilers.asZ80.Namespace;

import java.util.ArrayList;
import java.util.List;

public class Program {
    private final List<Row> list = new ArrayList<>();
    private Namespace namespace;
    private final List<String> includefiles = new ArrayList<>();

    void addIncludeFiles(List<String> inclfiles) {
        includefiles.addAll(inclfiles);
    }

    public void addRow(Row node) {
        list.add(node);
    }

    public int getSize() {
        int size = 0;
        for (Row row : list) {
            size += row.getSize();
        }
        return size;
    }

    /* PASS1 = symbol table
     * 1. get all label definitions
     * 2. get all macro definitions
     */
    public Namespace getNamespace() {
        return namespace;
    }

    /**
     * Method check whether this "subprogram" contains include
     * pseudocode(s) and if yes, whether the statement calls for
     * filename given by parameter.
     *
     * @param filename name of the file that "include" pseudocode should contain
     * @return true if subprogram contains "include filename" pseudocode
     */
    boolean getIncludeLoops(String filename) {
        int i;
        for (i = 0; i < includefiles.size(); i++) {
            String s = includefiles.get(i);
            if (s.equals(filename)) {
                return true;
            }
        }
        includefiles.add(filename);
        Row in;
        for (i = 0; i < list.size(); i++) {
            in = list.get(i);
            if (in.getIncludeLoops(filename)) {
                return true;
            }
        }
        return false;
    }

    public void pass1(Namespace namespace) throws Exception {
        this.namespace = namespace;
        pass1();
    }

    // creates symbol table
    // return next current address
    public void pass1() throws Exception {
        // only labels and macros have right to be all added to symbol table at once
        for (Row row : list) {
            if (row.label != null) {
                if (!namespace.addLabel(row.label)) {
                    throw new Exception("Error: Label already defined: " + row.label.getName());
                }
            }
            if ((row.statement instanceof PseudoMACRO)) {
                if (!namespace.addMacro((PseudoMACRO) row.statement)) {
                    throw new Exception("Error: Macro already defined: "
                        + ((PseudoMACRO) row.statement).getName());
                }
            }
            if ((row.statement instanceof PseudoINCLUDE)) {
                row.pass1(includefiles, namespace);
            } else {
                row.pass1();
            }
        }
    }

    // pass2 tries to evaulate all expressions and compute relative addresses
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        int curr_addr;
        for (Row row : list) {
            try {
                curr_addr = row.pass2(parentEnv, addr_start);
                addr_start = curr_addr;
            } catch (NeedMorePassException e) {
                parentEnv.addPassNeed(row);
                addr_start += row.getSize();
            }
        }
        return addr_start;
    }

    public int pass2(int addr_start) throws Exception {
        return this.pass2(namespace, addr_start);
    }

    public boolean pass3(Namespace parentEnv) throws Exception {
        int pnCount = parentEnv.getPassNeedCount();
        for (int i = parentEnv.getPassNeedCount() - 1; i >= 0; i--) {
            if (parentEnv.getPassNeed(i).pass3(parentEnv)) {
                pnCount--;
                parentEnv.removePassNeed(i);
            }
        }
        return pnCount < parentEnv.getPassNeedCount();
    }

    public void pass4(IntelHEX hex) throws Exception {
        for (Row row : list) {
            row.pass4(hex);
        }
    }

    public void pass4(IntelHEX hex, Namespace env) throws Exception {
        this.namespace = env;
        pass4(hex);
    }
}
