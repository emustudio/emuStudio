/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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
 */

package net.sf.emustudio.rasp.memory.memoryitems;

public class RASPInstructionImpl implements RASPInstruction {

    /**
     * machine code of the instruction
     */
    private final int instructionCode;

    /**
     * Constructor.
     *
     * @param instructionCode the machine code of the instruction
     */
    public RASPInstructionImpl(int instructionCode) {
        this.instructionCode = instructionCode;
    }

    @Override
    public int getCode() {
        return instructionCode;
    }

    @Override
    public String getCodeStr() {
        switch (instructionCode) {
            case READ:
                return "READ";
            case WRITE_CONSTANT:
                return "WRITE =";
            case WRITE_REGISTER:
                return "WRITE";
            case LOAD_CONSTANT:
                return "LOAD =";
            case LOAD_REGISTER:
                return "LOAD";
            case STORE:
                return "STORE";
            case ADD_CONSTANT:
                return "ADD =";
            case ADD_REGISTER:
                return "ADD";
            case SUB_CONSTANT:
                return "SUB =";
            case SUB_REGISTER:
                return "SUB";
            case MUL_CONSTANT:
                return "MUL =";
            case MUL_REGISTER:
                return "MUL";
            case DIV_CONSTANT:
                return "DIV =";
            case DIV_REGISTER:
                return "DIV";
            case JMP:
                return "JMP";
            case JZ:
                return "JZ";
            case JGTZ:
                return "JGTZ";
            case HALT:
                return "HALT";
        }
        return "unknown";
    }

    @Override
    public String toString() {
        return getCodeStr();
    } 

}
