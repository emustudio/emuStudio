/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ram;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.ram.ast.Instruction;
import net.emustudio.plugins.compiler.ram.ast.Label;
import net.emustudio.plugins.compiler.ram.ast.Value;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import static net.emustudio.plugins.memory.ram.api.RamInstruction.Opcode.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

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
                new Instruction(0, 0, READ, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(5))),
                new Instruction(0, 0, READ, RamInstruction.Direction.INDIRECT, 1, Optional.of(new Value(6)))
        );
    }

    @Test
    public void testWRITE() throws Exception {
        compile("WRITE =3\nWRITE 4\nWRITE *8");

        assertProgram(
                new Instruction(0, 0, WRITE, RamInstruction.Direction.CONSTANT, 0, Optional.of(new Value(3))),
                new Instruction(0, 0, WRITE, RamInstruction.Direction.DIRECT, 1, Optional.of(new Value(4))),
                new Instruction(0, 0, WRITE, RamInstruction.Direction.INDIRECT, 2, Optional.of(new Value(8)))
        );
    }

    @Test
    public void testLOAD() throws Exception {
        compile("LOAD ='hello'\nLOAD 7\nLOAD *11");

        assertProgram(
                new Instruction(0, 0, LOAD, RamInstruction.Direction.CONSTANT, 0, Optional.of(new Value("hello", false))),
                new Instruction(0, 0, LOAD, RamInstruction.Direction.DIRECT, 1, Optional.of(new Value(7))),
                new Instruction(0, 0, LOAD, RamInstruction.Direction.INDIRECT, 2, Optional.of(new Value(11)))
        );
    }

    @Test
    public void testSTORE() throws Exception {
        compile("STORE 111111112\nSTORE *55\n");

        assertProgram(
                new Instruction(0, 0, STORE, RamInstruction.Direction.DIRECT, 0, Optional.of(new Value(111111112))),
                new Instruction(0, 0, STORE, RamInstruction.Direction.INDIRECT, 1, Optional.of(new Value(55)))
        );
    }

    @Test
    public void testADD() throws Exception {
        compile("ADD =\"omg omg\"\nADD 99\nADD *1");

        assertProgram(
                new Instruction(0, 0, ADD, RamInstruction.Direction.CONSTANT, 0, Optional.of(new Value("omg omg", false))),
                new Instruction(0, 0, ADD, RamInstruction.Direction.DIRECT, 1, Optional.of(new Value(99))),
                new Instruction(0, 0, ADD, RamInstruction.Direction.INDIRECT, 2, Optional.of(new Value(1)))
        );
    }

    @Test
    public void testSUB() throws Exception {
        compile("SUB =\"omg omg\"\nSUB 229\nSUB *2453");

        assertProgram(
                new Instruction(0, 0, SUB, RamInstruction.Direction.CONSTANT, 0, Optional.of(new Value("omg omg", false))),
                new Instruction(0, 0, SUB, RamInstruction.Direction.DIRECT, 1, Optional.of(new Value(229))),
                new Instruction(0, 0, SUB, RamInstruction.Direction.INDIRECT, 2, Optional.of(new Value(2453)))
        );
    }

    @Test
    public void testMUL() throws Exception {
        compile("MUL =\"omg omg\"\nMUL 229\nMUL *2453");

        assertProgram(
                new Instruction(0, 0, MUL, RamInstruction.Direction.CONSTANT, 0, Optional.of(new Value("omg omg", false))),
                new Instruction(0, 0, MUL, RamInstruction.Direction.DIRECT, 1, Optional.of(new Value(229))),
                new Instruction(0, 0, MUL, RamInstruction.Direction.INDIRECT, 2, Optional.of(new Value(2453)))
        );
    }

    @Test
    public void testDIV() throws Exception {
        compile("DIV =\"omg omg\"\nDIV 229\nDIV *2453");

        assertProgram(
                new Instruction(0, 0, DIV, RamInstruction.Direction.CONSTANT, 0, Optional.of(new Value("omg omg", false))),
                new Instruction(0, 0, DIV, RamInstruction.Direction.DIRECT, 1, Optional.of(new Value(229))),
                new Instruction(0, 0, DIV, RamInstruction.Direction.INDIRECT, 2, Optional.of(new Value(2453)))
        );
    }

    @Test
    public void testJMP() throws Exception {
        compile("here: JMP here");

        assertProgram(new Instruction(
                JMP, RamInstruction.Direction.DIRECT, 0,
                Optional.of(new Value("here", true)),
                new Label(0, 0, "here", 0)
        ));
        assertEquals(Optional.of(0), memoryStub.read(0).getLabel().map(RamLabel::getAddress));
    }

    @Test
    public void testJMP_ForwardReference() throws Exception {
        compile("JMP here\nhere:HALT");

        assertProgram(
                new Instruction(
                        JMP, RamInstruction.Direction.DIRECT, 0,
                        Optional.of(new Value("here", true)),
                        new Label(0, 0, "here", 1)),
                new Instruction(0, 0, HALT, RamInstruction.Direction.DIRECT, 1, Optional.empty())
        );
        assertEquals(Optional.of(1), memoryStub.read(0).getLabel().map(RamLabel::getAddress));
    }

    @Test
    public void testJZ() throws Exception {
        compile("JZ here\nhere:HALT");

        assertProgram(
                new Instruction(
                        JZ, RamInstruction.Direction.DIRECT, 0,
                        Optional.of(new Value("here", true)),
                        new Label(0, 0, "here", 1)),
                new Instruction(0, 0, HALT, RamInstruction.Direction.DIRECT, 1, Optional.empty())
        );
        assertEquals(Optional.of(1), memoryStub.read(0).getLabel().map(RamLabel::getAddress));
    }

    @Test
    public void testJGTZ() throws Exception {
        compile("JGTZ here\nhere:HALT");

        assertProgram(
                new Instruction(
                        JGTZ, RamInstruction.Direction.DIRECT, 0,
                        Optional.of(new Value("here", true)),
                        new Label(0, 0, "here", 1)),
                new Instruction(0, 0, HALT, RamInstruction.Direction.DIRECT, 1, Optional.empty())
        );
        assertEquals(Optional.of(1), memoryStub.read(0).getLabel().map(RamLabel::getAddress));
    }

    @Test
    public void testHALT() throws Exception {
        compile("halt");

        assertProgram(
                new Instruction(0, 0, HALT, RamInstruction.Direction.DIRECT, 0, Optional.empty())
        );
    }

    @Test(expected = Exception.class)
    public void testNegativeRegistersAreNotSupported() throws Exception {
        compile("STORE -2");
    }

    @Test
    public void testCompileWithoutSpecifyingOutputDoesNotOverwriteSource() throws Exception {
        ContextPool tmpContextPool = createMock(ContextPool.class);
        expect(tmpContextPool.getMemoryContext(0L, RamMemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(tmpContextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(tmpContextPool).anyTimes();
        replay(applicationApi);

        CompilerRAM tmpCompiler = new CompilerRAM(0L, applicationApi, PluginSettings.UNAVAILABLE);

        File sourceFile = folder.newFile("test-ram.ram");
        Files.write(sourceFile.toPath(), "HALT".getBytes(), StandardOpenOption.WRITE);

        tmpCompiler.compile(sourceFile.getPath());

        assertTrue(sourceFile.getParentFile().toPath().resolve("test-ram.bram").toFile().exists());
    }
}
