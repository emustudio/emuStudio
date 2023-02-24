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
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.plugins.memory.rasp.api.RaspMemoryCell;
import net.jcip.annotations.Immutable;

@Immutable
public class RaspMemoryCellImpl implements RaspMemoryCell {
    private final boolean isInstruction;
    private final int address;
    private final int value;

    private RaspMemoryCellImpl(boolean isInstruction, int address, int value) {
        this.isInstruction = isInstruction;
        this.address = address;
        this.value = value;
    }

    public static RaspMemoryCellImpl instruction(int address, int opcode) {
        return new RaspMemoryCellImpl(true, address, opcode);
    }

    public static RaspMemoryCellImpl operand(int address, int value) {
        return new RaspMemoryCellImpl(false, address, value);
    }

    @Override
    public boolean isInstruction() {
        return isInstruction;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public int getValue() {
        return value;
    }
}
