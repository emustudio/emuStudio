/*
 * Copyright (C) 2016 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package net.sf.emustudio.ssem.assembler;

import net.sf.emustudio.ssem.assembler.tree.ASTvisitor;
import net.sf.emustudio.ssem.assembler.tree.Instruction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class CodeGenerator implements ASTvisitor, AutoCloseable {
    private final DataOutputStream writer;

    public CodeGenerator(OutputStream writer) {
        this.writer= new DataOutputStream(Objects.requireNonNull(writer));
    }

    @Override
    public void visit(Instruction instruction) throws CompileException, IOException {
        byte address = instruction.getOperand().orElse((byte)0);

        if (address < 0 || address > 31) {
            throw new CompileException("Operand must be between <0, 31>; it was " + address);
        }

        // Instruction has 32 bits, i.e. 4 bytes

        int addressSSEM = (((address >> 4) & 1)
            | (((address >> 3) & 1) << 1)
            | (((address >> 2) & 1) << 2)
            | (((address >> 1) & 1) << 3)
            | ((address & 1) << 4)) << 3;

        writer.writeByte(addressSSEM); // address + 3 empty bits

        // next: 5 empty bits + 3 bit instruction
        int opcode = instruction.getOpcode() & 7;
        writer.writeByte(opcode);

        // 16 empty bits
        writer.write(new byte[2]);
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
