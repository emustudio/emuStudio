package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.compiler.ssem.CompileException;
import net.emustudio.plugins.compiler.ssem.SSEMParser;

import java.nio.ByteBuffer;

public class CodeGenerator {
    private final ByteBuffer code = ByteBuffer.allocate(33 * 4); // one is for start line

    public ByteBuffer generateCode(Program program) {
        code.position(0);
        code.putInt(program.getStartLine());

        program.forEach((line, instruction) -> {
            code.position(4 * line);
            if (instruction.tokenType == SSEMParser.BNUM || instruction.tokenType == SSEMParser.NUM) {
                code.putInt(instruction.operand);
            } else {
                writeInstruction(instruction.getOpcode(), instruction.operand);
            }
        });
        return code.clear();
    }

    private void writeInstruction(int opcode, int operand) {
        if (operand < 0 || operand > 31) {
            throw new CompileException("Operand must be between <0, 31>; it was " + operand);
        }

        byte address = (byte)(NumberUtils.reverseBits((byte)(operand & 0xFF), 8) & 0xF8);
        // 5 bits address + 3 empty bits
        code.put(address);
        // next: 5 empty bits + 3 bit instruction
        code.put((byte)(opcode & 0xFF));
        // 16 empty bits
        code.put((byte)0);
        code.put((byte)0);
    }
}
