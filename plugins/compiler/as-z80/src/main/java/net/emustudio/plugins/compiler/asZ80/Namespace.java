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
package net.emustudio.plugins.compiler.asZ80;

import net.emustudio.plugins.compiler.asZ80.tree.*;

import java.io.File;
import java.util.*;

/**
 * Namespace is used in compile time.
 * <p>
 * It is a compile environment. It stores needed values for all compiler passes.
 * sets, macros and equs are pseudo-instructions that aren't added to symbol table
 * in pass1. This means that if eg. equ wasn't defined before first use error
 * comes.
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

    // it is used in a macro block
    public void removeAllDefinitions(String name) {
        labels.remove(name);
        macros.remove(name);
        constants.remove(name);
        variables.remove(name);
    }

    public void copyTo(Namespace env) {
        labels.values().forEach(env::addLabel);
        macros.values().forEach(env::addMacro);
        constants.values().forEach(env::addConstant);
        variables.values().forEach(env::setVariable);
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

    public void removePassNeed(int index) {
        passNeed.remove(index);
    }
}
