/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compilers.ssem;

import net.emustudio.plugins.compilers.ssem.tree.Instruction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class CodeGeneratorTest {

    private SeekableByteArrayOutputStream out;
    private CodeGenerator codeGenerator;

    @Before
    public void setUp() throws Exception {
        out = new SeekableByteArrayOutputStream(32);
        codeGenerator = new CodeGenerator(out);
    }

    @After
    public void tearDown() throws Exception {
        codeGenerator.close();
    }

    @Test
    public void testCMP() throws Exception {
        codeGenerator.visit(Instruction.cmp());

        assertArrayEquals(new byte[]{0, Instruction.CMP, 0, 0}, out.toArray());
    }

    @Test
    public void testSTP() throws Exception {
        codeGenerator.visit(Instruction.stp());

        assertArrayEquals(new byte[]{0, Instruction.STP, 0, 0}, out.toArray());
    }

    @Test
    public void testJMP() throws Exception {
        codeGenerator.visit(Instruction.jmp((byte) 6));

        assertArrayEquals(new byte[]{0x60, Instruction.JMP, 0, 0}, out.toArray());
    }

    @Test
    public void testJRP() throws Exception {
        codeGenerator.visit(Instruction.jrp((byte) 23));

        assertArrayEquals(new byte[]{(byte) 0xE8, Instruction.JRP, 0, 0}, out.toArray());
    }

    @Test
    public void testLDN() throws Exception {
        codeGenerator.visit(Instruction.ldn((byte) 12));

        assertArrayEquals(new byte[]{(byte) 0x30, Instruction.LDN, 0, 0}, out.toArray());
    }

    @Test
    public void testSTO() throws Exception {
        codeGenerator.visit(Instruction.sto((byte) 30));

        assertArrayEquals(new byte[]{(byte) 0x78, Instruction.STO, 0, 0}, out.toArray());
    }

    @Test
    public void testSUB() throws Exception {
        codeGenerator.visit(Instruction.sub((byte) 18));

        assertArrayEquals(new byte[]{(byte) 0x48, Instruction.SUB, 0, 0}, out.toArray());
    }

    private static class SeekableByteArrayOutputStream extends SeekableOutputStream {
        private final byte[] array;
        private int pos;
        private int length;

        public SeekableByteArrayOutputStream(int count) {
            this.array = new byte[count];
        }

        @Override
        public void seek(int position) throws IOException {
            length = Math.max(position, pos);
            pos = position;
        }

        @Override
        public void write(int b) throws IOException {
            array[pos] = (byte) b;
            pos++;
            length = Math.max(pos, length);
        }

        public byte[] toArray() {
            byte[] tmp = new byte[length];
            System.arraycopy(array, 0, tmp, 0, length);
            return tmp;
        }
    }
}
