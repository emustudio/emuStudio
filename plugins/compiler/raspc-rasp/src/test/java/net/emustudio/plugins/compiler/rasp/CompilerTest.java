/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class CompilerTest extends AbstractCompilerTest {

    @Test
    public void testJmpInstruction() throws Exception {
        compile(
                "org 22\n" +
                        "START: jmp HERE\n" +
                        "jmp START\n" +
                        "HERE: halt"
        );

        assertProgram(
                null,
                null,
                15,
                26,
                15,
                22,
                18
        );
    }

    @Test(expected = Exception.class)
    public void testNonExistingLabel() throws Exception {
        compile("jmp hahaha");
    }

    @Test(expected = Exception.class)
    public void testAlreadyDefinedLabel() throws Exception {
        compile("label:\nlabel:");
    }

    @Test
    public void testREAD() throws Exception {
        compile("READ 5");
        assertProgram(READ, 5);
    }

    @Test
    public void testWRITE() throws Exception {
        compile("WRITE =3\nWRITE 4");
        assertProgram(2, 3, 3, 4);
    }

    @Test
    public void testLOAD() throws Exception {
        compile("LOAD =6\nLOAD 7");
        assertProgram(4, 6, 5, 7);
    }

    @Test
    public void testSTORE() throws Exception {
        compile("STORE 111111112");
        assertProgram(6, 111111112);
    }

    @Test
    public void testADD() throws Exception {
        compile("ADD =2\nADD 99");
        assertProgram(7, 2, 8, 99);
    }

    @Test
    public void testSUB() throws Exception {
        compile("SUB =3\nSUB 229");
        assertProgram(9, 3, 10, 229);
    }

    @Test
    public void testMUL() throws Exception {
        compile("MUL =-5\nMUL 229");
        assertProgram(11, -5, 12, 229);
    }

    @Test
    public void testDIV() throws Exception {
        compile("DIV =0\nDIV 229");
        assertProgram(13, 0, 14, 229);
    }

    @Test
    public void testJMP() throws Exception {
        compile("here: JMP here");
        assertProgram(JMP, 20);
    }

    @Test
    public void testJMP_ForwardReference() throws Exception {
        compile("JMP here\nhere:HALT");
        assertProgram(JMP, 22, HALT);
    }

    @Test
    public void testJZ() throws Exception {
        compile("JZ here\nhere:HALT");
        assertProgram(JZ, 22, HALT);
    }

    @Test
    public void testJGTZ() throws Exception {
        compile("JGTZ here\nhere:HALT");
        assertProgram(JGTZ, 22, HALT);
    }

    @Test
    public void testHALT() throws Exception {
        compile("halt");
        assertProgram(HALT);
    }

    @Test(expected = Exception.class)
    public void testNegativeRegistersAreNotSupported() throws Exception {
        compile("STORE -2");
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
    public void testGetDescription() {
        assertNotNull(compiler.getDescription());
        assertFalse(compiler.getDescription().isEmpty());
    }

    @Test
    public void testGetSourceFileExtensions() {
        assertNotNull(compiler.getSourceFileExtensions());
        assertFalse(compiler.getSourceFileExtensions().isEmpty());
        assertEquals("rasp", compiler.getSourceFileExtensions().get(0).getExtension());
    }

    @Test
    public void testCreateLexer() {
        LexicalAnalyzer lexer = compiler.createLexer();
        assertNotNull(lexer);
    }

    @Test
    public void testHexNumber1() throws Exception {
        compile("READ 0x0A");
        assertProgram(READ, 10);
    }

    @Test
    public void testHexNumber2() throws Exception {
        compile("READ 0Ah");
        assertProgram(READ, 10);
    }

    @Test
    public void testOctalNumber() throws Exception {
        compile("READ 12o");
        assertProgram(READ, 10);
    }

    @Test
    public void testOctalNumberWithQ() throws Exception {
        compile("READ 12q");
        assertProgram(READ, 10);
    }

    @Test
    public void testBinaryNumber() throws Exception {
        compile("READ 1010b");
        assertProgram(READ, 10);
    }

    @Test
    public void testDecimalWithSuffix() throws Exception {
        compile("READ 10d");
        assertProgram(READ, 10);
    }

    @Test
    public void testDecimalWithUpperCaseSuffix() throws Exception {
        compile("READ 10D");
        assertProgram(READ, 10);
    }

    @Test
    public void testInputDirective() throws Exception {
        compile("<INPUT> 1 2 3\nHALT");
        assertProgram(HALT);
    }

    @Test
    public void testInputDirectiveWithHexValues() throws Exception {
        compile("<INPUT> 0xA 0xB\nHALT");
        assertProgram(HALT);
    }

    @Test
    public void testComment() throws Exception {
        compile("; this is a comment\nHALT");
        assertProgram(HALT);
    }

    @Test
    public void testCommentWithHash() throws Exception {
        compile("# this is also a comment\nHALT");
        assertProgram(HALT);
    }

    @Test
    public void testEmptyProgram() throws Exception {
        compile("");
    }

    @Test
    public void testMultipleInstructions() throws Exception {
        compile("READ 1\nWRITE =2\nHALT");
        assertProgram(READ, 1, 2, 2, HALT);
    }

    @Test
    public void testLabelOnSameLineAsInstruction() throws Exception {
        compile("start: READ 1\nHALT");
        assertProgram(READ, 1, HALT);
    }

    @Test(expected = Exception.class)
    public void testSyntaxError() throws Exception {
        compile("INVALID_INSTRUCTION 5");
    }

    @Test(expected = Exception.class)
    public void testNegativeRegisterInRead() throws Exception {
        compile("READ -1");
    }

    @Test(expected = Exception.class)
    public void testNegativeRegisterInLoad() throws Exception {
        compile("LOAD -1");
    }

    @Test(expected = Exception.class)
    public void testNegativeRegisterInAdd() throws Exception {
        compile("ADD -1");
    }

    @Test(expected = Exception.class)
    public void testNegativeRegisterInWrite() throws Exception {
        compile("WRITE -1");
    }

    @Test(expected = Exception.class)
    public void testNegativeRegisterInSub() throws Exception {
        compile("SUB -1");
    }

    @Test(expected = Exception.class)
    public void testNegativeRegisterInMul() throws Exception {
        compile("MUL -1");
    }

    @Test(expected = Exception.class)
    public void testNegativeRegisterInDiv() throws Exception {
        compile("DIV -1");
    }

    @Test
    public void testNegativeConstantInLoadIsAllowed() throws Exception {
        compile("LOAD =-5");
        assertProgram(4, -5);
    }

    @Test
    public void testNegativeConstantInAddIsAllowed() throws Exception {
        compile("ADD =-3");
        assertProgram(7, -3);
    }

    @Test
    public void testCompileWithoutSpecifyingOutput() throws Exception {
        ContextPool tmpContextPool = createMock(ContextPool.class);
        expect(tmpContextPool.getMemoryContext(0L, RaspMemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(tmpContextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(tmpContextPool).anyTimes();
        replay(applicationApi);

        CompilerRASP tmpCompiler = new CompilerRASP(0L, applicationApi, PluginSettings.UNAVAILABLE);

        File sourceFile = folder.newFile("test-rasp.rasp");
        Files.write(sourceFile.toPath(), "HALT".getBytes(), StandardOpenOption.WRITE);

        tmpCompiler.compile(sourceFile.toPath(), Optional.empty());

        assertTrue(sourceFile.getParentFile().toPath().resolve("test-rasp.brasp").toFile().exists());
    }

    @Test
    public void testCompileWithoutMemory() throws Exception {
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(null).anyTimes();
        replay(applicationApi);

        CompilerRASP tmpCompiler = new CompilerRASP(0L, applicationApi, PluginSettings.UNAVAILABLE);
        tmpCompiler.initialize();

        File sourceFile = folder.newFile("test-no-mem.rasp");
        Files.write(sourceFile.toPath(), "HALT".getBytes(), StandardOpenOption.WRITE);
        File outputFile = folder.newFile();
        tmpCompiler.compile(sourceFile.toPath(), Optional.of(outputFile.toPath()));
    }

    @Test
    public void testOrgDirective() throws Exception {
        compile("org 30\nHALT");
        // HALT should be at address 30, so relative to memStart=20, it's at offset 10
        assertEquals(Integer.valueOf(HALT), memoryStub.read(30));
    }

    @Test
    public void testInputDirectiveWithBinaryValues() throws Exception {
        compile("<INPUT> 1010b 11b\nHALT");
        assertProgram(HALT);
    }

    @Test
    public void testInputDirectiveWithOctalValues() throws Exception {
        compile("<INPUT> 12o 7q\nHALT");
        assertProgram(HALT);
    }

    @Test
    public void testConstantHexInWrite() throws Exception {
        compile("WRITE =0xA");
        assertProgram(2, 10);
    }

    @Test
    public void testConstantHexInSub() throws Exception {
        compile("SUB =0xF");
        assertProgram(9, 15);
    }

    @Test
    public void testConstantHexInDiv() throws Exception {
        compile("DIV =0x2");
        assertProgram(13, 2);
    }

    @Test
    public void testConstantHexInMul() throws Exception {
        compile("MUL =0x3");
        assertProgram(11, 3);
    }
}
