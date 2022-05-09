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
package net.emustudio.plugins.compiler.as8080.e2e;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.cpu.testsuite.memory.MemoryStub;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.compiler.as8080.Assembler8080;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public abstract class AbstractCompilerTest {
    protected Assembler8080 compiler;
    protected MemoryStub<Byte> memoryStub;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        memoryStub = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool);
        replay(applicationApi);

        compiler = new Assembler8080(0L, applicationApi, PluginSettings.UNAVAILABLE);
        compiler.addCompilerListener(new CompilerListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onMessage(CompilerMessage message) {
                if (message.getMessageType() != CompilerMessage.MessageType.TYPE_INFO) {
                    System.out.println(message);
                }
            }

            @Override
            public void onFinish() {
            }
        });
        compiler.initialize();
    }

    protected void compile(String content) throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);

        File outputFile = folder.newFile();
        if (!compiler.compile(sourceFile.getAbsolutePath(), outputFile.getAbsolutePath())) {
            throw new Exception("Compilation failed");
        }
    }

    protected void assertProgram(int... bytes) {
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(
                String.format("[addr=%x] expected=%x, but was=%x", i, bytes[i], memoryStub.read(i)),
                (byte)bytes[i], memoryStub.read(i).byteValue()
            );
        }
        for (int i = bytes.length; i < memoryStub.getSize(); i++) {
            assertEquals(
                String.format("[addr=%x] expected=%x, but was=%x", i, 0, memoryStub.read(i)),
                0, memoryStub.read(i).byteValue()
            );
        }
    }
}
