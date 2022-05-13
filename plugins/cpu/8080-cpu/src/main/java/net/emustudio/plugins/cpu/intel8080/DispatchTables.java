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
package net.emustudio.plugins.cpu.intel8080;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class DispatchTables {
    private final static Logger LOGGER = LoggerFactory.getLogger(DispatchTables.class);
    public final static MethodHandle[] DISPATCH_TABLE = new MethodHandle[256];

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType retInt = MethodType.methodType(int.class);
        
        try {
            DISPATCH_TABLE[0x00] = lookup.findVirtual(EmulatorEngine.class, "I_NOP", retInt);
            DISPATCH_TABLE[0x01] = lookup.findVirtual(EmulatorEngine.class, "I_LXI", retInt);
            DISPATCH_TABLE[0x02] = lookup.findVirtual(EmulatorEngine.class, "I_STAX", retInt);
            DISPATCH_TABLE[0x03] = lookup.findVirtual(EmulatorEngine.class, "I_INX", retInt);
            DISPATCH_TABLE[0x04] = lookup.findVirtual(EmulatorEngine.class, "I_INR", retInt);
            DISPATCH_TABLE[0x05] = lookup.findVirtual(EmulatorEngine.class, "I_DCR", retInt);
            DISPATCH_TABLE[0x06] = lookup.findVirtual(EmulatorEngine.class, "I_MVI", retInt);
            DISPATCH_TABLE[0x07] = lookup.findVirtual(EmulatorEngine.class, "I_RLC", retInt);
            DISPATCH_TABLE[0x09] = lookup.findVirtual(EmulatorEngine.class, "I_DAD", retInt);
            DISPATCH_TABLE[0x0A] = lookup.findVirtual(EmulatorEngine.class, "I_LDAX", retInt);
            DISPATCH_TABLE[0x0B] = lookup.findVirtual(EmulatorEngine.class, "I_DCX", retInt);
            DISPATCH_TABLE[0x0C] = lookup.findVirtual(EmulatorEngine.class, "I_INR", retInt);
            DISPATCH_TABLE[0x0D] = lookup.findVirtual(EmulatorEngine.class, "I_DCR", retInt);
            DISPATCH_TABLE[0x0E] = lookup.findVirtual(EmulatorEngine.class, "I_MVI", retInt);
            DISPATCH_TABLE[0x0F] = lookup.findVirtual(EmulatorEngine.class, "I_RRC", retInt);
            DISPATCH_TABLE[0x11] = lookup.findVirtual(EmulatorEngine.class, "I_LXI", retInt);
            DISPATCH_TABLE[0x12] = lookup.findVirtual(EmulatorEngine.class, "I_STAX", retInt);
            DISPATCH_TABLE[0x13] = lookup.findVirtual(EmulatorEngine.class, "I_INX", retInt);
            DISPATCH_TABLE[0x14] = lookup.findVirtual(EmulatorEngine.class, "I_INR", retInt);
            DISPATCH_TABLE[0x15] = lookup.findVirtual(EmulatorEngine.class, "I_DCR", retInt);
            DISPATCH_TABLE[0x16] = lookup.findVirtual(EmulatorEngine.class, "I_MVI", retInt);
            DISPATCH_TABLE[0x17] = lookup.findVirtual(EmulatorEngine.class, "I_RAL", retInt);
            DISPATCH_TABLE[0x19] = lookup.findVirtual(EmulatorEngine.class, "I_DAD", retInt);
            DISPATCH_TABLE[0x1A] = lookup.findVirtual(EmulatorEngine.class, "I_LDAX", retInt);
            DISPATCH_TABLE[0x1B] = lookup.findVirtual(EmulatorEngine.class, "I_DCX", retInt);
            DISPATCH_TABLE[0x1C] = lookup.findVirtual(EmulatorEngine.class, "I_INR", retInt);
            DISPATCH_TABLE[0x1D] = lookup.findVirtual(EmulatorEngine.class, "I_DCR", retInt);
            DISPATCH_TABLE[0x1E] = lookup.findVirtual(EmulatorEngine.class, "I_MVI", retInt);
            DISPATCH_TABLE[0x1F] = lookup.findVirtual(EmulatorEngine.class, "I_RAR", retInt);
            DISPATCH_TABLE[0x21] = lookup.findVirtual(EmulatorEngine.class, "I_LXI", retInt);
            DISPATCH_TABLE[0x22] = lookup.findVirtual(EmulatorEngine.class, "I_SHLD", retInt);
            DISPATCH_TABLE[0x23] = lookup.findVirtual(EmulatorEngine.class, "I_INX", retInt);
            DISPATCH_TABLE[0x24] = lookup.findVirtual(EmulatorEngine.class, "I_INR", retInt);
            DISPATCH_TABLE[0x25] = lookup.findVirtual(EmulatorEngine.class, "I_DCR", retInt);
            DISPATCH_TABLE[0x26] = lookup.findVirtual(EmulatorEngine.class, "I_MVI", retInt);
            DISPATCH_TABLE[0x27] = lookup.findVirtual(EmulatorEngine.class, "I_DAA", retInt);
            DISPATCH_TABLE[0x29] = lookup.findVirtual(EmulatorEngine.class, "I_DAD", retInt);
            DISPATCH_TABLE[0x2A] = lookup.findVirtual(EmulatorEngine.class, "I_LHLD", retInt);
            DISPATCH_TABLE[0x2B] = lookup.findVirtual(EmulatorEngine.class, "I_DCX", retInt);
            DISPATCH_TABLE[0x2C] = lookup.findVirtual(EmulatorEngine.class, "I_INR", retInt);
            DISPATCH_TABLE[0x2D] = lookup.findVirtual(EmulatorEngine.class, "I_DCR", retInt);
            DISPATCH_TABLE[0x2E] = lookup.findVirtual(EmulatorEngine.class, "I_MVI", retInt);
            DISPATCH_TABLE[0x2F] = lookup.findVirtual(EmulatorEngine.class, "I_CMA", retInt);
            DISPATCH_TABLE[0x31] = lookup.findVirtual(EmulatorEngine.class, "I_LXI", retInt);
            DISPATCH_TABLE[0x32] = lookup.findVirtual(EmulatorEngine.class, "I_STA", retInt);
            DISPATCH_TABLE[0x33] = lookup.findVirtual(EmulatorEngine.class, "I_INX", retInt);
            DISPATCH_TABLE[0x34] = lookup.findVirtual(EmulatorEngine.class, "I_INR", retInt);
            DISPATCH_TABLE[0x35] = lookup.findVirtual(EmulatorEngine.class, "I_DCR", retInt);
            DISPATCH_TABLE[0x36] = lookup.findVirtual(EmulatorEngine.class, "I_MVI", retInt);
            DISPATCH_TABLE[0x37] = lookup.findVirtual(EmulatorEngine.class, "I_STC", retInt);
            DISPATCH_TABLE[0x39] = lookup.findVirtual(EmulatorEngine.class, "I_DAD", retInt);
            DISPATCH_TABLE[0x3A] = lookup.findVirtual(EmulatorEngine.class, "I_LDA", retInt);
            DISPATCH_TABLE[0x3B] = lookup.findVirtual(EmulatorEngine.class, "I_DCX", retInt);
            DISPATCH_TABLE[0x3C] = lookup.findVirtual(EmulatorEngine.class, "I_INR", retInt);
            DISPATCH_TABLE[0x3D] = lookup.findVirtual(EmulatorEngine.class, "I_DCR", retInt);
            DISPATCH_TABLE[0x3E] = lookup.findVirtual(EmulatorEngine.class, "I_MVI", retInt);
            DISPATCH_TABLE[0x3F] = lookup.findVirtual(EmulatorEngine.class, "I_CMC", retInt);
            DISPATCH_TABLE[0x40] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x41] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x42] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x43] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x44] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x45] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x46] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x47] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x48] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x49] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x4A] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x4B] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x4C] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x4D] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x4E] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x4F] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x50] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x51] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x52] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x53] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x54] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x55] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x56] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x57] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x58] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x59] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x5A] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x5B] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x5C] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x5D] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x5E] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x5F] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x60] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x61] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x62] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x63] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x64] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x65] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x66] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x67] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x68] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x69] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x6A] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x6B] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x6C] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x6D] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x6E] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x6F] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x70] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x71] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x72] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x73] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x74] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x75] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x76] = lookup.findVirtual(EmulatorEngine.class, "I_HLT", retInt);
            DISPATCH_TABLE[0x77] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x78] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x79] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x7A] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x7B] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x7C] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x7D] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x7E] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x7F] = lookup.findVirtual(EmulatorEngine.class, "I_MOV", retInt);
            DISPATCH_TABLE[0x80] = lookup.findVirtual(EmulatorEngine.class, "I_ADD", retInt);
            DISPATCH_TABLE[0x81] = lookup.findVirtual(EmulatorEngine.class, "I_ADD", retInt);
            DISPATCH_TABLE[0x82] = lookup.findVirtual(EmulatorEngine.class, "I_ADD", retInt);
            DISPATCH_TABLE[0x83] = lookup.findVirtual(EmulatorEngine.class, "I_ADD", retInt);
            DISPATCH_TABLE[0x84] = lookup.findVirtual(EmulatorEngine.class, "I_ADD", retInt);
            DISPATCH_TABLE[0x85] = lookup.findVirtual(EmulatorEngine.class, "I_ADD", retInt);
            DISPATCH_TABLE[0x86] = lookup.findVirtual(EmulatorEngine.class, "I_ADD", retInt);
            DISPATCH_TABLE[0x87] = lookup.findVirtual(EmulatorEngine.class, "I_ADD", retInt);
            DISPATCH_TABLE[0x88] = lookup.findVirtual(EmulatorEngine.class, "I_ADC", retInt);
            DISPATCH_TABLE[0x89] = lookup.findVirtual(EmulatorEngine.class, "I_ADC", retInt);
            DISPATCH_TABLE[0x8A] = lookup.findVirtual(EmulatorEngine.class, "I_ADC", retInt);
            DISPATCH_TABLE[0x8B] = lookup.findVirtual(EmulatorEngine.class, "I_ADC", retInt);
            DISPATCH_TABLE[0x8C] = lookup.findVirtual(EmulatorEngine.class, "I_ADC", retInt);
            DISPATCH_TABLE[0x8D] = lookup.findVirtual(EmulatorEngine.class, "I_ADC", retInt);
            DISPATCH_TABLE[0x8E] = lookup.findVirtual(EmulatorEngine.class, "I_ADC", retInt);
            DISPATCH_TABLE[0x8F] = lookup.findVirtual(EmulatorEngine.class, "I_ADC", retInt);
            DISPATCH_TABLE[0x90] = lookup.findVirtual(EmulatorEngine.class, "I_SUB", retInt);
            DISPATCH_TABLE[0x91] = lookup.findVirtual(EmulatorEngine.class, "I_SUB", retInt);
            DISPATCH_TABLE[0x92] = lookup.findVirtual(EmulatorEngine.class, "I_SUB", retInt);
            DISPATCH_TABLE[0x93] = lookup.findVirtual(EmulatorEngine.class, "I_SUB", retInt);
            DISPATCH_TABLE[0x94] = lookup.findVirtual(EmulatorEngine.class, "I_SUB", retInt);
            DISPATCH_TABLE[0x95] = lookup.findVirtual(EmulatorEngine.class, "I_SUB", retInt);
            DISPATCH_TABLE[0x96] = lookup.findVirtual(EmulatorEngine.class, "I_SUB", retInt);
            DISPATCH_TABLE[0x97] = lookup.findVirtual(EmulatorEngine.class, "I_SUB", retInt);
            DISPATCH_TABLE[0x98] = lookup.findVirtual(EmulatorEngine.class, "I_SBB", retInt);
            DISPATCH_TABLE[0x99] = lookup.findVirtual(EmulatorEngine.class, "I_SBB", retInt);
            DISPATCH_TABLE[0x9A] = lookup.findVirtual(EmulatorEngine.class, "I_SBB", retInt);
            DISPATCH_TABLE[0x9B] = lookup.findVirtual(EmulatorEngine.class, "I_SBB", retInt);
            DISPATCH_TABLE[0x9C] = lookup.findVirtual(EmulatorEngine.class, "I_SBB", retInt);
            DISPATCH_TABLE[0x9D] = lookup.findVirtual(EmulatorEngine.class, "I_SBB", retInt);
            DISPATCH_TABLE[0x9E] = lookup.findVirtual(EmulatorEngine.class, "I_SBB", retInt);
            DISPATCH_TABLE[0x9F] = lookup.findVirtual(EmulatorEngine.class, "I_SBB", retInt);
            DISPATCH_TABLE[0xA0] = lookup.findVirtual(EmulatorEngine.class, "I_ANA", retInt);
            DISPATCH_TABLE[0xA1] = lookup.findVirtual(EmulatorEngine.class, "I_ANA", retInt);
            DISPATCH_TABLE[0xA2] = lookup.findVirtual(EmulatorEngine.class, "I_ANA", retInt);
            DISPATCH_TABLE[0xA3] = lookup.findVirtual(EmulatorEngine.class, "I_ANA", retInt);
            DISPATCH_TABLE[0xA4] = lookup.findVirtual(EmulatorEngine.class, "I_ANA", retInt);
            DISPATCH_TABLE[0xA5] = lookup.findVirtual(EmulatorEngine.class, "I_ANA", retInt);
            DISPATCH_TABLE[0xA6] = lookup.findVirtual(EmulatorEngine.class, "I_ANA", retInt);
            DISPATCH_TABLE[0xA7] = lookup.findVirtual(EmulatorEngine.class, "I_ANA", retInt);
            DISPATCH_TABLE[0xA8] = lookup.findVirtual(EmulatorEngine.class, "I_XRA", retInt);
            DISPATCH_TABLE[0xA9] = lookup.findVirtual(EmulatorEngine.class, "I_XRA", retInt);
            DISPATCH_TABLE[0xAA] = lookup.findVirtual(EmulatorEngine.class, "I_XRA", retInt);
            DISPATCH_TABLE[0xAB] = lookup.findVirtual(EmulatorEngine.class, "I_XRA", retInt);
            DISPATCH_TABLE[0xAC] = lookup.findVirtual(EmulatorEngine.class, "I_XRA", retInt);
            DISPATCH_TABLE[0xAD] = lookup.findVirtual(EmulatorEngine.class, "I_XRA", retInt);
            DISPATCH_TABLE[0xAE] = lookup.findVirtual(EmulatorEngine.class, "I_XRA", retInt);
            DISPATCH_TABLE[0xAF] = lookup.findVirtual(EmulatorEngine.class, "I_XRA", retInt);
            DISPATCH_TABLE[0xB0] = lookup.findVirtual(EmulatorEngine.class, "I_ORA", retInt);
            DISPATCH_TABLE[0xB1] = lookup.findVirtual(EmulatorEngine.class, "I_ORA", retInt);
            DISPATCH_TABLE[0xB2] = lookup.findVirtual(EmulatorEngine.class, "I_ORA", retInt);
            DISPATCH_TABLE[0xB3] = lookup.findVirtual(EmulatorEngine.class, "I_ORA", retInt);
            DISPATCH_TABLE[0xB4] = lookup.findVirtual(EmulatorEngine.class, "I_ORA", retInt);
            DISPATCH_TABLE[0xB5] = lookup.findVirtual(EmulatorEngine.class, "I_ORA", retInt);
            DISPATCH_TABLE[0xB6] = lookup.findVirtual(EmulatorEngine.class, "I_ORA", retInt);
            DISPATCH_TABLE[0xB7] = lookup.findVirtual(EmulatorEngine.class, "I_ORA", retInt);
            DISPATCH_TABLE[0xB8] = lookup.findVirtual(EmulatorEngine.class, "I_CMP", retInt);
            DISPATCH_TABLE[0xB9] = lookup.findVirtual(EmulatorEngine.class, "I_CMP", retInt);
            DISPATCH_TABLE[0xBA] = lookup.findVirtual(EmulatorEngine.class, "I_CMP", retInt);
            DISPATCH_TABLE[0xBB] = lookup.findVirtual(EmulatorEngine.class, "I_CMP", retInt);
            DISPATCH_TABLE[0xBC] = lookup.findVirtual(EmulatorEngine.class, "I_CMP", retInt);
            DISPATCH_TABLE[0xBD] = lookup.findVirtual(EmulatorEngine.class, "I_CMP", retInt);
            DISPATCH_TABLE[0xBE] = lookup.findVirtual(EmulatorEngine.class, "I_CMP", retInt);
            DISPATCH_TABLE[0xBF] = lookup.findVirtual(EmulatorEngine.class, "I_CMP", retInt);
            DISPATCH_TABLE[0xC0] = lookup.findVirtual(EmulatorEngine.class, "I_RET_COND", retInt); // RNZ
            DISPATCH_TABLE[0xC1] = lookup.findVirtual(EmulatorEngine.class, "I_POP", retInt);
            DISPATCH_TABLE[0xC2] = lookup.findVirtual(EmulatorEngine.class, "I_JMP_COND", retInt); // JNZ
            DISPATCH_TABLE[0xC3] = lookup.findVirtual(EmulatorEngine.class, "I_JMP", retInt);
            DISPATCH_TABLE[0xC4] = lookup.findVirtual(EmulatorEngine.class, "I_CALL_COND", retInt); // CNZ
            DISPATCH_TABLE[0xC5] = lookup.findVirtual(EmulatorEngine.class, "I_PUSH", retInt);
            DISPATCH_TABLE[0xC6] = lookup.findVirtual(EmulatorEngine.class, "I_ADI", retInt);
            DISPATCH_TABLE[0xC7] = lookup.findVirtual(EmulatorEngine.class, "I_RST", retInt);
            DISPATCH_TABLE[0xC8] = lookup.findVirtual(EmulatorEngine.class, "I_RET_COND", retInt); // RZ
            DISPATCH_TABLE[0xC9] = lookup.findVirtual(EmulatorEngine.class, "I_RET", retInt);
            DISPATCH_TABLE[0xCA] = lookup.findVirtual(EmulatorEngine.class, "I_JMP_COND", retInt); // JZ
            DISPATCH_TABLE[0xCC] = lookup.findVirtual(EmulatorEngine.class, "I_CALL_COND", retInt); // CZ
            DISPATCH_TABLE[0xCD] = lookup.findVirtual(EmulatorEngine.class, "I_CALL", retInt);
            DISPATCH_TABLE[0xCE] = lookup.findVirtual(EmulatorEngine.class, "I_ACI", retInt);
            DISPATCH_TABLE[0xCF] = lookup.findVirtual(EmulatorEngine.class, "I_RST", retInt);
            DISPATCH_TABLE[0xD0] = lookup.findVirtual(EmulatorEngine.class, "I_RET_COND", retInt); // RNC
            DISPATCH_TABLE[0xD1] = lookup.findVirtual(EmulatorEngine.class, "I_POP", retInt);
            DISPATCH_TABLE[0xD2] = lookup.findVirtual(EmulatorEngine.class, "I_JMP_COND", retInt); // JNC
            DISPATCH_TABLE[0xD3] = lookup.findVirtual(EmulatorEngine.class, "I_OUT", retInt);
            DISPATCH_TABLE[0xD4] = lookup.findVirtual(EmulatorEngine.class, "I_CALL_COND", retInt); // CNC
            DISPATCH_TABLE[0xD5] = lookup.findVirtual(EmulatorEngine.class, "I_PUSH", retInt);
            DISPATCH_TABLE[0xD6] = lookup.findVirtual(EmulatorEngine.class, "I_SUI", retInt);
            DISPATCH_TABLE[0xD7] = lookup.findVirtual(EmulatorEngine.class, "I_RST", retInt);
            DISPATCH_TABLE[0xD8] = lookup.findVirtual(EmulatorEngine.class, "I_RET_COND", retInt); // RC
            DISPATCH_TABLE[0xDA] = lookup.findVirtual(EmulatorEngine.class, "I_JMP_COND", retInt); // JC
            DISPATCH_TABLE[0xDB] = lookup.findVirtual(EmulatorEngine.class, "I_IN", retInt);
            DISPATCH_TABLE[0xDC] = lookup.findVirtual(EmulatorEngine.class, "I_CALL_COND", retInt); // CC
            DISPATCH_TABLE[0xDE] = lookup.findVirtual(EmulatorEngine.class, "I_SBI", retInt);
            DISPATCH_TABLE[0xDF] = lookup.findVirtual(EmulatorEngine.class, "I_RST", retInt);
            DISPATCH_TABLE[0xE0] = lookup.findVirtual(EmulatorEngine.class, "I_RET_COND", retInt); // RPO
            DISPATCH_TABLE[0xE1] = lookup.findVirtual(EmulatorEngine.class, "I_POP", retInt);
            DISPATCH_TABLE[0xE2] = lookup.findVirtual(EmulatorEngine.class, "I_JMP_COND", retInt); // JPO
            DISPATCH_TABLE[0xE3] = lookup.findVirtual(EmulatorEngine.class, "I_XTHL", retInt);
            DISPATCH_TABLE[0xE4] = lookup.findVirtual(EmulatorEngine.class, "I_CALL_COND", retInt); // CPO
            DISPATCH_TABLE[0xE5] = lookup.findVirtual(EmulatorEngine.class, "I_PUSH", retInt);
            DISPATCH_TABLE[0xE6] = lookup.findVirtual(EmulatorEngine.class, "I_ANI", retInt);
            DISPATCH_TABLE[0xE7] = lookup.findVirtual(EmulatorEngine.class, "I_RST", retInt);
            DISPATCH_TABLE[0xE8] = lookup.findVirtual(EmulatorEngine.class, "I_RET_COND", retInt); // RPE
            DISPATCH_TABLE[0xE9] = lookup.findVirtual(EmulatorEngine.class, "I_PCHL", retInt);
            DISPATCH_TABLE[0xEA] = lookup.findVirtual(EmulatorEngine.class, "I_JMP_COND", retInt); // JPE
            DISPATCH_TABLE[0xEB] = lookup.findVirtual(EmulatorEngine.class, "I_XCHG", retInt);
            DISPATCH_TABLE[0xEC] = lookup.findVirtual(EmulatorEngine.class, "I_CALL_COND", retInt); // CPE
            DISPATCH_TABLE[0xEE] = lookup.findVirtual(EmulatorEngine.class, "I_XRI", retInt);
            DISPATCH_TABLE[0xEF] = lookup.findVirtual(EmulatorEngine.class, "I_RST", retInt);
            DISPATCH_TABLE[0xF0] = lookup.findVirtual(EmulatorEngine.class, "I_RET_COND", retInt); // RP
            DISPATCH_TABLE[0xF1] = lookup.findVirtual(EmulatorEngine.class, "I_POP", retInt);
            DISPATCH_TABLE[0xF2] = lookup.findVirtual(EmulatorEngine.class, "I_JMP_COND", retInt); // JP
            DISPATCH_TABLE[0xF3] = lookup.findVirtual(EmulatorEngine.class, "I_DI", retInt);
            DISPATCH_TABLE[0xF4] = lookup.findVirtual(EmulatorEngine.class, "I_CALL_COND", retInt); // CP
            DISPATCH_TABLE[0xF5] = lookup.findVirtual(EmulatorEngine.class, "I_PUSH", retInt);
            DISPATCH_TABLE[0xF6] = lookup.findVirtual(EmulatorEngine.class, "I_ORI", retInt);
            DISPATCH_TABLE[0xF7] = lookup.findVirtual(EmulatorEngine.class, "I_RST", retInt);
            DISPATCH_TABLE[0xF8] = lookup.findVirtual(EmulatorEngine.class, "I_RET_COND", retInt); // RM
            DISPATCH_TABLE[0xF9] = lookup.findVirtual(EmulatorEngine.class, "I_SPHL", retInt);
            DISPATCH_TABLE[0xFA] = lookup.findVirtual(EmulatorEngine.class, "I_JMP_COND", retInt); // JM
            DISPATCH_TABLE[0xFB] = lookup.findVirtual(EmulatorEngine.class, "I_EI", retInt);
            DISPATCH_TABLE[0xFC] = lookup.findVirtual(EmulatorEngine.class, "I_CALL_COND", retInt); // CM
            DISPATCH_TABLE[0xFE] = lookup.findVirtual(EmulatorEngine.class, "I_CPI", retInt);
            DISPATCH_TABLE[0xFF] = lookup.findVirtual(EmulatorEngine.class, "I_RST", retInt);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            LOGGER.error("Could not set up dispatch table. The emulator won't work correctly", e);
        }
    }
}
