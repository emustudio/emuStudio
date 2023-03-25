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
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrCB;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrXDCB;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoVar;

import java.util.Set;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.CompileError.valueOutOfBounds;

public class CollectExprsInOpcodeVisitor extends NodeVisitor {
    private final static Set<Integer> allowedRstValues = Set.of(
            0, 0x8, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38
    );

    @Override
    public void visit(Instr node) {
        if (node.opcode == OPCODE_RST) {
            node
                    .collectChild(Evaluated.class)
                    .map(e -> {
                        e.remove();
                        return e.value;
                    })
                    .filter(allowedRstValues::contains)
                    .map(v -> v / 8)
                    .ifPresentOrElse(node::setY, () -> error(valueOutOfBounds(node, allowedRstValues)));
        }
    }

    @Override
    public void visit(InstrCB node) {
        // SET, RES, BIT
        switch (node.opcode) {
            case OPCODE_SET:
            case OPCODE_RES:
            case OPCODE_BIT:
                node
                        .collectChild(Evaluated.class)
                        .map(e -> {
                            e.remove();
                            return e.value;
                        })
                        .ifPresent(node::setY);
        }
    }

    @Override
    public void visit(InstrXDCB node) {
        // SET, RES, BIT
        switch (node.opcode) {
            case OPCODE_SET:
            case OPCODE_RES:
            case OPCODE_BIT:
                node
                        .collectChild(Evaluated.class)
                        .map(e -> {
                            e.remove();
                            return e.value;
                        })
                        .ifPresent(node::setY);
        }
    }

    @Override
    public void visit(PseudoVar node) {
        // remove useless VAR node before it gets evaluated in code generator...
        node.remove();
    }
}
