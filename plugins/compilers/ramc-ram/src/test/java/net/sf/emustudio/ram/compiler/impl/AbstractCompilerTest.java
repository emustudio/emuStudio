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
package net.sf.emustudio.ram.compiler.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.compiler.Message;
import emulib.runtime.ContextPool;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public abstract class AbstractCompilerTest {
    protected RAMCompiler compiler;
    protected MemoryStub memoryStub;
    protected int errorCode;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        memoryStub = new MemoryStub();

        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemoryContext(0, RAMMemoryContext.class))
            .andReturn(memoryStub).anyTimes();
        replay(pool);

        compiler = new RAMCompiler(0L, pool);
        compiler.addCompilerListener(new Compiler.CompilerListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onMessage(Message message) {
                System.out.println(message);
            }

            @Override
            public void onFinish(int errorCode) {
                System.out.println("Compilation finished with code: " + errorCode);
                AbstractCompilerTest.this.errorCode = errorCode;
            }
        });
        compiler.initialize(createMock(SettingsManager.class));
    }

    protected void compile(String content) throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);

        File outputFile = folder.newFile();
        compiler.compile(sourceFile.getAbsolutePath(), outputFile.getAbsolutePath());
    }

    protected void assertProgram(RAMInstruction... program) {
        assertTrue(errorCode == 0);
        for (int i = 0; i < program.length; i++) {
            assertEquals(
                String.format("%d. expected=%s, but was=%s", i, program[i], memoryStub.read(i)),
                program[i], memoryStub.read(i)
            );
        }
        for (int i = program.length; i < memoryStub.getSize(); i++) {
            assertNull(
                String.format("%d. expected=null, but was=%s", i, memoryStub.read(i)),
                memoryStub.read(i)
            );
        }
    }

    protected void assertError() {
        assertFalse(errorCode == 0);
        for (int i = 0; i < memoryStub.getSize(); i++) {
            assertNull(
                String.format("%d. expected=null, but was=%s", i, memoryStub.read(i)),
                memoryStub.read(i)
            );
        }
    }

}
