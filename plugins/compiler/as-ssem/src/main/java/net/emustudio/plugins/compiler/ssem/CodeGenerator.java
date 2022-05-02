package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.compiler.ssem.ast.Program;

import java.nio.ByteBuffer;

public class CodeGenerator {
    private final ByteBuffer code = ByteBuffer.allocate(33 * 4); // one is for start line

    public ByteBuffer generateCode(Program program) {
        code.position(0);
        code.putInt(program.getStartLine());

        program.forEach((line, instruction) -> {
            code.position(4 * (line + 1));
            if (instruction.tokenType == SSEMParser.BNUM) {
                code.putInt((int) instruction.operand);
            } else if (instruction.tokenType == SSEMParser.NUM) {
                code.putInt((int) NumberUtils.reverseBits(instruction.operand, 32));
            } else {
                writeInstruction(instruction.getOpcode(), instruction.operand);
            }
        });
        return code.clear();
    }

    private void writeInstruction(int opcode, long operand) {
        int instruction = NumberUtils.reverseBits((int)operand & 0x1F, 32) | ((opcode & 0x07) << 16);
        code.putInt(instruction);
    }
}
