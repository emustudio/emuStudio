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
package net.sf.emustudio.brainduck.brainc;

import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import net.sf.emustudio.brainduck.brainc.impl.CompilerImpl;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class InstructionTest extends AbstractCompilerTest {

    @Test
    public void testCompile() throws Exception {
        compile(
            ";><+-.,[]"
        );

        assertProgram(
            0,1,2,3,4,5,6,7,8
        );
    }

    @Test
    public void testCompileProgramWithComments() throws Exception {
        compile(
            "Code:   Pseudo code:\n" +
                ">>      Move the pointer to cell2\n" +
                "[-]     Set cell2 to 0 \n" +
                "<<      Move the pointer back to cell0\n" +
                "[       While cell0 is not 0\n" +
                "  -       Subtract 1 from cell0\n" +
                "  >>      Move the pointer to cell2\n" +
                "  +       Add 1 to cell2\n" +
                "  <<      Move the pointer back to cell0\n" +
                "]       End while"
        );

        assertProgram(
            1,1,7,4,8,2,2,7,4,1,1,3,2,2,8
        );
    }

    @Test
    public void testProgramAfterCommentDoesNotWork() throws Exception {
        compile(
            "So this is the comment and program >>\n"
        );

        assertProgram();
    }

    @Test
    public void testNullProgramDoesNotChangeMemory() throws Exception {
        compile(">>");

        ContextPool contextPool = createMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0L, MemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(contextPool);

        new CompilerImpl(0L, contextPool).compile("nonexistant");

        assertProgram(
            1,1
        );
    }

    @Test
    public void testCommandLine() throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), ">>".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();

        CompilerImpl.main("--output", outputFile.getPath(), sourceFile.getPath());

        List<String> lines = Files.readAllLines(outputFile.toPath());

        assertEquals(2, lines.size());
        assertEquals(":020000000101FC", lines.get(0));
        assertEquals(":00000001FF", lines.get(1));
    }

    @Test
    public void testCommandLinePrintHelp() throws Exception {
        CompilerImpl.main("--help");
    }

    @Test
    public void testCommandLineNonexistantSourceFileDoesNotThrow() throws Exception {
        CompilerImpl.main("slfjkdf");
    }

    @Test
    public void testCommandLinePrintVersion() throws Exception {
        CompilerImpl.main("--version");
    }
}
