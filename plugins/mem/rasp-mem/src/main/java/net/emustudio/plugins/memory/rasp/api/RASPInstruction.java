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

package net.emustudio.plugins.memory.rasp.api;

/**
 * Interface representing RASP instruction.
 *
 * @author miso
 */
public interface RASPInstruction extends MemoryItem {

    /**
     * Operation code of RASP instruction.
     */
    public final static int READ = 1;
    /**
     * Operation code of RASP instruction.
     */
    public final static int WRITE_CONSTANT = 2;
    /**
     * Operation code of RASP instruction.
     */
    public final static int WRITE_REGISTER = 3;
    /**
     * Operation code of RASP instruction.
     */
    public final static int LOAD_CONSTANT = 4;
    /**
     * Operation code of RASP instruction.
     */
    public final static int LOAD_REGISTER = 5;
    /**
     * Operation code of RASP instruction.
     */
    public final static int STORE = 6;
    /**
     * Operation code of RASP instruction.
     */
    public final static int ADD_CONSTANT = 7;
    /**
     * Operation code of RASP instruction.
     */
    public final static int ADD_REGISTER = 8;
    /**
     * Operation code of RASP instruction.
     */
    public final static int SUB_CONSTANT = 9;
    /**
     * Operation code of RASP instruction.
     */
    public final static int SUB_REGISTER = 10;
    /**
     * Operation code of RASP instruction.
     */
    public final static int MUL_CONSTANT = 11;
    /**
     * Operation code of RASP instruction.
     */
    public final static int MUL_REGISTER = 12;
    /**
     * Operation code of RASP instruction.
     */
    public final static int DIV_CONSTANT = 13;
    /**
     * Operation code of RASP instruction.
     */
    public static final int DIV_REGISTER = 14;
    /**
     * Operation code of RASP instruction.
     */
    public static final int JMP = 15;
    /**
     * Operation code of RASP instruction.
     */
    public static final int JZ = 16;
    /**
     * Operation code of RASP instruction.
     */
    public static final int JGTZ = 17;
    /**
     * Operation code of RASP instruction.
     */
    public static final int HALT = 18;

    /**
     * Get operation code of the instruction.
     *
     * @return operation code of the instruction.
     */
    public int getCode();

    /**
     * Get string representation of the RASP instruction (mnemonic code).
     *
     * @return string representation of the instruction
     */
    public String getCodeStr();

}
