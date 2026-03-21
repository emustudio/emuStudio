/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrCB;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrXDCB;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoVar;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.CompileError.ERROR_VALUE_OUT_OF_BOUNDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectExprsInOpcodeVisitorTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");

    @Test
    public void testRstSetsYFromExpression() {
        // RST 0x10 => y = 0x10/8 = 2
        Program program = new Program("");
        Instr rst = new Instr(POSITION, OPCODE_RST, 3, 0, 7);
        rst.setSizeBytes(1);
        rst.addChild(new Evaluated(POSITION, 0x10));
        program.addChild(rst);

        new CollectExprsInOpcodeVisitor().visit(program);
        assertTrue(program.env().hasNoErrors());
        // Evaluated child should be removed
        assertEquals(0, rst.getChildren().size());
    }

    @Test
    public void testRstInvalidValueReportsError() {
        // RST 14 is not in allowed set {0, 8, 16, 24, 32, 40, 48, 56}
        Program program = new Program("");
        Instr rst = new Instr(POSITION, OPCODE_RST, 3, 0, 7);
        rst.setSizeBytes(1);
        rst.addChild(new Evaluated(POSITION, 14));
        program.addChild(rst);

        new CollectExprsInOpcodeVisitor().visit(program);
        assertTrue(program.env().hasError(ERROR_VALUE_OUT_OF_BOUNDS));
    }

    @Test
    public void testCBbitSetsY() {
        // BIT 5, b => y should be set to 5
        Program program = new Program("");
        InstrCB bit = new InstrCB(POSITION, OPCODE_BIT, 0, 0);
        bit.setSizeBytes(2);
        bit.addChild(new Evaluated(POSITION, 5));
        program.addChild(bit);

        new CollectExprsInOpcodeVisitor().visit(program);
        assertTrue(program.env().hasNoErrors());
        assertEquals(0, bit.getChildren().size());
    }

    @Test
    public void testCBresSetsY() {
        // RES 3, c => y should be set to 3
        Program program = new Program("");
        InstrCB res = new InstrCB(POSITION, OPCODE_RES, 0, 1);
        res.setSizeBytes(2);
        res.addChild(new Evaluated(POSITION, 3));
        program.addChild(res);

        new CollectExprsInOpcodeVisitor().visit(program);
        assertTrue(program.env().hasNoErrors());
        assertEquals(0, res.getChildren().size());
    }

    @Test
    public void testCBsetSetsY() {
        // SET 7, d => y should be set to 7
        Program program = new Program("");
        InstrCB set = new InstrCB(POSITION, OPCODE_SET, 0, 2);
        set.setSizeBytes(2);
        set.addChild(new Evaluated(POSITION, 7));
        program.addChild(set);

        new CollectExprsInOpcodeVisitor().visit(program);
        assertTrue(program.env().hasNoErrors());
        assertEquals(0, set.getChildren().size());
    }

    @Test
    public void testXDCBbitSetsY() {
        // BIT 5, (IX+d) => y should be set to 5
        Program program = new Program("");
        InstrXDCB bit = new InstrXDCB(POSITION, OPCODE_BIT, 0xDD, 0, 6);
        bit.setSizeBytes(4);
        bit.addChild(new Evaluated(POSITION, 5));
        bit.addChild(new Evaluated(POSITION, 0x20).setSizeBytes(1));
        program.addChild(bit);

        new CollectExprsInOpcodeVisitor().visit(program);
        assertTrue(program.env().hasNoErrors());
        // Only the displacement Evaluated should remain as child
        assertEquals(1, bit.getChildren().size());
    }

    @Test
    public void testXDCBresSetsY() {
        Program program = new Program("");
        InstrXDCB res = new InstrXDCB(POSITION, OPCODE_RES, 0xFD, 0, 6);
        res.setSizeBytes(4);
        res.addChild(new Evaluated(POSITION, 6));
        res.addChild(new Evaluated(POSITION, 0x10).setSizeBytes(1));
        program.addChild(res);

        new CollectExprsInOpcodeVisitor().visit(program);
        assertTrue(program.env().hasNoErrors());
        assertEquals(1, res.getChildren().size());
    }

    @Test
    public void testCBrlcDoesNotSetY() {
        // RLC b - no bit expression, should not change y
        Program program = new Program("");
        InstrCB rlc = new InstrCB(POSITION, OPCODE_RLC, 0, 0);
        rlc.setSizeBytes(2);
        program.addChild(rlc);

        new CollectExprsInOpcodeVisitor().visit(program);
        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testVarNodeIsRemoved() {
        Program program = new Program("");
        PseudoVar var = new PseudoVar(POSITION, "x");
        program.addChild(var);
        program.addChild(new Instr(POSITION, OPCODE_NOP, 0, 0, 0).setSizeBytes(1));

        new CollectExprsInOpcodeVisitor().visit(program);
        // var should be removed, only nop remains
        assertEquals(1, program.getChildren().size());
    }
}

