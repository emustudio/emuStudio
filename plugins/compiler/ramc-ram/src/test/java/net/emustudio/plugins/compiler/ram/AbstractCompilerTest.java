/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram;

import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public abstract class AbstractCompilerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    protected CompilerRAM compiler;
    protected MemoryStub memoryStub;
    private int errorCount;

    @Before
    public void setUp() throws Exception {
        memoryStub = new MemoryStub(new Annotations());

        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemoryContext(0, RamMemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(pool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(pool).anyTimes();
        replay(applicationApi);

        errorCount = 0;
        compiler = new CompilerRAM(0L, applicationApi, PluginSettings.UNAVAILABLE);
        compiler.initialize();
        compiler.addCompilerListener(new CompilerListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onMessage(CompilerMessage compilerMessage) {
                if (compilerMessage.getMessageType() != CompilerMessage.MessageType.TYPE_INFO) {
                    System.out.println(compilerMessage);
                }
                if (compilerMessage.getMessageType() == CompilerMessage.MessageType.TYPE_ERROR) {
                    errorCount++;
                }
            }

            @Override
            public void onFinish() {

            }
        });
    }

    protected void compile(String content) throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);

        File outputFile = folder.newFile();
        compiler.compile(sourceFile.toPath(), Optional.of(outputFile.toPath()));
        if (errorCount > 0) {
            throw new Exception("Compilation failed with " + errorCount + " errors");
        }
    }

    protected void assertProgram(RamInstruction... program) {
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
