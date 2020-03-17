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
package net.emustudio.plugins.compiler.ramc;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.compiler.ramc.tree.RAMInstructionImpl;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotEquals;

public class CompilerTest extends AbstractCompilerTest {

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }

    @Test
    public void testREAD() throws Exception {
        compile("READ 5\nREAD *6");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.READ, RAMInstruction.Direction.REGISTER, 5),
            new RAMInstructionImpl(RAMInstruction.READ, RAMInstruction.Direction.INDIRECT, 6)
        );
    }

    @Test
    public void testWRITE() throws Exception {
        compile("WRITE =3\nWRITE 4\nWRITE *8");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.WRITE, RAMInstruction.Direction.DIRECT, "3"),
            new RAMInstructionImpl(RAMInstruction.WRITE, RAMInstruction.Direction.REGISTER, 4),
            new RAMInstructionImpl(RAMInstruction.WRITE, RAMInstruction.Direction.INDIRECT, 8)
        );
    }

    @Test
    public void testLOAD() throws Exception {
        compile("LOAD =hello\nLOAD 7\nLOAD *11");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.LOAD, RAMInstruction.Direction.DIRECT, "hello"),
            new RAMInstructionImpl(RAMInstruction.LOAD, RAMInstruction.Direction.REGISTER, 7),
            new RAMInstructionImpl(RAMInstruction.LOAD, RAMInstruction.Direction.INDIRECT, 11)
        );
    }

    @Test
    public void testSTORE() throws Exception {
        compile("STORE 111111112\nSTORE *55\n");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.STORE, RAMInstruction.Direction.REGISTER, 111111112),
            new RAMInstructionImpl(RAMInstruction.STORE, RAMInstruction.Direction.INDIRECT, 55)
        );
    }

    @Test
    public void testADD() throws Exception {
        compile("ADD =\"omg omg\"\nADD 99\nADD *1");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.ADD, RAMInstruction.Direction.DIRECT, "omg omg"),
            new RAMInstructionImpl(RAMInstruction.ADD, RAMInstruction.Direction.REGISTER, 99),
            new RAMInstructionImpl(RAMInstruction.ADD, RAMInstruction.Direction.INDIRECT, 1)
        );
    }

    @Test
    public void testSUB() throws Exception {
        compile("SUB =\"omg omg\"\nSUB 229\nSUB *2453");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.SUB, RAMInstruction.Direction.DIRECT, "omg omg"),
            new RAMInstructionImpl(RAMInstruction.SUB, RAMInstruction.Direction.REGISTER, 229),
            new RAMInstructionImpl(RAMInstruction.SUB, RAMInstruction.Direction.INDIRECT, 2453)
        );
    }

    @Test
    public void testMUL() throws Exception {
        compile("MUL =\"omg omg\"\nMUL 229\nMUL *2453");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.MUL, RAMInstruction.Direction.DIRECT, "omg omg"),
            new RAMInstructionImpl(RAMInstruction.MUL, RAMInstruction.Direction.REGISTER, 229),
            new RAMInstructionImpl(RAMInstruction.MUL, RAMInstruction.Direction.INDIRECT, 2453)
        );
    }

    @Test
    public void testDIV() throws Exception {
        compile("DIV =\"omg omg\"\nDIV 229\nDIV *2453");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.DIV, RAMInstruction.Direction.DIRECT, "omg omg"),
            new RAMInstructionImpl(RAMInstruction.DIV, RAMInstruction.Direction.REGISTER, 229),
            new RAMInstructionImpl(RAMInstruction.DIV, RAMInstruction.Direction.INDIRECT, 2453)
        );
    }

    @Test
    public void testJMP() throws Exception {
        compile("here: JMP here");

        assertProgram(new RAMInstructionImpl(RAMInstruction.JMP, "here"));
        assertEquals(0, memoryStub.read(0).getOperand());
    }

    @Test
    public void testJMP_ForwardReference() throws Exception {
        compile("JMP here\nhere:HALT");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.JMP, "here"),
            new RAMInstructionImpl(RAMInstruction.HALT, RAMInstruction.Direction.REGISTER, 0)
        );
        assertEquals(1, memoryStub.read(0).getOperand());
    }

    @Test
    public void testJZ() throws Exception {
        compile("JZ here\nhere:HALT");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.JZ, "here"),
            new RAMInstructionImpl(RAMInstruction.HALT, RAMInstruction.Direction.REGISTER, 0)
        );
        assertEquals(1, memoryStub.read(0).getOperand());
    }

    @Test
    public void testJGTZ() throws Exception {
        compile("JGTZ here\nhere:HALT");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.JGTZ, "here"),
            new RAMInstructionImpl(RAMInstruction.HALT, RAMInstruction.Direction.REGISTER, 0)
        );
        assertEquals(1, memoryStub.read(0).getOperand());
    }

    @Test
    public void testHALT() throws Exception {
        compile("halt");

        assertProgram(
            new RAMInstructionImpl(RAMInstruction.HALT, RAMInstruction.Direction.REGISTER, 0)
        );
    }

    @Test(expected = Exception.class)
    public void testNegativeRegistersAreNotSupported() throws Exception {
        compile("STORE -2");
    }

    @Test
    public void testCompileWithoutSpecifyingOutputDoesNotOverwriteSource() throws Exception {
        ContextPool tmpContextPool = createMock(ContextPool.class);
        tmpContextPool.register(eq(0L), EasyMock.<RAMInstruction>anyObject(), eq(RAMInstruction.class));
        expectLastCall().once();
        expect(tmpContextPool.getMemoryContext(0L, RAMMemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(tmpContextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(tmpContextPool).anyTimes();
        replay(applicationApi);

        CompilerImpl tmpCompiler = new CompilerImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);

        File sourceFile = folder.newFile("test-ram.ram");
        Files.write(sourceFile.toPath(), "HALT".getBytes(), StandardOpenOption.WRITE);

        tmpCompiler.compile(sourceFile.getPath());

        assertTrue(sourceFile.getParentFile().toPath().resolve("test-ram.ro").toFile().exists());
    }
}
