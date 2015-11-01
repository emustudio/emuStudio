/*
 * Copyright (C) 2007-2015 Peter Jakubčo
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

import net.sf.emustudio.zilogZ80.assembler.tree.Label;
import net.sf.emustudio.zilogZ80.assembler.tree.PseudoEQU;
import net.sf.emustudio.zilogZ80.assembler.tree.PseudoMACRO;
import net.sf.emustudio.zilogZ80.assembler.tree.PseudoVAR;
import net.sf.emustudio.zilogZ80.assembler.tree.Row;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Namespace is used in compile time.
 *
 * It is a compile environment. It stores needed values for all compiler passes.
 * sets, macros and equs are pseudo-instructions that aren't added to symbol table
 * in pass1. This means that if eg. equ wasn't defined before first use error
 * comes.
 *
 */
public class Namespace {
    private final Map<String, Label> labels = new HashMap<>();
    private final Map<String, PseudoMACRO> macros = new HashMap<>();
    private final Map<String, PseudoEQU> constants = new HashMap<>();
    private final Map<String, PseudoVAR> variables = new HashMap<>();
    private final List<Row> passNeed = new ArrayList<>();
    private final File inputFile;

    public Namespace(String inputFileName) {
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

    public boolean addLabel(Label label) {
        return addIdentifier(labels, label, label.getName());
    }

    public Label getLabel(String name) {
        return labels.get(name);
    }

    public boolean addMacro(PseudoMACRO macro) {
        return addIdentifier(macros, macro, macro.getName());
    }

    public PseudoMACRO getMacro(String name) {
        return macros.get(name);
    }

    public boolean addConstant(PseudoEQU constant) {
        return addIdentifier(constants, constant, constant.getName());
    }

    public PseudoEQU getConstant(String name) {
        return constants.get(name);
    }

    public boolean setVariable(PseudoVAR variable) {
        if (identifierExists(variable.getName()) && !variables.containsKey(variable.getName())) {
            return false;
        }
        variables.put(variable.getName(), variable);
        return true;
    }

    public PseudoVAR getVariable(String name) {
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

    public void copyTo(Namespace env) {
        for (Label label : labels.values()) {
            env.addLabel(label);
        }
        for (PseudoMACRO macro : macros.values()) {
            env.addMacro(macro);
        }
        for (PseudoEQU equ : constants.values()) {
            env.addConstant(equ);
        }
        for (PseudoVAR set : variables.values()) {
            env.setVariable(set);
        }
    }

    public void addPassNeed(Row n) {
        passNeed.add(n);
    }

    public int getPassNeedCount() {
        return passNeed.size();
    }

    public Row getPassNeed(int index) {
        return passNeed.get(index);
    }

    public void removePassNeed(Row n) {
        passNeed.remove(n);
    }

    public void removePassNeed(int index) {
        passNeed.remove(index);
    }

}
