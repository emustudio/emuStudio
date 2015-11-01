/*
 * Copyright (C) 2008-2015 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.zilogZ80.assembler.tree;

import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;

public class Arithmetic extends Expression {

    public static final int ADD = 0;
    public static final int MINUS = 1;
    public static final int MUL = 2;
    public static final int DIV = 3;
    public static final int MOD = 4;
    public static final int SHL = 5;
    public static final int SHR = 6;
    public static final int AND = 7;
    public static final int OR = 8;
    public static final int XOR = 9;
    public static final int EQ = 10;
    public static final int LE = 11;
    public static final int GE = 12;
    public static final int LESS = 13;
    public static final int GREATER = 14;
    public static final int NOT = 15;
    private Expression left;
    private Expression right;
    private int operator;

    /**
     * Creates a new instance of Arithmetic
     */
    public Arithmetic(Expression left, Expression right, final int operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    /// compile time ///
    @Override
    public int eval(Namespace env, int curr_addr) throws Exception {
        int lv = left.eval(env, curr_addr);
        int rv = 0;
        if (right != null) {
            rv = right.eval(env, curr_addr);
        }

        this.value = 0;
        switch (operator) {
            case OR:
                this.value = lv | rv;
                break;
            case XOR:
                this.value = lv ^ rv;
                break;
            case AND:
                this.value = lv & rv;
                break;
            case NOT:
                this.value = ~lv;
                break;
            case ADD:
                this.value = lv + rv;
                break;
            case MINUS:
                this.value = (right == null) ? -lv : (lv - rv);
                break;
            case MUL:
                this.value = lv * rv;
                break;
            case DIV:
                this.value = lv / rv;
                break;
            case MOD:
                this.value = lv % rv;
                break;
            case SHL:
                this.value = lv << rv;
                break;
            case SHR:
                this.value = lv >>> rv;
                break;
            case EQ:
                this.value = (lv == rv) ? 1 : 0;
                break;
            case LE:
                this.value = (lv <= rv) ? 1 : 0;
                break;
            case GE:
                this.value = (lv >= rv) ? 1 : 0;
                break;
            case LESS:
                this.value = (lv < rv) ? 1 : 0;
                break;
            case GREATER:
                this.value = (lv > rv) ? 1 : 0;
                break;
        }
        return this.value;
    }
}
