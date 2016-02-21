/*
 * Copyright (C) 2016 Peter Jakubčo
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
package net.sf.emustudio.ssem.assembler;

import net.sf.emustudio.ssem.assembler.tree.Instruction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertArrayEquals;

public class CodeGeneratorTest {

    private ByteArrayOutputStream out;
    private CodeGenerator codeGenerator;

    @Before
    public void setUp() throws Exception {
        out = new ByteArrayOutputStream(32);
        codeGenerator = new CodeGenerator(out);
    }

    @After
    public void tearDown() throws Exception {
        codeGenerator.close();
    }

    @Test
    public void testCMP() throws Exception {
        codeGenerator.visit(Instruction.cmp());

        assertArrayEquals(new byte[] {0,Instruction.CMP,0,0}, out.toByteArray());
    }

    @Test
    public void testSTP() throws Exception {
        codeGenerator.visit(Instruction.stp());

        assertArrayEquals(new byte[] {0,Instruction.STP,0,0}, out.toByteArray());
    }

    @Test
    public void testJMP() throws Exception {
        codeGenerator.visit(Instruction.jmp((byte)6));

        assertArrayEquals(new byte[] {0x60,Instruction.JMP,0,0}, out.toByteArray());
    }

    @Test
    public void testJRP() throws Exception {
        codeGenerator.visit(Instruction.jrp((byte)23));

        assertArrayEquals(new byte[] {(byte)0xE8,Instruction.JRP,0,0}, out.toByteArray());
    }

    @Test
    public void testLDN() throws Exception {
        codeGenerator.visit(Instruction.ldn((byte)12));

        assertArrayEquals(new byte[] {(byte)0x30,Instruction.LDN,0,0}, out.toByteArray());
    }

    @Test
    public void testSTO() throws Exception {
        codeGenerator.visit(Instruction.sto((byte)30));

        assertArrayEquals(new byte[] {(byte)0x78,Instruction.STO,0,0}, out.toByteArray());
    }

    @Test
    public void testSUB() throws Exception {
        codeGenerator.visit(Instruction.sub((byte)18));

        assertArrayEquals(new byte[] {(byte)0x48,Instruction.SUB,0,0}, out.toByteArray());
    }

}
