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
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotEquals;

public class InstructionTest extends AbstractCompilerTest {

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }

    @Test
    public void testCompile() throws Exception {
        compile(
                ";><+-.,[]"
        );

        assertProgram(
                0, 1, 2, 3, 4, 5, 6, 7, 8
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
                1, 1, 7, 4, 8, 2, 2, 7, 4, 1, 1, 3, 2, 2, 8
        );
    }

    @Test
    public void testProgramAfterCommentDoesNotWork() throws Exception {
        compile(
                "So this is the comment and program >>\n"
        );
        assertProgram(0);
    }

    @Test
    public void testCompileEmptyEol() throws Exception {
        compile(
                ";\n>"
        );

        assertProgram(
                0, 1
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNullProgramDoesNotChangeMemory() throws Exception {
        compile(">>");

        ContextPool contextPool = createMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0L, MemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        new CompilerBrainduck(0L, applicationApi, PluginSettings.UNAVAILABLE).compile(Path.of("nonexistant"), Optional.empty());

        assertProgram(
                1, 1
        );
    }
}
