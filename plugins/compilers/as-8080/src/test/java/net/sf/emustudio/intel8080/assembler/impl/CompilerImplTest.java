package net.sf.emustudio.intel8080.assembler.impl;

import emulib.plugins.compiler.Compiler;
import emulib.plugins.compiler.Message;
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
                    String.format("%d. expected=%x, but was=%x", i, bytes[i], memoryStub.read(i)),
                    bytes[i], (int) memoryStub.read(i)
            );
        }
    }

    @Test
    public void testForwardAbsoluteJump() throws Exception {
        compile(
                "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "jz ler\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x06, 0x00, 0x77
        );
    }

    @Test
    public void testBackwardAbsoluteJump() throws Exception {
        compile(
                "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "jz now\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x00, 0x00, 0x77
        );
    }

    @Test
    public void testCallBackward() throws Exception {
        compile(
                "dcx sp\n" +
                        "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "call now\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x3B, 0x78, 0xFE, 0x43, 0xCD, 0x01, 0x00, 0x77
        );
    }

    @Test
    public void testCallForward() throws Exception {
        compile(
                "dcx sp\n" +
                        "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "call ler\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x3B, 0x78, 0xFE, 0x43, 0xCD, 0x07, 0x00, 0x77
        );
    }

    @Test
    public void testRSTtooBigArgument() throws Exception {
        compile("rst 10");

        assertFalse(errorCode == 0);
    }

    @Test
    public void testDCXwithLXI() throws Exception {
        compile(
                "dcx sp\n"
                        + "lxi h, text\n"
                        + "text:\n"
                        + "db 'ahoj'"
        );

        assertProgram(
                0x3B, 0x21, 0x04, 0, 'a', 'h', 'o', 'j'
        );
    }

    @Test
    public void testINthenJMP() throws Exception {
        compile(
                "jmp sample\n"
                        + "in 10h\n"
                        + "sample:\n"
                        + "mov a, b\n"
        );

        assertProgram(
                0xC3, 0x5, 0, 0xDB, 0x10, 0x78
        );
    }

    @Test
    public void testGetChar() throws Exception {
        compile(
                "jmp sample\n"
                        + "getchar:\n"
                        + "in 10h\n"
                        + "ani 1\n"
                        + "jz getchar\n"
                        + "in 11h\n"
                        + "out 11h\n"
                        + "ret\n"
                        + "sample:\n"
                        + "mov a, b"
        );

        assertProgram(
                0xC3, 0x0F, 0, 0xDB, 0x10, 0xE6, 1, 0xCA, 0x03, 0, 0xDB, 0x11, 0xD3, 0x11, 0xC9, 0x78
        );
    }
}