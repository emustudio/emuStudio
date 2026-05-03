/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;

import java.util.Objects;

public class CreateInstrVisitor extends As8080ParserBaseVisitor<Node> {
    private final String sourceFileName;

    public CreateInstrVisitor(String sourceFileName) {
        this.sourceFileName = Objects.requireNonNull(sourceFileName);
    }

    @Override
    public Node visitInstrNoArgs(As8080Parser.InstrNoArgsContext ctx) {
        return new InstrNoArgs(sourceFileName, ctx.opcode);
    }

    @Override
    public Node visitInstrReg(As8080Parser.InstrRegContext ctx) {
        return new InstrReg(sourceFileName, ctx.opcode, ctx.reg.getStart());
    }

    @Override
    public Node visitInstrRegReg(As8080Parser.InstrRegRegContext ctx) {
        return new InstrRegReg(sourceFileName, ctx.opcode, ctx.dst.getStart(), ctx.src.getStart());
    }

    @Override
    public Node visitInstrRegPair(As8080Parser.InstrRegPairContext ctx) {
        return new InstrRegPair(sourceFileName, ctx.opcode, ctx.regpair);
    }

    @Override
    public Node visitInstrRegPairExpr(As8080Parser.InstrRegPairExprContext ctx) {
        InstrRegPairExpr instr = new InstrRegPairExpr(sourceFileName, ctx.opcode, ctx.regpair);
        instr.addChild(exprVisitor().visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstrRegExpr(As8080Parser.InstrRegExprContext ctx) {
        InstrRegExpr instr = new InstrRegExpr(sourceFileName, ctx.opcode, ctx.reg.getStart());
        instr.addChild(exprVisitor().visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstrExpr(As8080Parser.InstrExprContext ctx) {
        InstrExpr instr = new InstrExpr(sourceFileName, ctx.opcode);
        instr.addChild(exprVisitor().visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstr8bitExpr(As8080Parser.Instr8bitExprContext ctx) {
        InstrExpr instr = new InstrExpr(sourceFileName, ctx.opcode);
        instr.addChild(exprVisitor().visit(ctx.expr));
        return instr;
    }

    private CreateExprVisitor exprVisitor() {
        return CreateVisitors.expr(sourceFileName);
    }
}
