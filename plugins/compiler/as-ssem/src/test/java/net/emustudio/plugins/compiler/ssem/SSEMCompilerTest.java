/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.cpu.testsuite.memory.MemoryStub;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SSEMCompilerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private SSEMCompiler compiler;
    private MemoryStub<Byte> memoryStub;
    private int errorCount;

    @Before
    public void setUp() throws Exception {
        memoryStub = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);

        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemory(0, Byte.class)).andReturn(memoryStub).anyTimes();
        replay(pool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(pool).anyTimes();
        replay(applicationApi);

        errorCount = 0;
        compiler = new SSEMCompiler(0L, applicationApi, PluginSettings.UNAVAILABLE);
        compiler.addCompilerListener(compilerMessage -> {
            if (compilerMessage.getType() == CompilerMessage.Type.ERROR) {
                errorCount++;
            }
        });
        compiler.initialize();
    }

    private void compile(String content) throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);

        File outputFile = folder.newFile();
        compiler.compile(sourceFile.toPath(), Optional.of(outputFile.toPath()));
        if (errorCount > 0) {
            throw new Exception("Compilation failed with " + errorCount + " errors");
        }
    }

    private void assertProgram(int... bytes) {
        Byte[] value = memoryStub.read(0, bytes.length);
        byte[] expected = new byte[bytes.length];
        byte[] actual = new byte[value.length];
        for (int i = 0; i < bytes.length; i++) {
            expected[i] = (byte) bytes[i];
        }
        for (int i = 0; i < value.length; i++) {
            actual[i] = value[i];
        }

        assertArrayEquals(
                String.format(
                        "Expected=%x, but was=%x",
                        Numbers.readInt(Arrays.stream(bytes).boxed().toArray(Integer[]::new), Numbers.Strategy.BIG_ENDIAN),
                        Numbers.readInt(value, Numbers.Strategy.BIG_ENDIAN)
                ),
                expected, actual
        );
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }

    @Test
    public void testSTO() throws Exception {
        compile("00 STO 22\n");
        assertProgram(0x68, 0x6, 0, 0);
    }

    @Test
    public void testLDN() throws Exception {
        compile("00 LDN 29");
        assertProgram(0xB8, 0x2, 0, 0);
    }

    @Test
    public void testSUB() throws Exception {
        compile("00 SUB 30");
        assertProgram(0x78, 0x1, 0, 0);
    }

    @Test
    public void testSTP() throws Exception {
        compile("00 STP");
        assertProgram(0, 0x7, 0, 0);
    }

    @Test
    public void testJMP() throws Exception {
        compile("00 JMP 10");
        // JMP opcode=0, operand=10
        // instruction = reverseBits(10 & 0x1F, 32) | ((0 & 0x07) << 16)
        // reverseBits(10, 32) = reverseBits(01010, 32) = 0x50000000
        assertProgram(0x50, 0, 0, 0);
    }

    @Test
    public void testJPR() throws Exception {
        compile("00 JPR 10");
        // JPR opcode=4, operand=10
        assertProgram(0x50, 0x4, 0, 0);
    }

    @Test
    public void testCMP() throws Exception {
        compile("00 CMP");
        // CMP opcode=3, operand=0
        assertProgram(0, 0x3, 0, 0);
    }

    @Test
    public void testNUM() throws Exception {
        compile("00 NUM 5");
        // NUM writes reverseBits(5, 32)
        // reverseBits(5, 32) = reverseBits(00000101, 32) = 0xA0000000
        assertProgram(0xA0, 0, 0, 0);
    }

    @Test
    public void testBNUM() throws Exception {
        compile("00 BNUM 100");
        // BNUM writes operand directly (not reversed)
        // binary 100 = 4
        assertProgram(0, 0, 0, 4);
    }

    @Test
    public void testStartLine() throws Exception {
        compile("02 start\n00 STP");
        assertProgram(0, 0x7, 0, 0);
    }

    @Test
    public void testCreateLexer() {
        assertNotNull(compiler.createLexer());
    }

    @Test
    public void testGetDescription() {
        assertNotNull(compiler.getDescription());
        assertFalse(compiler.getDescription().isEmpty());
    }

    @Test
    public void testGetSourceFileTypes() {
        assertFalse(compiler.getSourceFileTypes().isEmpty());
        assertEquals(List.of("ssem"), compiler.getSourceFileTypes().get(0).getExtensions());
    }

    @Test
    public void testCompileWithMultipleInstructions() throws Exception {
        compile("00 LDN 10\n01 SUB 20\n02 STP");
    }

    @Test
    public void testCompileWithComments() throws Exception {
        compile("00 STP -- this is a comment");
        assertProgram(0, 0x7, 0, 0);
    }

    @Test
    public void testCompileWithHexOperand() throws Exception {
        compile("00 LDN 0x1D");
        // LDN opcode=2, operand=0x1D=29
        assertProgram(0xB8, 0x2, 0, 0);
    }

    @Test(expected = Exception.class)
    public void testCompileInvalidSyntax() throws Exception {
        compile("stp");
    }

    @Test(expected = Exception.class)
    public void testCompileDuplicateLine() throws Exception {
        compile("00 STP\n00 LDN 1");
    }

    @Test(expected = Exception.class)
    public void testCompileLineOutOfBounds() throws Exception {
        compile("99 STP");
    }

    @Test(expected = Exception.class)
    public void testCompileOperandOutOfBounds() throws Exception {
        compile("00 LDN 99");
    }

    @Test
    public void testCompileWithoutMemory() throws Exception {
        // Create compiler without memory
        ApplicationApi noMemApi = createNiceMock(ApplicationApi.class);
        expect(noMemApi.getContextPool()).andReturn(null).anyTimes();
        replay(noMemApi);

        SSEMCompiler noMemCompiler = new SSEMCompiler(0L, noMemApi, PluginSettings.UNAVAILABLE);
        noMemCompiler.initialize();

        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), "00 STP\n".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();
        noMemCompiler.compile(sourceFile.toPath(), Optional.of(outputFile.toPath()));
    }

    @Test
    public void testCompileWithDefaultOutputPath() throws Exception {
        File sourceFile = folder.newFile("test.ssem");
        Files.write(sourceFile.toPath(), "00 STP\n".getBytes(), StandardOpenOption.WRITE);
        compiler.compile(sourceFile.toPath(), Optional.empty());
    }

    @Test
    public void testInitializeWithContextNotFoundException() throws Exception {
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemory(0, Byte.class))
                .andThrow(new ContextNotFoundException("Memory not found"))
                .anyTimes();
        replay(pool);
        ApplicationApi appApi = createNiceMock(ApplicationApi.class);
        expect(appApi.getContextPool()).andReturn(pool).anyTimes();
        replay(appApi);

        SSEMCompiler noCtxCompiler = new SSEMCompiler(0L, appApi, PluginSettings.UNAVAILABLE);
        // Should not throw - just logs a warning
        noCtxCompiler.initialize();
    }
}
