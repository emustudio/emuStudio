/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
        int instruction = NumberUtils.reverseBits((int) operand & 0x1F, 32) | ((opcode & 0x07) << 16);
        code.putInt(instruction);
    }
}
