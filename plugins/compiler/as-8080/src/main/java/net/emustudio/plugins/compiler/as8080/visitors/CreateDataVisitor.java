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

import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import org.antlr.v4.runtime.Token;

public class CreateDataVisitor extends As8080ParserBaseVisitor<Node> {

    @Override
    public Node visitDataDB(DataDBContext ctx) {
        Token start = ctx.getStart();
        DataDB db = new DataDB(start.getLine(), start.getCharPositionInLine());

        for (RDBdataContext next : ctx.rDBdata()) {
            if (next.expr != null) {
                db.addChild(CreateVisitors.expr.visit(next.expr));
            } else if (next.instr != null) {
                db.addChild(CreateVisitors.instr.visit(next.instr));
            }
        }
        return db;
    }

    @Override
    public Node visitDataDW(DataDWContext ctx) {
        Token start = ctx.getStart();
        DataDW dw = new DataDW(start.getLine(), start.getCharPositionInLine());

        for (RDWdataContext next : ctx.rDWdata()) {
            if (next.expr != null) {
                dw.addChild(CreateVisitors.expr.visit(next.expr));
            }
        }

        return dw;
    }

    @Override
    public Node visitDataDS(DataDSContext ctx) {
        Token start = ctx.getStart();
        DataDS ds = new DataDS(start.getLine(), start.getCharPositionInLine());
        ds.addChild(CreateVisitors.expr.visit(ctx.data));
        return ds;
    }
}
