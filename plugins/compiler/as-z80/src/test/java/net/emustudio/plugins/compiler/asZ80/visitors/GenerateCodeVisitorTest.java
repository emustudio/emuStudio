/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoOrg;
import org.junit.Test;

import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.assertEquals;

public class GenerateCodeVisitorTest {

    @Test
    public void testCodeGeneration() {
        Program program = new Program();
        program
                .addChild(new DataDB(0, 0)
                        .addChild(new Evaluated(0, 0, 255))
                        .addChild(new Instr(0, 0, OPCODE_RST, 3, 0, 7)
                                .addChild(new Evaluated(0, 0, 4))))
                .addChild(new DataDW(0, 0)
                        .addChild(new Evaluated(0, 0, 1)))
                .addChild(new DataDS(0, 0)
                        .addChild(new Evaluated(0, 0, 5)))
                .addChild(new PseudoMacroCall(0, 0, "x")
                        .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1)
                                .setSizeBytes(3)
                                .addChild(new Evaluated(0, 0, 0xFEAB).setSizeBytes(2)))
                        .addChild(new PseudoMacroCall(0, 0, "y")
                                .addChild(new Instr(0, 0, OPCODE_LD, 0, 2, 1)
                                        .setSizeBytes(3)
                                        .addChild(new Evaluated(0, 0, 1).setSizeBytes(2))))
                        .addChild(new Instr(0, 0, OPCODE_LD, 0, 4, 1)
                                .setSizeBytes(3)
                                .addChild(new Evaluated(0, 0, 0x1234).setSizeBytes(2))));

        IntelHEX hex = new IntelHEX();
        GenerateCodeVisitor visitor = new GenerateCodeVisitor(hex);
        visitor.visit(program);
        Map<Integer, Byte> code = hex.getCode();

        assertEquals((byte) 255, code.get(0).byteValue());
        assertEquals((byte) 0xc7, code.get(1).byteValue());
        assertEquals(1, code.get(2).byteValue()); // dw - lower byte
        assertEquals(0, code.get(3).byteValue()); // dw - upper byte
        assertEquals(0, code.get(4).byteValue());
        assertEquals(0, code.get(5).byteValue());
        assertEquals(0, code.get(6).byteValue());
        assertEquals(0, code.get(7).byteValue());
        assertEquals(0, code.get(8).byteValue());
        assertEquals(1, code.get(9).byteValue()); // ld bc
        assertEquals((byte) 0xAB, code.get(10).byteValue());
        assertEquals((byte) 0xFE, code.get(11).byteValue());
        assertEquals(0x11, code.get(12).byteValue()); // ld de
        assertEquals(1, code.get(13).byteValue());
        assertEquals(0, code.get(14).byteValue());
        assertEquals(0x21, code.get(15).byteValue()); // ld hl
        assertEquals(0x34, code.get(16).byteValue());
        assertEquals(0x12, code.get(17).byteValue());
    }

    @Test
    public void testPseudoOrg() {
        Program program = new Program();
        program
                .addChild(new PseudoOrg(0, 0)
                        .addChild(new Evaluated(0, 0, 5)))
                .addChild(new Instr(0, 0, OPCODE_CALL, 3, 0, 4)
                        .setSizeBytes(3)
                        .addChild(new Evaluated(0, 0, 0x400).setSizeBytes(2)))
                .addChild(new PseudoOrg(0, 0)
                        .addChild(new Evaluated(0, 0, 0)))
                .addChild(new Instr(0, 0, OPCODE_EX, 3, 5, 3));

        IntelHEX hex = new IntelHEX();
        GenerateCodeVisitor visitor = new GenerateCodeVisitor(hex);
        visitor.visit(program);
        Map<Integer, Byte> code = hex.getCode();

        assertEquals((byte) 0xeb, code.get(0).byteValue());
        assertEquals((byte) 0xc4, code.get(5).byteValue());
        assertEquals(0, code.get(6).byteValue());
        assertEquals(4, code.get(7).byteValue());
    }
}
