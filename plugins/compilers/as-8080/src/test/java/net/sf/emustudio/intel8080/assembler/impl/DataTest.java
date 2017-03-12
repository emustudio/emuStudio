/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.intel8080.assembler.impl;

import org.junit.Test;

public class DataTest extends AbstractCompilerTest {

    @Test
    public void testDBwithNegativeValueWorks() throws Exception {
        compile(
            "db -1"
        );
        assertProgram(
            0xFF, 0
        );
    }

    @Test
    public void testDBwithNegativeValueHigherLowerThanMinus127doesNotWork() throws Exception {
        compile(
            "db -1299"
        );

        assertError();
    }

    @Test
    public void testDBallocatesOneByte() throws Exception {
        compile(
            "db 10\nmov a,b\n"
        );
        assertProgram(
            10, 0x78
        );
    }

    @Test
    public void testDBbiggerThan255DoesNotWork() throws Exception {
        compile(
            "db 256\n"
        );

        assertError();
    }

    @Test
    public void testDBseveralBytesWork() throws Exception {
        compile(
            "db 255,1,2\n"
        );
        assertProgram(
            0xFF, 1, 2
        );
    }

    @Test
    public void testDBWithInstruction() throws Exception {
        compile(
            "db inr A\n"
        );

        assertProgram(
            0x3C
        );
    }

    @Test
    public void testDBliteral() throws Exception {
        compile(
            "db 'if'\n"
        );

        assertProgram(
            'i', 'f'
        );
    }

    @Test
    public void testDBshortLiteral() throws Exception {
        compile(
            "db 'i'\n"
        );

        assertProgram(
            'i'
        );
    }

    @Test
    public void testDWwithNegativeValueWorks() throws Exception {
        compile(
            "dw -1"
        );
        assertProgram(
            0xFF, 0xFF, 0
        );
    }

    @Test
    public void testDWwithNegativeValueHigherLowerThanMinus3768doesNotWork() throws Exception {
        compile(
            "dw -32769"
        );

        assertError();
    }

    @Test
    public void testDWallocatesTwoBytesInLittleEndian() throws Exception {
        compile(
            "dw 10\nmov a,b\n"
        );
        assertProgram(
            10, 0, 0x78
        );
    }

    @Test
    public void testDWseveralValuesWork() throws Exception {
        compile(
            "dw 10,4\nmov a,b\n"
        );
        assertProgram(
            10, 0, 4, 0, 0x78
        );
    }

    @Test
    public void testDWmoreThanFFFFdoesNotWork() throws Exception {
        compile(
            "dw 10000h\nmov a,b\n"
        );

        assertError();
    }

    @Test
    public void testDW_ValueTooBig() throws Exception {
        compile(
            "org 0FFFFh\n"
                + "rrc\n"
                + "test:\n"
                + "dw test"
        );

        assertError();
    }

    @Test
    public void testDSwithNegativeValueDoesNotWork() throws Exception {
        compile(
            "ds -1"
        );

        assertError();
    }

    @Test
    public void testDSbreaksPreviousMemoryContent() throws Exception {
        memoryStub.write(0, (short)0x10);
        memoryStub.write(1, (short) 0x11);

        compile(
            "ds 2\n" + "now: mov a,b\n"
        );

        assertProgram(
            0x0, 0x0, 0x78
        );
    }

    @Test
    public void testJumpBackwardWithDSamong() throws Exception {
        compile(
            "ds 2\n" +
                "now: mov a,b\n" +
                "ds 2\n" +
                "cpi 'C'\n" +
                "jz now\n" +
                "ler: mov m, a"
        );

        assertProgram(
            0, 0, 0x78, 0, 0, 0xFE, 0x43, 0xCA, 0x02, 0x00, 0x77
        );
    }

    @Test
    public void testDS_ValueTooBig() throws Exception {
        compile(
            "org 0FFh\n"
                + "rrc\n"
                + "test:\n"
                + "ds test"
        );

        assertError();
    }
}
