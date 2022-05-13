/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoLabel;
import net.emustudio.plugins.compiler.asZ80.ast.Node;

public class CreateLineVisitor extends AsZ80ParserBaseVisitor<Node> {

    @Override
    public Node visitRLine(RLineContext ctx) {
        Node label = null;
        if (ctx.label != null) {
            label = new PseudoLabel(ctx.label);
        }
        Node statement = null;
        if (ctx.statement != null) {
            statement = visit(ctx.statement);
        }
        if (label != null) {
            if (statement != null) {
                label.addChild(statement);
            }
            return label;
        }
        return statement;
    }

    @Override
    public Node visitRStatement(RStatementContext ctx) {
        if (ctx.instr != null) {
            return CreateVisitors.instr.visit(ctx.instr);
        } else if (ctx.data != null) {
            return CreateVisitors.data.visit(ctx.data);
        } else if (ctx.pseudo != null) {
            return CreateVisitors.pseudo.visit(ctx.pseudo);
        }
        throw new IllegalStateException("No statement defined!");
    }
}
