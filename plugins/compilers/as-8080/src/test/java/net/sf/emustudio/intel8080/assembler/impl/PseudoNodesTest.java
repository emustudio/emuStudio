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
package net.sf.emustudio.intel8080.assembler.impl;

import emulib.plugins.compiler.*;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PseudoNodesTest {
    private CompilerImpl compiler;
    private MemoryStub memoryStub;
    private int errorCode;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        memoryStub = new MemoryStub();

        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemoryContext(0, MemoryContext.class))
                .andReturn(memoryStub).anyTimes();
        replay(pool);

        compiler = new CompilerImpl(0L, pool);
        compiler.addCompilerListener(new emulib.plugins.compiler.Compiler.CompilerListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onMessage(Message message) {
                System.out.println(message);
            }

            @Override
            public void onFinish(int errorCode) {
                PseudoNodesTest.this.errorCode = errorCode;
            }
        });
    }

    private void compile(String content) throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);

        File outputFile = folder.newFile();
        compiler.compile(sourceFile.getAbsolutePath(), outputFile.getAbsolutePath());
    }

    private void assertProgram(int... bytes) {
        assertTrue(errorCode == 0);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(
                    String.format("%d. expected=%x, but was=%x",i, bytes[i], memoryStub.read(i)),
                    bytes[i], (int)memoryStub.read(i)
            );
        }
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
    public void testORG() throws Exception {
        compile(
                "org 2\n" +
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
    public void testORGwithJumpBackwards() throws Exception {
        compile(
                "sample:\n"
                        + "org 2\n"
                        + "jmp sample"
        );

        assertProgram(
                0, 0, 0xC3, 0, 0
        );
    }

    @Test
    public void testORGwithJumpForwards() throws Exception {
        compile(
                "jmp sample\n"
                        + "org 5\n"
                        + "sample:\n"
                + "mov a, b"
        );

        assertProgram(
                0xC3, 0x05, 0, 0, 0, 0x78
        );
    }

    @Test
    public void testORGdoesNotBreakPreviousMemoryContent() throws Exception {
        memoryStub.write(0, (short)0x10);
        memoryStub.write(1, (short) 0x11);

        compile(
                "org 2\n" + "now: mov a,b\n"
        );

        assertProgram(
                0x10, 0x11, 0x78
        );
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
    public void testLabelAsConstantWorks() throws Exception {
        compile(
                "here equ 0\n"
                        + "mov a,b\n"
                        + "jz here"
        );

        assertProgram(
                0x78, 0xCA, 0, 0
        );
    }

    @Test
    public void testConstantAsLabelWorks() throws Exception {
        compile(
                "here equ there\n"
                        + "there: mov a,b\n"
                        + "jz here"
        );

        assertProgram(
                0x78, 0xCA, 0, 0
        );
    }

    @Test
    public void testRecursiveConstantDefinitionsDoesNotWork() throws Exception {
        compile(
                "here equ there\n"
                        + "there equ here\n"
                        + "jz here"
        );
        assertFalse(errorCode == 0);
    }

    @Test
    public void testTwoSameLabelsDoNotWork() throws Exception {
        compile(
                "here:\nhere:\njz here"
        );
        assertFalse(errorCode == 0);
    }

    @Test
    public void testTwoSameConstantsDoNotWork() throws Exception {
        compile(
                "here equ 0\nhere equ 1"
        );
        assertFalse(errorCode == 0);
    }

    @Test
    public void testVariableCanBeOverwritten() throws Exception {
        compile(
                "here set 0\nhere set 1\ncpi here"
        );
        assertProgram(
                0xFE, 1
        );
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
        assertFalse(errorCode == 0);
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
        assertFalse(errorCode == 0);
    }

    @Test
    public void testORGthenDSdoNotOverlap() throws Exception {
        compile(
                "org 2\nds 2\nmov a,b"
        );
        assertProgram(
                0, 0, 0, 0, 0x78
        );
    }

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
        assertFalse(errorCode == 0);
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
        assertFalse(errorCode == 0);
    }

    @Test
    public void testDSwithNegativeValueDoesNotWork() throws Exception {
        compile(
                "ds -1"
        );
        assertFalse(errorCode == 0);
    }

    @Test
    public void testIncludeAndForwardCall() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
                "call sample\n"
                        + "include '" + includeFile.getAbsolutePath() + "'\n"
        );

        assertProgram(
                0xCD, 03, 00, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testDoubleIncludeAndForwardCall() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
                "call sample2\n"
                        + "include '" + first.getAbsolutePath() + "'\n"
                        + "include '" + second.getAbsolutePath() + "'\n"
        );

        assertProgram(
                0xCD, 06, 00, 0x3E, 0, 0xC9, 0x3E, 0 ,0xC9
        );
    }

    @Test
    public void testIncludeAndBackwardCall() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
                "include '" + includeFile.getAbsolutePath() + "'\n"
                        + "call sample\n"
        );

        assertProgram(
                0x3E, 0, 0xC9, 0xCD, 0, 0
        );
    }

    @Test
    public void testDoubleIncludeAndBackwardCall() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
                "include '" + first.getAbsolutePath() + "'\n"
                        + "include '" + second.getAbsolutePath() + "'\n"
                        + "call sample\n"
        );

        assertProgram(
                0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0xCD, 0, 0
        );
    }

    @Test
    public void testIncludeAndJMPafter() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
                "jmp next\n"
                + "include '" + includeFile.getAbsolutePath() + "'\n"
                + "next:\n"
                + "mov a, b\n"
        );

        assertProgram(
                0xC3, 0x06, 0, 0x3E, 0, 0xC9, 0x78
        );
    }

    @Test
    public void testDoubleIncludeAndJMPafter() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
                "jmp next\n"
                        + "include '" + first.getAbsolutePath() + "'\n"
                        + "include '" + second.getAbsolutePath() + "'\n"
                        + "next:\n"
                        + "mov a, b\n"
        );

        assertProgram(
                0xC3, 0x09, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0x78
        );
    }

    @Test
    public void testORGwithInclude() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
                "org 3\n"
                        + "call sample\n"
                        + "include '" + includeFile.getAbsolutePath() + "'\n"
        );

        assertProgram(
                0, 0, 0, 0xCD, 06, 00, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testORGwithDoubleInclude() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
                "org 3\n"
                        + "call sample\n"
                        + "include '" + second.getAbsolutePath() + "'\n"
                        + "include '" + first.getAbsolutePath() + "'\n"
        );

        assertProgram(
                0, 0, 0, 0xCD, 0x09, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testORGwithDoubleIncludeAndJMPafter() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
                "org 3\n"
                        + "jmp next\n"
                        + "include '" + first.getAbsolutePath() + "'\n"
                        + "include '" + second.getAbsolutePath() + "'\n"
                        + "next:\n"
                        + "mov a, b\n"
        );

        assertProgram(
                0, 0, 0, 0xC3, 0x0C, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0x78
        );
    }

    @Test
    public void testORGwithDB() throws Exception {
        compile(
                "org 3\n"
                + "lxi h, text\n"
                + "text:\n"
                + "db 'ahoj'"
        );

        assertProgram(
                0, 0, 0, 0x21, 0x06, 0, 'a', 'h', 'o', 'j'
        );
    }

}
