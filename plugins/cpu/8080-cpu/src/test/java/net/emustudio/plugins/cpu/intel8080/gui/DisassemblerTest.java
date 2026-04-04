/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.gui;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.cpu.Decoder;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisassemblerTest {

    private ByteMemoryStub memoryStub;
    private Decoder decoder;
    private Disassembler disassembler;

    @Before
    public void setUp() {
        memoryStub = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        decoder = new DecoderImpl(memoryStub);
        disassembler = new DisassemblerImpl(memoryStub, decoder);
    }

    @Test
    public void testDisassembleAllInstructions() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{
                // 1-byte instructions (no operands)
                0x00,                   // nop
                0x02,                   // stax b
                0x12,                   // stax d
                0x0a,                   // ldax b
                0x1a,                   // ldax d
                0x07,                   // rlc
                0x0f,                   // rrc
                0x17,                   // ral
                0x1f,                   // rar
                0x27,                   // daa
                0x2f,                   // cma
                0x37,                   // stc
                0x3f,                   // cmc
                0x76,                   // hlt
                0x77,                   // mov m, a

                // Return instructions
                0xc0,                   // rnz
                0xc8,                   // rz
                0xd0,                   // rnc
                0xd8,                   // rc
                0xe0,                   // rpo
                0xe8,                   // rpe
                0xf0,                   // rp
                0xf8,                   // rm
                0xc9,                   // ret
                0xe9,                   // pchl
                0xf9,                   // sphl

                // Exchange/misc 1-byte
                0xe3,                   // xthl
                0xeb,                   // xchg
                0xf3,                   // di
                0xfb,                   // ei

                // LXI rp, imm16
                0x01, 0x34, 0x12,       // lxi b, 1234h
                0x11, 0x34, 0x12,       // lxi d, 1234h
                0x21, 0x34, 0x12,       // lxi h, 1234h
                0x31, 0x34, 0x12,       // lxi sp, 1234h

                // DAD rp
                0x09,                   // dad b
                0x19,                   // dad d
                0x29,                   // dad h
                0x39,                   // dad sp

                // INX rp
                0x03,                   // inx b
                0x13,                   // inx d
                0x23,                   // inx h
                0x33,                   // inx sp

                // DCX rp
                0x0b,                   // dcx b
                0x1b,                   // dcx d
                0x2b,                   // dcx h
                0x3b,                   // dcx sp

                // POP rp2
                0xc1,                   // pop b
                0xd1,                   // pop d
                0xe1,                   // pop h
                0xf1,                   // pop psw

                // PUSH rp2
                0xc5,                   // push b
                0xd5,                   // push d
                0xe5,                   // push h
                0xf5,                   // push psw

                // INR r
                0x04,                   // inr b
                0x0c,                   // inr c
                0x14,                   // inr d
                0x1c,                   // inr e
                0x24,                   // inr h
                0x2c,                   // inr l
                0x34,                   // inr m
                0x3c,                   // inr a

                // DCR r
                0x05,                   // dcr b
                0x0d,                   // dcr c
                0x15,                   // dcr d
                0x1d,                   // dcr e
                0x25,                   // dcr h
                0x2d,                   // dcr l
                0x35,                   // dcr m
                0x3d,                   // dcr a

                // MVI r, imm8
                0x06, 0x20,             // mvi b, 20h
                0x0e, 0x20,             // mvi c, 20h
                0x16, 0x20,             // mvi d, 20h
                0x1e, 0x20,             // mvi e, 20h
                0x26, 0x20,             // mvi h, 20h
                0x2e, 0x20,             // mvi l, 20h
                0x36, 0x20,             // mvi m, 20h
                0x3e, 0x20,             // mvi a, 20h

                // MOV r, r (bcde dest)
                0x40,                   // mov b, b
                0x41,                   // mov b, c
                0x42,                   // mov b, d
                0x43,                   // mov b, e
                0x44,                   // mov b, h
                0x45,                   // mov b, l
                0x46,                   // mov b, m
                0x47,                   // mov b, a  (using mov a, b path? actually 0x47 = mov b,a)
                // Wait, 0x47 is actually in the "mov b, a" path? Let me re-check:
                // 0x40-0x47: b,b  b,c  b,d  b,e  b,h  b,l  b,m  b,a  (but 0x47 is really "mov b, a")
                // This goes into the r_bcde path (dest in bits 3-4)
                0x48,                   // mov c, b
                0x49,                   // mov c, c
                0x4a,                   // mov c, d
                0x4b,                   // mov c, e
                0x4c,                   // mov c, h
                0x4d,                   // mov c, l
                0x4e,                   // mov c, m
                0x4f,                   // mov c, a
                0x50,                   // mov d, b
                0x51,                   // mov d, c
                0x52,                   // mov d, d
                0x53,                   // mov d, e
                0x54,                   // mov d, h
                0x55,                   // mov d, l
                0x56,                   // mov d, m
                0x57,                   // mov d, a
                0x58,                   // mov e, b
                0x59,                   // mov e, c
                0x5a,                   // mov e, d
                0x5b,                   // mov e, e
                0x5c,                   // mov e, h
                0x5d,                   // mov e, l
                0x5e,                   // mov e, m
                0x5f,                   // mov e, a

                // MOV h/l, r (r_h_l dest)
                0x60,                   // mov h, b
                0x61,                   // mov h, c
                0x62,                   // mov h, d
                0x63,                   // mov h, e
                0x64,                   // mov h, h
                0x65,                   // mov h, l
                0x66,                   // mov h, m
                0x67,                   // mov h, a
                0x68,                   // mov l, b
                0x69,                   // mov l, c
                0x6a,                   // mov l, d
                0x6b,                   // mov l, e
                0x6c,                   // mov l, h
                0x6d,                   // mov l, l
                0x6e,                   // mov l, m
                0x6f,                   // mov l, a

                // MOV m, r (dest = memory)
                0x70,                   // mov m, b
                0x71,                   // mov m, c
                0x72,                   // mov m, d
                0x73,                   // mov m, e
                0x74,                   // mov m, h
                0x75,                   // mov m, l
                // 0x76 = hlt (already covered above)
                // 0x77 = mov m, a (already covered above)

                // MOV a, r
                0x78,                   // mov a, b
                0x79,                   // mov a, c
                0x7a,                   // mov a, d
                0x7b,                   // mov a, e
                0x7c,                   // mov a, h
                0x7d,                   // mov a, l
                0x7e,                   // mov a, m
                0x7f,                   // mov a, a

                // ALU operations (add/adc/sub/sbb/ana/xra/ora/cmp) with register operand
                0x80,                   // add b
                0x88,                   // adc b
                0x90,                   // sub b
                0x98,                   // sbb b
                0xa0,                   // ana b
                0xa8,                   // xra b
                0xb0,                   // ora b
                0xb8,                   // cmp b

                // ALU with all registers for one operation (add)
                0x81,                   // add c
                0x82,                   // add d
                0x83,                   // add e
                0x84,                   // add h
                0x85,                   // add l
                0x86,                   // add m
                0x87,                   // add a

                // ALUI (immediate ALU operations)
                0xc6, 0x20,             // adi 20h
                0xce, 0x20,             // aci 20h
                0xd6, 0x20,             // sui 20h
                0xde, 0x20,             // sbi 20h
                0xe6, 0x20,             // ani 20h
                0xee, 0x20,             // xri 20h
                0xf6, 0x20,             // ori 20h
                0xfe, 0x20,             // cpi 20h

                // RST instructions
                0xc7,                   // rst 0
                0xcf,                   // rst 1
                0xd7,                   // rst 2
                0xdf,                   // rst 3
                0xe7,                   // rst 4
                0xef,                   // rst 5
                0xf7,                   // rst 6
                0xff,                   // rst 7

                // Jump instructions (3-byte)
                0xc2, 0x34, 0x12,       // jnz 1234h
                0xca, 0x34, 0x12,       // jz 1234h
                0xd2, 0x34, 0x12,       // jnc 1234h
                0xda, 0x34, 0x12,       // jc 1234h
                0xe2, 0x34, 0x12,       // jpo 1234h
                0xea, 0x34, 0x12,       // jpe 1234h
                0xf2, 0x34, 0x12,       // jp 1234h
                0xfa, 0x34, 0x12,       // jm 1234h
                0xc3, 0x34, 0x12,       // jmp 1234h

                // Call instructions (3-byte)
                0xc4, 0x34, 0x12,       // cnz 1234h
                0xcc, 0x34, 0x12,       // cz 1234h
                0xd4, 0x34, 0x12,       // cnc 1234h
                0xdc, 0x34, 0x12,       // cc 1234h
                0xe4, 0x34, 0x12,       // cpo 1234h
                0xec, 0x34, 0x12,       // cpe 1234h
                0xf4, 0x34, 0x12,       // cp 1234h
                0xfc, 0x34, 0x12,       // cm 1234h
                0xcd, 0x34, 0x12,       // call 1234h

                // I/O instructions (2-byte)
                0xd3, 0x20,             // out 20h
                0xdb, 0x20,             // in 20h

                // Memory reference instructions (3-byte)
                0x22, 0x34, 0x12,       // shld 1234h
                0x32, 0x34, 0x12,       // sta 1234h
                0x2a, 0x34, 0x12,       // lhld 1234h
                0x3a, 0x34, 0x12,       // lda 1234h
        });

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < memoryStub.getSize(); i = disassembler.getNextInstructionPosition(i)) {
            DisassembledInstruction instr = disassembler.disassemble(i);
            builder.append(instr.mnemo);
            builder.append("|");
        }
        String result = builder.toString();

        String expected =
                "nop|" +
                "stax b|" +
                "stax d|" +
                "ldax b|" +
                "ldax d|" +
                "rlc|" +
                "rrc|" +
                "ral|" +
                "rar|" +
                "daa|" +
                "cma|" +
                "stc|" +
                "cmc|" +
                "hlt|" +
                "mov m, a|" +
                "rnz|" +
                "rz|" +
                "rnc|" +
                "rc|" +
                "rpo|" +
                "rpe|" +
                "rp|" +
                "rm|" +
                "ret|" +
                "pchl|" +
                "sphl|" +
                "xthl|" +
                "xchg|" +
                "di|" +
                "ei|" +
                "lxi b, 1234h|" +
                "lxi d, 1234h|" +
                "lxi h, 1234h|" +
                "lxi sp, 1234h|" +
                "dad b|" +
                "dad d|" +
                "dad h|" +
                "dad sp|" +
                "inx b|" +
                "inx d|" +
                "inx h|" +
                "inx sp|" +
                "dcx b|" +
                "dcx d|" +
                "dcx h|" +
                "dcx sp|" +
                "pop b|" +
                "pop d|" +
                "pop h|" +
                "pop psw|" +
                "push b|" +
                "push d|" +
                "push h|" +
                "push psw|" +
                "inr b|" +
                "inr c|" +
                "inr d|" +
                "inr e|" +
                "inr h|" +
                "inr l|" +
                "inr m|" +
                "inr a|" +
                "dcr b|" +
                "dcr c|" +
                "dcr d|" +
                "dcr e|" +
                "dcr h|" +
                "dcr l|" +
                "dcr m|" +
                "dcr a|" +
                "mvi b, 20h|" +
                "mvi c, 20h|" +
                "mvi d, 20h|" +
                "mvi e, 20h|" +
                "mvi h, 20h|" +
                "mvi l, 20h|" +
                "mvi m, 20h|" +
                "mvi a, 20h|" +
                "mov b, b|" +
                "mov b, c|" +
                "mov b, d|" +
                "mov b, e|" +
                "mov b, h|" +
                "mov b, l|" +
                "mov b, m|" +
                "mov b, a|" +
                "mov c, b|" +
                "mov c, c|" +
                "mov c, d|" +
                "mov c, e|" +
                "mov c, h|" +
                "mov c, l|" +
                "mov c, m|" +
                "mov c, a|" +
                "mov d, b|" +
                "mov d, c|" +
                "mov d, d|" +
                "mov d, e|" +
                "mov d, h|" +
                "mov d, l|" +
                "mov d, m|" +
                "mov d, a|" +
                "mov e, b|" +
                "mov e, c|" +
                "mov e, d|" +
                "mov e, e|" +
                "mov e, h|" +
                "mov e, l|" +
                "mov e, m|" +
                "mov e, a|" +
                "mov h, b|" +
                "mov h, c|" +
                "mov h, d|" +
                "mov h, e|" +
                "mov h, h|" +
                "mov h, l|" +
                "mov h, m|" +
                "mov h, a|" +
                "mov l, b|" +
                "mov l, c|" +
                "mov l, d|" +
                "mov l, e|" +
                "mov l, h|" +
                "mov l, l|" +
                "mov l, m|" +
                "mov l, a|" +
                "mov m, b|" +
                "mov m, c|" +
                "mov m, d|" +
                "mov m, e|" +
                "mov m, h|" +
                "mov m, l|" +
                "mov a, b|" +
                "mov a, c|" +
                "mov a, d|" +
                "mov a, e|" +
                "mov a, h|" +
                "mov a, l|" +
                "mov a, m|" +
                "mov a, a|" +
                "add b|" +
                "adc b|" +
                "sub b|" +
                "sbb b|" +
                "ana b|" +
                "xra b|" +
                "ora b|" +
                "cmp b|" +
                "add c|" +
                "add d|" +
                "add e|" +
                "add h|" +
                "add l|" +
                "add m|" +
                "add a|" +
                "adi 20h|" +
                "aci 20h|" +
                "sui 20h|" +
                "sbi 20h|" +
                "ani 20h|" +
                "xri 20h|" +
                "ori 20h|" +
                "cpi 20h|" +
                "rst 0|" +
                "rst 1|" +
                "rst 2|" +
                "rst 3|" +
                "rst 4|" +
                "rst 5|" +
                "rst 6|" +
                "rst 7|" +
                "jnz 1234h|" +
                "jz 1234h|" +
                "jnc 1234h|" +
                "jc 1234h|" +
                "jpo 1234h|" +
                "jpe 1234h|" +
                "jp 1234h|" +
                "jm 1234h|" +
                "jmp 1234h|" +
                "cnz 1234h|" +
                "cz 1234h|" +
                "cnc 1234h|" +
                "cc 1234h|" +
                "cpo 1234h|" +
                "cpe 1234h|" +
                "cp 1234h|" +
                "cm 1234h|" +
                "call 1234h|" +
                "out 20h|" +
                "in 20h|" +
                "shld 1234h|" +
                "sta 1234h|" +
                "lhld 1234h|" +
                "lda 1234h|";

        assertEquals(expected, result);
    }

    @Test
    public void testDecoder() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x00});
        DecodedInstruction instr = decoder.decode(0);
        assertEquals(1, instr.image.length);
    }

    @Test
    public void testDecoderThreeByteInstruction() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0xC3, 0x34, 0x12});
        DecodedInstruction instr = decoder.decode(0);
        assertEquals(3, instr.image.length);
    }

    @Test
    public void testDecoderTwoByteInstruction() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x06, 0x20});
        DecodedInstruction instr = decoder.decode(0);
        assertEquals(2, instr.image.length);
    }

    @Test
    public void testGetNextInstructionPositionOneByteInstruction() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x00});
        assertEquals(1, disassembler.getNextInstructionPosition(0));
    }

    @Test
    public void testGetNextInstructionPositionTwoByteInstruction() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x06, 0x20});
        assertEquals(2, disassembler.getNextInstructionPosition(0));
    }

    @Test
    public void testGetNextInstructionPositionThreeByteInstruction() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0xC3, 0x34, 0x12});
        assertEquals(3, disassembler.getNextInstructionPosition(0));
    }

    @Test
    public void testDisassembleReturnsCode() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0xC3, 0x34, 0x12});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("C3 34 12", instr.opCode);
    }

    @Test
    public void testDisassembleOneByteCode() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("00", instr.opCode);
    }

    @Test
    public void testDisassembleTwoByteCode() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x06, 0xAB});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("06 AB", instr.opCode);
    }

    @Test
    public void testDecoderCachingReturnsSameResultForSameAddress() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x00});
        DecodedInstruction first = decoder.decode(0);
        DecodedInstruction second = decoder.decode(0);
        // Cached result should be the same object
        assertEquals(first, second);
    }

    @Test
    public void testDisassembleCachingReturnsSameResult() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x00});
        DisassembledInstruction first = disassembler.disassemble(0);
        DisassembledInstruction second = disassembler.disassemble(0);
        assertEquals(first.mnemo, second.mnemo);
        assertEquals(first.opCode, second.opCode);
    }

    @Test
    public void testDisassembleMultiplePositions() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{0x00, 0xC3, 0x34, 0x12});
        DisassembledInstruction first = disassembler.disassemble(0);
        DisassembledInstruction second = disassembler.disassemble(1);
        assertEquals("nop", first.mnemo);
        assertEquals("jmp 1234h", second.mnemo);
    }

    @Test
    public void testDisassembleAluWithAllRegisters() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{
                0x88,  // adc b
                0x89,  // adc c
                0x8a,  // adc d
                0x8b,  // adc e
                0x8c,  // adc h
                0x8d,  // adc l
                0x8e,  // adc m
                0x8f,  // adc a
                0x90,  // sub b
                0x91,  // sub c
                0x92,  // sub d
                0x93,  // sub e
                0x94,  // sub h
                0x95,  // sub l
                0x96,  // sub m
                0x97,  // sub a
                0x98,  // sbb b
                0x99,  // sbb c
                0x9a,  // sbb d
                0x9b,  // sbb e
                0x9c,  // sbb h
                0x9d,  // sbb l
                0x9e,  // sbb m
                0x9f,  // sbb a
                0xa0,  // ana b
                0xa1,  // ana c
                0xa2,  // ana d
                0xa3,  // ana e
                0xa4,  // ana h
                0xa5,  // ana l
                0xa6,  // ana m
                0xa7,  // ana a
                0xa8,  // xra b
                0xa9,  // xra c
                0xaa,  // xra d
                0xab,  // xra e
                0xac,  // xra h
                0xad,  // xra l
                0xae,  // xra m
                0xaf,  // xra a
                0xb0,  // ora b
                0xb1,  // ora c
                0xb2,  // ora d
                0xb3,  // ora e
                0xb4,  // ora h
                0xb5,  // ora l
                0xb6,  // ora m
                0xb7,  // ora a
                0xb8,  // cmp b
                0xb9,  // cmp c
                0xba,  // cmp d
                0xbb,  // cmp e
                0xbc,  // cmp h
                0xbd,  // cmp l
                0xbe,  // cmp m
                0xbf,  // cmp a
        });

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < memoryStub.getSize(); i = disassembler.getNextInstructionPosition(i)) {
            builder.append(disassembler.disassemble(i).mnemo);
            builder.append("|");
        }
        String result = builder.toString();

        String expected =
                "adc b|adc c|adc d|adc e|adc h|adc l|adc m|adc a|" +
                "sub b|sub c|sub d|sub e|sub h|sub l|sub m|sub a|" +
                "sbb b|sbb c|sbb d|sbb e|sbb h|sbb l|sbb m|sbb a|" +
                "ana b|ana c|ana d|ana e|ana h|ana l|ana m|ana a|" +
                "xra b|xra c|xra d|xra e|xra h|xra l|xra m|xra a|" +
                "ora b|ora c|ora d|ora e|ora h|ora l|ora m|ora a|" +
                "cmp b|cmp c|cmp d|cmp e|cmp h|cmp l|cmp m|cmp a|";

        assertEquals(expected, result);
    }
}

