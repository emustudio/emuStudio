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
import net.emustudio.plugins.compilers.asZ80.exceptions.InvalidMacroParamsCountException;
import net.emustudio.plugins.compilers.asZ80.exceptions.UnknownMacroParametersException;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Expression;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Pseudo;
import net.emustudio.plugins.compilers.asZ80.Namespace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PseudoMACRO extends Pseudo {
    private final List<String> params; // macro parameters
    private List<Expression> call_params; // concrete parameters, they can change
    private final Program subprogram;
    private final String mnemo;
    // for generateCode
    private Namespace newEnv;

    public PseudoMACRO(String name, List<String> params, Program s, int line,
                       int column) {
        super(line, column);
        this.mnemo = name;
        this.params = Objects.requireNonNullElseGet(params, ArrayList::new);
        this.subprogram = s;
    }

    public String getName() {
        return mnemo;
    }

    public void setCallParams(List<Expression> params) {
        this.call_params = params;
    }

    @Override
    public int getSize() {
        return 0;
    }

    public int getStatSize() {
        return subprogram.getSize();
    }

    @Override
    public void pass1() {
    }

    // this is macro expansion ! can be called only in MacroCallPseudo class
    // call parameters have to be set
    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        createNewEnvironment(env);

        subprogram.pass1(newEnv);

        // check of call_params
        if (call_params == null) {
            throw new UnknownMacroParametersException(line, column, mnemo);
        }
        if (call_params.size() != params.size()) {
            throw new InvalidMacroParamsCountException(line, column, mnemo, params.size(), call_params.size());
        }
        // create/rewrite symbols => parameters as equ pseudo instructions
        for (int i = 0; i < params.size(); i++) {
            newEnv.addConstant(new PseudoEQU(params.get(i),
                call_params.get(i), line, column));
        }
        return subprogram.pass2(newEnv, addr_start);
    }

    private void createNewEnvironment(Namespace env) {
        newEnv = new Namespace(env.getInputFile().getAbsolutePath());
        env.copyTo(newEnv);
        for (String param : params) {
            newEnv.removeAllDefinitions(param);
        }
    }

    @Override
    public void generateCode(IntelHEX hex) throws Exception {
        subprogram.pass4(hex, newEnv);
    }
}
