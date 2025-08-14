/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.cpu.testsuite.memory.MemoryStub;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public abstract class AbstractCompilerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    protected CompilerBrainduck compiler;
    protected MemoryStub<Byte> memoryStub;
    private int errorCount;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        memoryStub = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);

        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(pool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(pool).anyTimes();
        replay(applicationApi);

        errorCount = 0;
        compiler = new CompilerBrainduck(0L, applicationApi, PluginSettings.UNAVAILABLE);
        compiler.addCompilerListener(new CompilerListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onMessage(CompilerMessage message) {
                System.out.println(message);
                if (message.getMessageType() == CompilerMessage.MessageType.TYPE_ERROR) {
                    errorCount++;
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
        compiler.compile(sourceFile.toPath(), Optional.of(outputFile.toPath()));
        if (errorCount > 0) {
            throw new Exception("Compilation failed with " + errorCount + " errors");
        }
    }

    void assertProgram(int... bytes) {
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(
                    String.format("%d. expected=%x, but was=%x", i, bytes[i], memoryStub.read(i)),
                    bytes[i], (int) memoryStub.read(i)
            );
        }
        for (int i = bytes.length; i < memoryStub.getSize(); i++) {
            assertEquals(
                    String.format("%d. expected=%x, but was=%x", i, 0, memoryStub.read(i)),
                    0, (int) memoryStub.read(i)
            );
        }
    }
}
