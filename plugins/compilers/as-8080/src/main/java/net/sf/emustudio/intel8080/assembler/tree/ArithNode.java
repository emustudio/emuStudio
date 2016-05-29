/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.intel8080.assembler.tree;

import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.treeAbstract.ExprNode;

public class ArithNode extends ExprNode {
    private ExprNode left;
    private ExprNode right;
    private String operator;
    
    public ArithNode(ExprNode left, ExprNode right, String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }
    
    @Override
    public int eval(CompileEnv env, int curr_addr) throws Exception {
        int lv = left.eval(env,curr_addr);
        int rv = 0;
        if (right != null) {
            rv = right.eval(env, curr_addr);
        }
 
        this.value = 0;
        switch (operator) {
            case "or":
                this.value = lv | rv;
                break;
            case "xor":
                this.value = lv ^ rv;
                break;
            case "and":
                this.value = lv & rv;
                break;
            case "not":
                this.value = ~lv;
                break;
            case "+":
                this.value = lv + rv;
                break;
            case "-":
                this.value = lv - rv;
                break;
            case "*":
                this.value = lv * rv;
                break;
            case "/":
                this.value = lv / rv;
                break;
            case "mod":
                this.value = lv % rv;
                break;
            case "shr":
                this.value = lv >>> rv;
                break;
            case "shl":
                this.value = lv << rv;
                break;
            case "=":
                this.value = (lv == rv) ? 1 : 0;
                break;
        }
        
        return this.value;
    }

}
