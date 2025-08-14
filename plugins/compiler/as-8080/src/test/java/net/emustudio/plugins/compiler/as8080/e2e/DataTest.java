/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.e2e;

import org.junit.Test;

public class DataTest extends AbstractCompilerTest {

    @Test
    public void testDBwithNegativeValueWorks() {
        compile("db -1");
        assertProgram(0xFF, 0);
    }

    @Test(expected = Exception.class)
    public void testDBwithNegativeValueHigherLowerThanMinus127doesNotWork() {
        compile("db -1299");
    }

    @Test
    public void testDBallocatesOneByte() {
        compile("db 10\nmov a,b\n");
        assertProgram(10, 0x78);
    }

    @Test(expected = Exception.class)
    public void testDBbiggerThan255DoesNotWork() {
        compile("db 256\n");
    }

    @Test
    public void testDBseveralBytesWork() {
        compile("db 255,1,2\n");
        assertProgram(0xFF, 1, 2);
    }

    @Test
    public void testDBWithInstruction() {
        compile("db inr A\n");
        assertProgram(0x3C);
    }

    @Test
    public void testDBliteral() {
        compile("db 'if'\n");
        assertProgram('i', 'f');
    }

    @Test
    public void testDBshortLiteral() {
        compile("db 'i'\n");
        assertProgram('i');
    }

    @Test
    public void testDWwithNegativeValueWorks() {
        compile("dw -1");
        assertProgram(0xFF, 0xFF, 0);
    }

    @Test(expected = Exception.class)
    public void testDWwithNegativeValueHigherLowerThanMinus3768doesNotWork() {
        compile("dw -32769");
    }

    @Test
    public void testDWallocatesTwoBytesInLittleEndian() {
        compile("dw 10\nmov a,b\n");
        assertProgram(10, 0, 0x78);
    }

    @Test
    public void testDWseveralValuesWork() {
        compile(
                "dw 10,4\nmov a,b\n"
        );
        assertProgram(
                10, 0, 4, 0, 0x78
        );
    }

    @Test(expected = Exception.class)
    public void testDWmoreThanFFFFdoesNotWork() {
        compile("dw 10000h\nmov a,b\n");
    }

    @Test(expected = Exception.class)
    public void testDW_ValueTooBig() {
        compile(
                "org 0FFFFh\n"
                        + "rrc\n"
                        + "test:\n"
                        + "dw test"
        );
    }

    @Test(expected = Exception.class)
    public void testDSwithNegativeValueDoesNotWork() {
        compile("ds -1");
    }

    @Test
    public void testDSbreaksPreviousMemoryContent() {
        memoryStub.write(0, (byte) 0x10);
        memoryStub.write(1, (byte) 0x11);

        compile("ds 2\n" + "now: mov a,b\n");
        assertProgram(0x0, 0x0, 0x78);
    }

    @Test
    public void testJumpBackwardWithDSamong() {
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
    public void testDbOrdering() {
        compile("db 186, \"Hello\", 186, 10, 13");
        assertProgram(186, 'H', 'e', 'l', 'l', 'o', 186, 10, 13);
    }
}
