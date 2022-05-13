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
import net.emustudio.plugins.compiler.as8080.ast.Program;

import java.util.Objects;

/**
 * The visitor creates internal AST (= "Program") of the parse tree.
 */
public class CreateProgramVisitor extends As8080ParserBaseVisitor<Program> {
    private final Program program;

    public CreateProgramVisitor(Program program) {
        this.program = Objects.requireNonNull(program);
    }

    @Override
    public Program visitRLine(As8080Parser.RLineContext ctx) {
        Node statement = CreateVisitors.line.visitRLine(ctx);
        if (statement != null) {
            program.addChild(statement);
        }
        return program;
    }
}
