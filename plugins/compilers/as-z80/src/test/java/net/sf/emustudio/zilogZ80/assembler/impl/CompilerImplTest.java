package net.sf.emustudio.zilogZ80.assembler.impl;

import emulib.plugins.compiler.*;
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

public class CompilerImplTest {
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
                CompilerImplTest.this.errorCode = errorCode;
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
    public void testForwardAbsoluteJump() throws Exception {
        compile(
                "now: ld a, b\n" +
                        "cp \"C\"\n" +
                        "jp z, ler\n" +
                        "ler: ld (hl), a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x06, 0x00, 0x77
        );
    }

    @Test
    public void testBackwardAbsoluteJump() throws Exception {
        compile(
                "now: ld a,b\n" +
                        "cp \"C\"\n" +
                        "jp z, now\n" +
                        "ler: ld (hl), a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x00, 0x00, 0x77
        );
    }

    @Test
    public void testForwardRelativeJump() throws Exception {
        compile(
                "now: ld a, b\n" +
                        "cp \"C\"\n" +
                        "jp z, ler\n" +
                        "ler: ld (hl), a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x06, 0x00, 0x77
        );
    }

    @Test
    public void testRSTtooBigArgument() throws Exception {
        compile("rst 40h");

        assertFalse(errorCode == 0);
    }

    @Test
    public void testRelativeJump() throws Exception {
        compile(
                        "loop: ld A, 0\n" +
                        "cp 0\n" +
                        "jr Z, end\n" +
                        "jp loop\n" +
                        "\n" +
                        "end:\n" +
                        "halt\n"
        );

        assertProgram(
                0x3E, 0, 0xFE, 0, 0x28, 3, 0xC3, 0, 0, 0x76
        );

    }
}