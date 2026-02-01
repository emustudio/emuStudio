/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class CreateDataVisitor extends AsZ80ParserBaseVisitor<Node> {
    private final String sourceFileName;

    public CreateDataVisitor(String sourceFileName) {
        this.sourceFileName = Objects.requireNonNull(sourceFileName);
    }


    @Override
    public Node visitDataDB(DataDBContext ctx) {
        Token start = ctx.getStart();
        DataDB db = new DataDB(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));

        for (RDBdataContext next : ctx.rDBdata()) {
            if (next.expr != null) {
                db.addChild(exprVisitor().visit(next.expr));
            } else if (next.instr != null) {
                db.addChild(instrVisitor().visit(next.instr));
            }
        }
        return db;
    }

    @Override
    public Node visitDataDW(DataDWContext ctx) {
        Token start = ctx.getStart();
        DataDW dw = new DataDW(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));

        for (RDWdataContext next : ctx.rDWdata()) {
            if (next.expr != null) {
                dw.addChild(exprVisitor().visit(next.expr));
            }
        }

        return dw;
    }

    @Override
    public Node visitDataDS(DataDSContext ctx) {
        Token start = ctx.getStart();
        DataDS ds = new DataDS(new SourceCodePosition(start.getLine(), start.getCharPositionInLine(), sourceFileName));
        ds.addChild(exprVisitor().visit(ctx.data));
        return ds;
    }

    private CreateExprVisitor exprVisitor() {
        return CreateVisitors.expr(sourceFileName);
    };

    private CreateInstrVisitor instrVisitor() {
        return CreateVisitors.instr(sourceFileName);
    };
}
