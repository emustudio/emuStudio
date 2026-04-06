/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.ssem.ast.Instruction;
import net.emustudio.plugins.compiler.ssem.ast.Program;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CodeGeneratorTest {

    private static final SourceCodePosition POS = new SourceCodePosition(1, 0, "test.ssem");

    @Test
    public void testGenerateCodeWithBNUM() {
        Program program = new Program();
        // BNUM with binary number 100 = 4 in decimal
        program.add(0, new Instruction(SSEMParser.BNUM, 4, POS, null), POS);

        CodeGenerator codeGenerator = new CodeGenerator();
        ByteBuffer code = codeGenerator.generateCode(program);

        assertNotNull(code);
        assertTrue(code.hasRemaining());

        // First 4 bytes = startLine (0), next 4 bytes = BNUM instruction
        code.position(4);
        int value = code.getInt();
        // BNUM writes operand directly as int (not reversed)
        assertEquals(4, value);
    }

    @Test
    public void testGenerateCodeWithNUM() {
        Program program = new Program();
        // NUM with decimal number
        program.add(0, new Instruction(SSEMParser.NUM, 5, POS, null), POS);

        CodeGenerator codeGenerator = new CodeGenerator();
        ByteBuffer code = codeGenerator.generateCode(program);

        assertNotNull(code);
        assertTrue(code.hasRemaining());

        // First 4 bytes = startLine (0), next 4 bytes = NUM instruction
        code.position(4);
        int value = code.getInt();
        // NUM writes reverseBits(operand, 32)
        assertNotEquals(0, value);
    }

    @Test
    public void testGenerateCodeWithRegularInstruction() {
        Program program = new Program();
        program.add(0, new Instruction(SSEMParser.STO, 22, POS, POS), POS);

        CodeGenerator codeGenerator = new CodeGenerator();
        ByteBuffer code = codeGenerator.generateCode(program);

        assertNotNull(code);
        assertTrue(code.hasRemaining());

        code.position(4);
        int value = code.getInt();
        // STO opcode=6, operand=22, instruction = reverseBits(22 & 0x1F, 32) | ((6 & 0x07) << 16)
        assertNotEquals(0, value);
    }

    @Test
    public void testGenerateCodeWithStartLine() {
        Program program = new Program();
        program.setStartLine(5, POS);
        program.add(0, new Instruction(SSEMParser.STP, 0, POS, null), POS);

        CodeGenerator codeGenerator = new CodeGenerator();
        ByteBuffer code = codeGenerator.generateCode(program);

        assertNotNull(code);
        code.position(0);
        int startLine = code.getInt();
        assertEquals(5, startLine);
    }

    @Test
    public void testGenerateCodeEmpty() {
        Program program = new Program();

        CodeGenerator codeGenerator = new CodeGenerator();
        ByteBuffer code = codeGenerator.generateCode(program);

        assertNotNull(code);
        assertTrue(code.hasRemaining());
        // First 4 bytes = startLine (0 default)
        assertEquals(0, code.getInt());
    }

    @Test
    public void testGenerateCodeMultipleInstructions() {
        Program program = new Program();
        program.add(0, new Instruction(SSEMParser.LDN, 10, POS, POS), POS);
        program.add(1, new Instruction(SSEMParser.SUB, 20, POS, POS), POS);
        program.add(2, new Instruction(SSEMParser.STP, 0, POS, null), POS);

        CodeGenerator codeGenerator = new CodeGenerator();
        ByteBuffer code = codeGenerator.generateCode(program);

        assertNotNull(code);
        assertTrue(code.hasRemaining());
    }

    @Test
    public void testGenerateCodeWithLongBinaryNumber() {
        Program program = new Program();
        // Use a large binary number (32-bit value)
        long binaryValue = 3355443200L;
        program.add(0, new Instruction(SSEMParser.BNUM, binaryValue, POS, null), POS);

        CodeGenerator codeGenerator = new CodeGenerator();
        ByteBuffer code = codeGenerator.generateCode(program);

        assertNotNull(code);
        code.position(4);
        int value = code.getInt();
        assertEquals((int) binaryValue, value);
    }
}

