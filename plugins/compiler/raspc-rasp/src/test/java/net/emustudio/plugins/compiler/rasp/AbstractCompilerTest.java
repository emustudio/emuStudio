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
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryCell;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public abstract class AbstractCompilerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    protected CompilerRASP compiler;
    protected MemoryStub memoryStub;

    @Before
    public void setUp() throws Exception {
        memoryStub = new MemoryStub();

        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemoryContext(0, RaspMemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(pool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(pool).anyTimes();
        replay(applicationApi);

        compiler = new CompilerRASP(0L, applicationApi, PluginSettings.UNAVAILABLE);
        compiler.initialize();
    }

    protected void compile(String content) throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);

        File outputFile = folder.newFile();
        compiler.addCompilerListener(new CompilerListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onMessage(CompilerMessage compilerMessage) {
                System.out.println(compilerMessage);
            }

            @Override
            public void onFinish() {

            }
        });
        if (!compiler.compile(sourceFile.getAbsolutePath(), outputFile.getAbsolutePath())) {
            throw new Exception("Compilation failed");
        }
    }

    protected void assertProgram(RaspMemoryCell... program) {
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
}
