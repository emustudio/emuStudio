/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoLabel;

import java.util.Objects;

public class CreateLineVisitor extends As8080ParserBaseVisitor<Node> {
    private final String sourceFileName;

    public CreateLineVisitor(String sourceFileName) {
        this.sourceFileName = Objects.requireNonNull(sourceFileName);
    }


    @Override
    public Node visitRLine(As8080Parser.RLineContext ctx) {
        Node label = null;
        if (ctx.label != null) {
            label = new PseudoLabel(sourceFileName, ctx.label);
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
    public Node visitRStatement(As8080Parser.RStatementContext ctx) {
        if (ctx.instr != null) {
            return instrVisitor().visit(ctx.instr);
        } else if (ctx.data != null) {
            return dataVisitor().visit(ctx.data);
        } else if (ctx.pseudo != null) {
            return pseudoVisitor().visit(ctx.pseudo);
        }
        throw new IllegalStateException("No statement defined!");
    }

    private CreateInstrVisitor instrVisitor() {
        return CreateVisitors.instr(sourceFileName);
    }

    ;

    private CreateDataVisitor dataVisitor() {
        return CreateVisitors.data(sourceFileName);
    }

    private CreatePseudoVisitor pseudoVisitor() {
        return CreateVisitors.pseudo(sourceFileName);
    }
}
