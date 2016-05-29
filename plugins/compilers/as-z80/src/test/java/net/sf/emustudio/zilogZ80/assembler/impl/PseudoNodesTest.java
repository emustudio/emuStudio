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
package net.sf.emustudio.zilogZ80.assembler.impl;

import emulib.plugins.compiler.Message;
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
                        "now: ld a,b\n" +
                        "ds 2\n" +
                        "cp \"C\"\n" +
                        "jp z, now\n" +
                        "ler: ld (hl), a"
        );

        assertProgram(
                0, 0, 0x78, 0, 0, 0xFE, 0x43, 0xCA, 0x02, 0x00, 0x77
        );
    }

    @Test
    public void testORG() throws Exception {
        compile(
                "org 2\n" +
                        "now: ld a,b\n" +
                        "ds 2\n" +
                        "cp \"C\"\n" +
                        "jp z, now\n" +
                        "ler: ld (hl), a"
        );

        assertProgram(
                0, 0, 0x78, 0, 0, 0xFE, 0x43, 0xCA, 0x02, 0x00, 0x77
        );
    }

    @Test
    public void testORGdoesNotBreakPreviousMemoryContent() throws Exception {
        memoryStub.write(0, (short)0x10);
        memoryStub.write(1, (short) 0x11);

        compile(
                "org 2\n" + "now: ld a,b\n"
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
                "ds 2\n" + "now: ld a,b\n"
        );

        assertProgram(
                0x0, 0x0, 0x78
        );
    }

    @Test
    public void testLabelAsConstantWorks() throws Exception {
        compile(
                "here equ 0\n"
                        + "ld a,b\n"
                        + "jp z, here"
        );

        assertProgram(
                0x78, 0xCA, 0, 0
        );
    }

    @Test
    public void testConstantAsLabelWorks() throws Exception {
        compile(
                "here equ there\n"
                        + "there: ld a,b\n"
                        + "jp z, here"
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
                        + "jp z, here"
        );
        assertFalse(errorCode == 0);
    }

    @Test
    public void testTwoSameLabelsDoNotWork() throws Exception {
        compile(
                "here:\nhere:\njp z, here"
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
                "here var 0\nhere var 1\ncp here"
        );
        assertProgram(
                0xFE, 1
        );
    }

    @Test
    public void testDBallocatesOneByte() throws Exception {
        compile(
                "db 10\nld a,b\n"
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
                "dw 10\nld a,b\n"
        );
        assertProgram(
                10, 0, 0x78
        );
    }

    @Test
    public void testDWseveralValuesWork() throws Exception {
        compile(
                "dw 10,4\nld a,b\n"
        );
        assertProgram(
                10, 0, 4, 0, 0x78
        );
    }

    @Test
    public void testDWmoreThanFFFFdoesNotWork() throws Exception {
        compile(
                "dw 10000h\nld a,b\n"
        );
        assertFalse(errorCode == 0);
    }

    @Test
    public void testORGthenDSdoNotOverlap() throws Exception {
        compile(
                "org 2\nds 2\nld a,b"
        );
        assertProgram(
                0, 0, 0, 0, 0x78
        );
    }

    @Test
    public void testTwoSuccessiveORG() throws Exception {
        compile(
                "org 2\norg 3\nhalt"
        );
        assertProgram(
                0, 0, 0, 0x76
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
}
