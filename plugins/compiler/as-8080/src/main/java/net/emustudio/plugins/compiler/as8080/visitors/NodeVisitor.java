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

import net.emustudio.plugins.compiler.as8080.CompileError;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.expr.*;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import net.emustudio.plugins.compiler.as8080.exceptions.FatalError;

import java.util.List;

public class NodeVisitor {
    protected NameSpace env;

    protected void error(CompileError error) {
        env.error(error);
    }

    protected void fatalError(CompileError error) {
        FatalError.now(error);
    }

    public void visit(Node node) {
        visitChildren(node);
    }

    public void visit(Program node) {
        if (env == null) {
            this.env = node.env();
        }
        visitChildren(node);
    }

    public void visit(DataDB node) {
        visitChildren(node);
    }

    public void visit(DataDW node) {
        visitChildren(node);
    }

    public void visit(DataDS node) {
        visitChildren(node);
    }

    public void visit(ExprCurrentAddress node) {
        visitChildren(node);
    }

    public void visit(ExprInfix node) {
        visitChildren(node);
    }

    public void visit(ExprId node) {
        visitChildren(node);
    }

    public void visit(ExprString node) {
        visitChildren(node);
    }

    public void visit(ExprNumber node) {
        visitChildren(node);
    }

    public void visit(ExprUnary node) {
        visitChildren(node);
    }

    public void visit(InstrExpr node) {
        visitChildren(node);
    }

    public void visit(InstrNoArgs node) {
        visitChildren(node);
    }

    public void visit(InstrReg node) {
        visitChildren(node);
    }

    public void visit(InstrRegExpr node) {
        visitChildren(node);
    }

    public void visit(InstrRegPair node) {
        visitChildren(node);
    }

    public void visit(InstrRegPairExpr node) {
        visitChildren(node);
    }

    public void visit(InstrRegReg node) {
        visitChildren(node);
    }

    public void visit(PseudoEqu node) {
        visitChildren(node);
    }

    public void visit(PseudoIf node) {
        visitChildren(node);
    }

    public void visit(PseudoIfExpression node) {
        visitChildren(node);
    }

    public void visit(PseudoInclude node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroCall node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroArgument node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroParameter node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroDef node) {
        visitChildren(node);
    }

    public void visit(PseudoOrg node) {
        visitChildren(node);
    }

    public void visit(PseudoSet node) {
        visitChildren(node);
    }

    public void visit(PseudoLabel node) {
        visitChildren(node);
    }

    public void visit(Evaluated node) {
        visitChildren(node);
    }

    protected void visitChildren(Node node) {
        for (Node child : node.getChildren()) {
            child.accept(this);
        }
    }

    protected void visitChildren(Node node, int skipFirstN) {
        List<Node> children = node.getChildren();
        for (int i = skipFirstN; i < children.size(); i++) {
            children.get(i).accept(this);
        }
    }
}
