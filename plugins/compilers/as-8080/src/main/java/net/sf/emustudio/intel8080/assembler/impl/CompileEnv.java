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

import net.sf.emustudio.intel8080.assembler.tree.EquPseudoNode;
import net.sf.emustudio.intel8080.assembler.tree.InstructionNode;
import net.sf.emustudio.intel8080.assembler.tree.LabelNode;
import net.sf.emustudio.intel8080.assembler.tree.MacroPseudoNode;
import net.sf.emustudio.intel8080.assembler.tree.SetPseudoNode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final Map<String, LabelNode> labels = new HashMap<>();
    private final Map<String, MacroPseudoNode> macros = new HashMap<>();
    private final Map<String, EquPseudoNode> constants = new HashMap<>();
    private final Map<String, SetPseudoNode> variables = new HashMap<>();
    private final List<InstructionNode> passNeed = new ArrayList<>();
    private final File inputFile;

    public CompileEnv(String inputFileName) {
        this.inputFile = new File(inputFileName);
    }

    public File getInputFile() {
        return inputFile;
    }

    private boolean identifierExists(String name) {
        Objects.requireNonNull(name);

        return labels.containsKey(name)
                || macros.containsKey(name)
                || constants.containsKey(name)
                || variables.containsKey(name);
    }

    private <T> boolean addIdentifier(Map<String, T> identifiers, T identifier, String name) {
        if (identifiers.get(name) == identifier) {
            return true;
        } else if (identifierExists(name)) {
            return false;
        }
        identifiers.put(name, identifier);
        return true;
    }

    public boolean addLabel(LabelNode label) {
        return addIdentifier(labels, label, label.getName());
    }

    public LabelNode getLabel(String name) {
        return labels.get(name);
    }

    public boolean addMacro(MacroPseudoNode macro) {
        return addIdentifier(macros, macro, macro.getName());
    }

    public MacroPseudoNode getMacro(String name) {
        return macros.get(name);
    }

    public boolean addConstant(EquPseudoNode constant) {
        return addIdentifier(constants, constant, constant.getName());
    }

    public EquPseudoNode getConstant(String name) {
        return constants.get(name);
    }

    public boolean setVariable(SetPseudoNode variable) {
        if (identifierExists(variable.getName()) && !variables.containsKey(variable.getName())) {
            return false;
        }
        variables.put(variable.getName(), variable);
        return true;
    }

    public SetPseudoNode getVariable(String name) {
        return variables.get(name);
    }

    // odstrani vsetky existujuce definicie s danym nazvom
    // vyuziva sa pri bloku macro
    public void removeAllDefinitions(String name) {
        labels.remove(name);
        macros.remove(name);
        constants.remove(name);
        variables.remove(name);
    }

    public void copyTo(CompileEnv env) {
        for (LabelNode label : labels.values()) {
            env.addLabel(label);
        }
        for (MacroPseudoNode macro : macros.values()) {
            env.addMacro(macro);
        }
        for (EquPseudoNode equ : constants.values()) {
            env.addConstant(equ);
        }
        for (SetPseudoNode set : variables.values()) {
            env.setVariable(set);
        }
    }

    public void addPassNeed(InstructionNode n) {
        passNeed.add(n);
    }

    public int getPassNeedCount() {
        return passNeed.size();
    }

    public InstructionNode getPassNeed(int index) {
        return passNeed.get(index);
    }

    public void removePassNeed(int index) {
        passNeed.remove(index);
    }

}
