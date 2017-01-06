/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.tree;

import emulib.runtime.HEXFileManager;
import net.sf.emustudio.zilogZ80.assembler.exceptions.InvalidMacroParamsCountException;
import net.sf.emustudio.zilogZ80.assembler.exceptions.UnknownMacroParametersException;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Pseudo;

import java.util.ArrayList;
import java.util.List;

public class PseudoMACRO extends Pseudo {
    private final List<String> params; // macro parameters
    private List<Expression> call_params; // concrete parameters, they can change
    private final Program subprogram;
    private final String mnemo;
    // for generateCode
    private Namespace newEnv;

    public PseudoMACRO(String name, ArrayList<String> params, Program s, int line,
            int column) {
        super(line, column);
        this.mnemo = name;
        if (params == null) {
            this.params = new ArrayList<>();
        } else {
            this.params = params;
        }
        this.subprogram = s;
    }

    public String getName() {
        return mnemo;
    }

    public void setCallParams(ArrayList<Expression> params) {
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
    public void pass1() throws Exception {
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
    public void generateCode(HEXFileManager hex) throws Exception {
        subprogram.pass4(hex, newEnv);
    }
}
