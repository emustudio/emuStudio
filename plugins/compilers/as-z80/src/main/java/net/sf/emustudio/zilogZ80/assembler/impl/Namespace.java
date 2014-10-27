/*
 * Copyright (C) 2007-2014 Peter Jakubčo
 *
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
package net.sf.emustudio.zilogZ80.assembler.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.emustudio.zilogZ80.assembler.tree.Label;
import net.sf.emustudio.zilogZ80.assembler.tree.PseudoEQU;
import net.sf.emustudio.zilogZ80.assembler.tree.PseudoMACRO;
import net.sf.emustudio.zilogZ80.assembler.tree.PseudoVAR;
import net.sf.emustudio.zilogZ80.assembler.tree.Row;

/**
 * Namespace is used in compile time.
 *
 * It is a compile environment. It stores needed values for all compiler passes.
 * sets, macros and equs are pseudoinstructions that arent added to symbol table
 * in pass1. This means that if eg. equ wasnt defined before first use error
 * comes.
 *
 */
public class Namespace {
    private final Map<String, Label> defLabels = new HashMap<>();
    private final Map<String, PseudoMACRO> defMacros = new HashMap<>();
    private final Map<String, PseudoEQU> defEqus = new HashMap<>();
    private final Map<String, PseudoVAR> defVars = new HashMap<>();
    private final List<Row> passNeed = new ArrayList<>();
    private final File inputFile;

    public Namespace(String inputFileName) {
        this.inputFile = new File(inputFileName);
    }

    public File getInputFile() {
        return inputFile;
    }

    // check if id is already defined (as whatever)
    private boolean idExists(String name) {
        if (defLabels.containsKey(name)) {
            return true;
        }
        if (defMacros.containsKey(name)) {
            return true;
        }
        if (defEqus.containsKey(name)) {
            return true;
        }
        return defVars.containsKey(name);
    }

    public boolean addLabelDef(Label l) {
        String n = l.getName();
        if (idExists(n) == true) {
            return false;
        } else {
            defLabels.put(n, l);
        }
        return true;
    }

    public Label getLabel(String name) {
        return defLabels.get(name);
    }

    public boolean addMacroDef(PseudoMACRO m) {
        String n = m.getName();
        if (idExists(n) == true) {
            return false;
        } else {
            defMacros.put(n, m);
        }
        return true;
    }

    // search for macro definition in symbol table
    public PseudoMACRO getMacro(String name) {
        return defMacros.get(name);
    }

    public boolean addEquDef(PseudoEQU e) {
        String n = e.getName();
        if (idExists(n) == true) {
            return false;
        } else {
            defEqus.put(n, e);
        }
        return true;
    }

    public PseudoEQU getEqu(String name) {
        return defEqus.get(name);
    }

    // prida alebo prepise existujucu definiciu
    // pridava sa samozrejme az v pass2
    public boolean addVarDef(PseudoVAR s) {
        defVars.put(s.getName(), s);
        return true;
    }

    public PseudoVAR getVar(String name) {
        return defVars.get(name);
    }

    // odstrani vsetky existujuce definicie s danym nazvom
    // vyuziva sa pri bloku macro
    public void removeAllDefinitions(String name) {
        defLabels.remove(name);
        defMacros.remove(name);
        defEqus.remove(name);
        defVars.remove(name);
    }

    public void copyTo(Namespace env) {
        Iterator<?> i = defLabels.values().iterator();
        while (i.hasNext()) {
            env.addLabelDef((Label) i.next());
        }
        i = defMacros.values().iterator();
        while (i.hasNext()) {
            env.addMacroDef((PseudoMACRO) i.next());
        }
        i = defEqus.values().iterator();
        while (i.hasNext()) {
            env.addEquDef((PseudoEQU) i.next());
        }
        i = defVars.values().iterator();
        while (i.hasNext()) {
            env.addVarDef((PseudoVAR) i.next());
        }
    }

    public void addPassNeed(Row n) {
        passNeed.add(n);
    }

    public int getPassNeedCount() {
        return passNeed.size();
    }

    public Row getPassNeed(int index) {
        return (Row) passNeed.get(index);
    }

    public void removePassNeed(Row n) {
        passNeed.remove(n);
    }

    public void removePassNeed(int index) {
        passNeed.remove(index);
    }

    public void clearPassNeeds() {
        passNeed.clear();
    }
}
