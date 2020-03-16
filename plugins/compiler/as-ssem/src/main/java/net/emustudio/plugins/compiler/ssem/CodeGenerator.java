/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
import net.emustudio.plugins.compiler.ssem.tree.ASTvisitor;
import net.emustudio.plugins.compiler.ssem.tree.Constant;
import net.emustudio.plugins.compiler.ssem.tree.Instruction;

import java.io.IOException;
import java.util.Objects;

public class CodeGenerator implements ASTvisitor, AutoCloseable {
    private final SeekableOutputStream writer;
    private int currentLine;

    public CodeGenerator(SeekableOutputStream writer) {
        this.writer = Objects.requireNonNull(writer);
    }

    @Override
    public void setCurrentLine(int line) {
        this.currentLine = line;
    }

    @Override
    public void visit(Instruction instruction) throws CompileException, IOException {
        byte address = instruction.getOperand().orElse((byte) 0);

        if (address < 0 || address > 31) {
            throw new CompileException("Operand must be between <0, 31>; it was " + address);
        }

        // Instruction has 32 bits, i.e. 4 bytes
        int addressSSEM = NumberUtils.reverseBits(address, 8) & 0xF8;
        writer.seek(4 * currentLine);

        writer.write(addressSSEM); // address + 3 empty bits

        // next: 5 empty bits + 3 bit instruction
        int opcode = instruction.getOpcode() & 7;
        writer.write(opcode);

        // 16 empty bits
        writer.write(new byte[2]);
    }

    @Override
    public void visit(Constant constant) throws Exception {
        int number = constant.getNumber();

        writer.seek(4 * currentLine);
        writeInt(number);
    }

    private void writeInt(int value) throws IOException {
        Byte[] word = new Byte[4];
        NumberUtils.writeInt(value, word, NumberUtils.Strategy.REVERSE_BITS);

        writer.write(word[0]);
        writer.write(word[1]);
        writer.write(word[2]);
        writer.write(word[3]);
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
