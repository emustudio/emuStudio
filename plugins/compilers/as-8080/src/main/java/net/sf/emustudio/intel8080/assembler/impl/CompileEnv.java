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
package net.sf.emustudio.intel8080.assembler.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.sf.emustudio.intel8080.assembler.tree.EquPseudoNode;
import net.sf.emustudio.intel8080.assembler.tree.InstructionNode;
import net.sf.emustudio.intel8080.assembler.tree.LabelNode;
import net.sf.emustudio.intel8080.assembler.tree.MacroPseudoNode;
import net.sf.emustudio.intel8080.assembler.tree.SetPseudoNode;

/**
 * Compile environment.
 *
 * It stores needed values for all compiler passes. This is something like symbol table.
 *
 * Sets, macros and equs are pseudoinstructions that aren't added into symbol table in pass1. This means if eg. equ
 * wasn't defined before first use, error raises.
 *
 */
public class CompileEnv {
    private final List<LabelNode> defLabels = new ArrayList<>();
    private final List<MacroPseudoNode> defMacros = new ArrayList<>();
    private final List<EquPseudoNode> defEqus = new ArrayList<>();
    private final List<SetPseudoNode> defSets = new ArrayList<>();
    private final List<InstructionNode> passNeed = new ArrayList<>();
    private final File inputFile;

    public CompileEnv(String inputFileName) {
        this.inputFile = new File(inputFileName);
    }

    public File getInputFile() {
        return inputFile;
    }
    
    // check if id is already defined (as whatever)
    private boolean idExists(String name) {
        Objects.requireNonNull(name);
        
        if (getLabel(name) != null) {
            return true;
        }
        if (getMacro(name) != null) {
            return true;
        }
        if (getEqu(name) != null) {
            return true;
        }
        return getSet(name) != null;
    }

    public boolean addLabelDef(LabelNode label) {
        if (idExists(label.getName())) {
            return false;
        }
        return defLabels.add(label);
    }

    public LabelNode getLabel(String name) {
        for (LabelNode label : defLabels) {
            if (label.getName().equals(name)) {
                return label;
            }
        }
        return null;
    }

    public boolean addMacroDef(MacroPseudoNode m) {
        if (idExists(m.getName())) {
            return false;
        }
        return defMacros.add(m);
    }

    // search for macro definition in symbol table
    public MacroPseudoNode getMacro(String name) {
        for (MacroPseudoNode macro : defMacros) {
            if (macro.getName().equals(name)) {
                return macro;
            }
        }
        return null;
    }

    public boolean addEquDef(EquPseudoNode e) {
        if (idExists(e.getName()) == true) {
            return false;
        }
        defEqus.add(e);
        return true;
    }

    public EquPseudoNode getEqu(String name) {
        for (EquPseudoNode equ : defEqus) {
            if (equ.getName().equals(name)) {
                return equ;
            }
        }
        return null;
    }

    // prida alebo prepise existujucu definiciu
    // pridava sa samozrejme az v pass2
    public boolean addSetDef(SetPseudoNode s) {
        if (idExists(s.getName())) {
            return false;
        }
        SetPseudoNode exs = getSet(s.getName());
        if (exs != null) {
            defSets.remove(exs);
        }
        return defSets.add(s);
    }

    public SetPseudoNode getSet(String name) {
        for (SetPseudoNode set : defSets) {
            if (set.getName().equals(name)) {
                return set;
            }
        }
        return null;
    }

    // odstrani vsetky existujuce definicie s danym nazvom
    // vyuziva sa pri bloku macro
    public void removeAllDefinitions(String name) {
        for (int i = defLabels.size() - 1; i >= 0; i--) {
            LabelNode in = (LabelNode) defLabels.get(i);
            if (in.getName().equals(name)) {
                defLabels.remove(i);
            }
        }
        for (int i = defMacros.size() - 1; i >= 0; i--) {
            MacroPseudoNode mn = (MacroPseudoNode) defMacros.get(i);
            if (mn.getName().equals(name)) {
                defMacros.remove(i);
            }
        }
        for (int i = defEqus.size() - 1; i >= 0; i--) {
            EquPseudoNode mn = (EquPseudoNode) defEqus.get(i);
            if (mn.getName().equals(name)) {
                defEqus.remove(i);
            }
        }
        for (int i = defSets.size() - 1; i >= 0; i--) {
            SetPseudoNode mn = (SetPseudoNode) defSets.get(i);
            if (mn.getName().equals(name)) {
                defSets.remove(i);
            }
        }
    }

    public boolean copyTo(CompileEnv env) {
        boolean r = true;
        for (LabelNode label : defLabels) {
            r &= env.addLabelDef(label);
        }
        for (MacroPseudoNode macro : defMacros) {
            r &= env.addMacroDef(macro);
        }
        for (EquPseudoNode equ : defEqus) {
            r &= env.addEquDef(equ);
        }
        for (SetPseudoNode set : defSets) {
            r &= env.addSetDef(set);
        }
        return r;
    }

    public void addPassNeed(InstructionNode n) {
        passNeed.add(n);
    }

    public int getPassNeedCount() {
        return passNeed.size();
    }

    public InstructionNode getPassNeed(int index) {
        return (InstructionNode) passNeed.get(index);
    }

    public void removePassNeed(InstructionNode n) {
        passNeed.remove(n);
    }

    public void removePassNeed(int index) {
        passNeed.remove(index);
    }

    public void clearPassNeeds() {
        passNeed.clear();
    }
}
