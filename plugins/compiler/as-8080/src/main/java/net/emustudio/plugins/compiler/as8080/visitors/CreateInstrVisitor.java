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
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;

public class CreateInstrVisitor extends As8080ParserBaseVisitor<Node> {

    @Override
    public Node visitInstrNoArgs(As8080Parser.InstrNoArgsContext ctx) {
        return new InstrNoArgs(ctx.opcode);
    }

    @Override
    public Node visitInstrReg(As8080Parser.InstrRegContext ctx) {
        return new InstrReg(ctx.opcode, ctx.reg.getStart());
    }

    @Override
    public Node visitInstrRegReg(As8080Parser.InstrRegRegContext ctx) {
        return new InstrRegReg(ctx.opcode, ctx.dst.getStart(), ctx.src.getStart());
    }

    @Override
    public Node visitInstrRegPair(As8080Parser.InstrRegPairContext ctx) {
        return new InstrRegPair(ctx.opcode, ctx.regpair);
    }

    @Override
    public Node visitInstrRegPairExpr(As8080Parser.InstrRegPairExprContext ctx) {
        InstrRegPairExpr instr = new InstrRegPairExpr(ctx.opcode, ctx.regpair);
        instr.addChild(CreateVisitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstrRegExpr(As8080Parser.InstrRegExprContext ctx) {
        InstrRegExpr instr = new InstrRegExpr(ctx.opcode, ctx.reg.getStart());
        instr.addChild(CreateVisitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstrExpr(As8080Parser.InstrExprContext ctx) {
        InstrExpr instr = new InstrExpr(ctx.opcode);
        instr.addChild(CreateVisitors.expr.visit(ctx.expr));
        return instr;
    }

    @Override
    public Node visitInstr8bitExpr(As8080Parser.Instr8bitExprContext ctx) {
        InstrExpr instr = new InstrExpr(ctx.opcode);
        instr.addChild(CreateVisitors.expr.visit(ctx.expr));
        return instr;
    }
}
