/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoOrg;

import static net.emustudio.plugins.compiler.asZ80.CompileError.expressionIsBiggerThanExpected;
import static net.emustudio.plugins.compiler.asZ80.CompileError.valueOutOfBounds;

/**
 * Checks proper sizes of evaluated nodes
 */
public class CheckExprSizesVisitor extends NodeVisitor {
    private int expectedBytes;
    private boolean isRelative; // if we should treat Evaluated value as "relative address"
    private int currentAddress; // for computing relative address


    @Override
    public void visit(DataDB node) {
        expectedBytes = 1;
        visitChildren(node);
    }

    @Override
    public void visit(DataDW node) {
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(DataDS node) {
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(Instr node) {
        boolean oldIsRelative = isRelative;
        isRelative = node.hasRelativeAddress();
        currentAddress = node.getAddress();

        expectedBytes = isRelative ? 1 : 0;
        visitChildren(node);
        isRelative = oldIsRelative;
    }

    @Override
    public void visit(InstrCB node) {
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrED node) {
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrXD node) {
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrXDCB node) {
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(PseudoOrg node) {
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(PseudoMacroArgument node) {
        node.remove();
    }

    @Override
    public void visit(Evaluated node) {
        int value;
        if (isRelative && node.isAddress) {
            value = (node.value - currentAddress - 2);
        } else {
            value = node.value < 0 ? ((~node.value) * 2) : node.value;
        }
        if (expectedBytes > 0) {
            int wasBits = (int) Math.floor(Math.log10(Math.abs(value)) / Math.log10(2)) + 1;
            int wasBytes = (int) Math.ceil(wasBits / 8.0);

            if (wasBytes > expectedBytes) {
                error(expressionIsBiggerThanExpected(node, expectedBytes, wasBytes));
            }
        } else {
            node.getMaxValue().ifPresent(maxValue -> {
                if (value > maxValue) {
                    error(valueOutOfBounds(node, 0, maxValue));
                }
            });
        }
    }
}
