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
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegPairExpr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoOrg;

import static net.emustudio.plugins.compiler.as8080.CompileError.expressionIsBiggerThanExpected;

/**
 * Checks proper sizes of evaluated nodes
 */
public class CheckExprSizesVisitor extends NodeVisitor {
    private int expectedBytes;

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
    public void visit(InstrExpr node) {
        expectedBytes = node.getExprSizeBytes();
        if (expectedBytes == 1 && node.getChildrenCount() > 1) {
            error(expressionIsBiggerThanExpected(node, expectedBytes, node.getChildrenCount()));
        }
        visitChildren(node);
    }

    @Override
    public void visit(InstrRegExpr node) {
        expectedBytes = 1;
        if (node.getChildrenCount() > 1) {
            error(expressionIsBiggerThanExpected(node, expectedBytes, node.getChildrenCount()));
        }
        visitChildren(node);
    }

    @Override
    public void visit(InstrRegPairExpr node) {
        expectedBytes = 2;
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
        if (expectedBytes > 0) {
            int value = node.value < 0 ? ((~node.value) * 2) : node.value;

            int wasBits = (int) Math.floor(Math.log10(Math.abs(value)) / Math.log10(2)) + 1;
            int wasBytes = (int) Math.ceil(wasBits / 8.0);

            if (wasBytes > expectedBytes) {
                error(expressionIsBiggerThanExpected(node, expectedBytes, wasBytes));
            }
        }
    }
}
