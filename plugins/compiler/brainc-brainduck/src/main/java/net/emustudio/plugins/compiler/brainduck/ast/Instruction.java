/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck.ast;

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.brainduck.BraincParser;

import java.util.Map;

public class Instruction {

    private final int instructionCode;

    public Instruction(int tokenType) {
        Map<Integer, Integer> tokenCodes = Map.of(
                BraincParser.HALT, 0,
                BraincParser.INC, 1,
                BraincParser.DEC, 2,
                BraincParser.INCV, 3,
                BraincParser.DECV, 4,
                BraincParser.PRINT, 5,
                BraincParser.LOAD, 6,
                BraincParser.LOOP, 7,
                BraincParser.ENDL, 8
        );
        this.instructionCode = tokenCodes.get(tokenType);
    }

    public void generateCode(IntelHEX hex) {
        hex.add(String.format("%1$02X", instructionCode));
    }

    @Override
    public String toString() {
        return "Instruction{" +
                "instructionCode=" + instructionCode +
                '}';
    }
}
