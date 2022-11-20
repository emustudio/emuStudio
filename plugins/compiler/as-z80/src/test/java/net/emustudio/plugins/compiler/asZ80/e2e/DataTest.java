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
package net.emustudio.plugins.compiler.asZ80.e2e;

import org.junit.Test;

public class DataTest extends AbstractCompilerTest {

    @Test
    public void testDBwithNegativeValueWorks() {
        compile(
            "db -1"
        );
        assertProgram(
            0xFF, 0
        );
    }

    @Test(expected = Exception.class)
    public void testDBwithNegativeValueHigherLowerThanMinus127doesNotWork() {
        compile(
            "db -1299"
        );
    }

    @Test
    public void testDBallocatesOneByte() {
        compile(
            "db 10\nld a,b\n"
        );
        assertProgram(
            10, 0x78
        );
    }

    @Test(expected = Exception.class)
    public void testDBbiggerThan255DoesNotWork() {
        compile(
            "db 256\n"
        );
    }

    @Test
    public void testDBseveralBytesWork() {
        compile(
            "db 255,1,2\n"
        );
        assertProgram(
            0xFF, 1, 2
        );
    }

    @Test
    public void testDBWithInstruction() {
        compile(
            "db inc A\n"
        );

        assertProgram(
            0x3C
        );
    }

    @Test
    public void testDBliteral() {
        compile(
            "db 'if'\n"
        );

        assertProgram(
            'i', 'f'
        );
    }

    @Test
    public void testDBshortLiteral() {
        compile(
            "db 'i'\n"
        );

        assertProgram(
            'i'
        );
    }

    @Test
    public void testDWwithNegativeValueWorks() {
        compile(
            "dw -1"
        );
        assertProgram(
            0xFF, 0xFF, 0
        );
    }

    @Test(expected = Exception.class)
    public void testDWwithNegativeValueHigherLowerThanMinus3768doesNotWork() {
        compile(
            "dw -32769"
        );
    }

    @Test
    public void testDWallocatesTwoBytesInLittleEndian() {
        compile(
            "dw 10\nld a,b\n"
        );
        assertProgram(
            10, 0, 0x78
        );
    }

    @Test
    public void testDWseveralValuesWork() {
        compile(
            "dw 10,4\nld a,b\n"
        );
        assertProgram(
            10, 0, 4, 0, 0x78
        );
    }

    @Test(expected = Exception.class)
    public void testDWmoreThanFFFFdoesNotWork() {
        compile(
            "dw 10000h\nld a,b\n"
        );
    }

    @Test(expected = Exception.class)
    public void testDW_ValueTooBig() {
        compile(
            "org 0FFFFh\n"
                + "rrca\n"
                + "test:\n"
                + "dw test"
        );
    }

    @Test(expected = Exception.class)
    public void testDSwithNegativeValueDoesNotWork() {
        compile(
            "ds -1"
        );
    }

    @Test
    public void testDSbreaksPreviousMemoryContent() {
        memoryStub.write(0, (byte) 0x10);
        memoryStub.write(1, (byte) 0x11);

        compile(
            "ds 2\n" + "now: ld a,b\n"
        );

        assertProgram(
            0x0, 0x0, 0x78
        );
    }

    @Test
    public void testJumpBackwardWithDSamong() {
        compile(
            "ds 2\n" +
                "now: ld a,b\n" +
                "ds 2\n" +
                "cp 'C'\n" +
                "jp z, now\n" +
                "ler: ld (HL), a"
        );

        assertProgram(
            0, 0, 0x78, 0, 0, 0xFE, 0x43, 0xCA, 0x02, 0x00, 0x77
        );
    }

    @Test
    public void testDbOrdering() {
        compile("db 186, \"Hello\", 186, 10, 13");
        assertProgram(
            186, 'H', 'e', 'l', 'l', 'o', 186, 10, 13
        );
    }
}
