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
}
