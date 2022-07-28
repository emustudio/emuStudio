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
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.Decoder;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.cpu.zilogZ80.gui.DecoderImpl;
import net.emustudio.plugins.cpu.zilogZ80.gui.DisassemblerImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisassemblerTest {

    ByteMemoryStub memoryStub;
    Decoder decoder;
    Disassembler disassembler;

    @Before
    public void setUp() {
        memoryStub = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        decoder = new DecoderImpl(memoryStub);
        disassembler = new DisassemblerImpl(memoryStub, decoder);
    }


    @Test
    public void testDisassemble() throws InvalidInstructionException {
        memoryStub.setMemory(new short[]{
            0,                                           // nop
            0x08,                                        // ex af, af'
            0x10, 0x20,                                  // djnz 20
            0x18, 0x20,                                  // jr 20
            0x20, 0x20,                                  // jr nz, 20
            0x28, 0x20,                                  // jr z, 20h
            0x30, 0x20,                                  // jr nc, 20h
            0x38, 0x20,                                  // jr c, 20h
            0x01, 0x34, 0x12,  // 10                     // ld bc, 0x1234
            0x11, 0x34, 0x12,                            // ld de, 0x1234
            0x21, 0x34, 0x12,                            // ld hl, 0x1234
            0x31, 0x34, 0x12,                            // ld sp, 0x1234
            0x09,                                        // add hl, bc
            0x19,                                        // add hl, de
            0x29,                                        // add hl, hl
            0x39,                                        // add hl, sp
            0x02,                                        // ld (bc), a
            0x12,                                        // ld (de), a
            0x0A, // 20                                  // ld a, (bc)
            0x1A,                                        // ld a, (de)
            0x22, 0x34, 0x12,                            // ld (0x1234), hl\n"  //  0
            0x32, 0x34, 0x12,                            // ld (0x1234), a
            0x2A, 0x34, 0x12,                            // ld hl, (0x1234)
            0x3A, 0x34, 0x12,                            // ld a, (0x1234)
            0x03,                                        // inc bc
            0x13,                                        // inc de
            0x23, // 30                                  // inc hl
            0x33,                                        // inc sp
            0x0B,                                        // dec bc
            0x1B,                                        // dec de
            0x2B,                                        // dec hl
            0x3B,                                        // dec sp
            0x04,                                        // inc b
            0x0C,                                        // inc c
            0x14,                                        // inc d
            0x1C,                                        // inc e
            0x24,                                        // inc h
            0x2C,                                        // inc l
            0x3C,                                        // inc a
            0x05,                                        // dec b
            0x0D,                                        // dec c
            0x15,                                        // dec d
            0x1D, // 40                                  // dec e
            0x25,                                        // dec h
            0x2D,                                        // dec l
            0x3D,                                        // dec a
            0x06, 0x20,                                  // ld b, 0x20
            0x0E, 0x20,                                  // ld c, 0x20
            0x16, 0x20,                                  // ld d, 0x20
            0x1E, 0x20,                                  // ld e, 0x20
            0x26, 0x20,                                  // ld h, 0x20
            0x2E, 0x20,                                  // ld l, 0x20
            0x07, // 50                                  // rlca
            0x0F,                                        // rrca
            0x17,                                        // rla
            0x1F,                                        // rra
            0x27,                                        // daa
            0x2F,                                        // cpl
            0x37,                                        // scf
            0x3F,                                        // ccf
            0x40,                                        // ld b, b
            0x41,                                        // ld b, c
            0x42,                                        // ld b, d
            0x43,                                        // ld b, e
            0x44,                                        // ld b, h
            0x45,                                        // ld b, l
            0x47,                                        // ld b, a
            0x70,                                        // ld (hl), b
            0x71, // 60                                  // ld (hl), c
            0x72,                                        // ld (hl), d
            0x73,                                        // ld (hl), e
            0x74,                                        // ld (hl), h
            0x75,                                        // ld (hl), l
            0x77,                                        // ld (hl), a
            0x76,                                        // halt
            0x80,                                        // add a, b
            0x86,                                        // add a, (hl)
            0x89,                                        // adc a, c
            0x8E,                                        // adc a, (hl)
            0x90,                                        // sub b
            0x96,                                        // sub (hl)
            0x98,                                        // sbc a, b
            0x9E,                                        // sbc a, (hl)
            0xA0,                                        // and b
            0xA6, // 70                                  // and (hl)
            0xA8,                                        // xor b
            0xAE,                                        // xor (hl)
            0xB0,                                        // or b
            0xB6,                                        // or (hl)
            0xB8,                                        // cp b
            0xBE,                                        // cp (hl)
            0xC0,                                        // ret nz
            0xC8,                                        // ret z
            0xD0,                                        // ret nc
            0xD8,                                        // ret c
            0xE0, // 7b                                  // ret po
            0xE8,                                        // ret pe
            0xF0,                                        // ret p
            0xF8,                                        // ret m
            0xC1,                                        // pop bc
            0xD1, // 80                                  // pop de
            0xE1,                                        // pop hl
            0xF1,                                        // pop af
            0xC9,                                        // ret
            0xD9,                                        // exx
            0xE9,                                        // jp hl
            0xE9,                                        // jp (hl)
            0xF9,                                        // ld sp, hl
            0xC2, 0x34, 0x12,                            // jp nz, 0x1234
            0xCA, 0x34, 0x12,                            // jp z, 0x1234
            0xD2, 0x34, 0x12,                            // jp nc, 0x1234
            0xDA, 0x34, 0x12,                            // jp c, 0x1234
            0xE2, 0x34, 0x12,                            // jp po, 0x1234
            0xEA, 0x34, 0x12,                            // jp pe, 0x1234
            0xF2, 0x34, 0x12,                            // jp p, 0x1234
            0xFA, 0x34, 0x12,                            // jp m, 0x1234
            0xC3, 0x34, 0x12,                            // jp 0x1234
            0xD3, 0x20,                                  // out (0x20), a
            0xDB, 0x20,                                  // in a, (0x20)
            0xE3,                                        // ex (sp), hl
            0xEB,                                        // ex de, hl
            0xF3,                                        // di
            0xFB,                                        // ei
            0xC4, 0x34, 0x12,                            // call nz, 0x1234
            0xCC, 0x34, 0x12,                            // call z, 0x1234
            0xD4, 0x34, 0x12,                            // call nc, 0x1234
            0xDC, 0x34, 0x12,                            // call c, 0x1234
            0xE4, 0x34, 0x12,                            // call po, 0x1234
            0xEC, 0x34, 0x12,                            // call pe, 0x1234
            0xF4, 0x34, 0x12,                            // call p, 0x1234
            0xFC, 0x34, 0x12,                            // call m, 0x1234
            0xC5,                                        // push bc
            0xD5,                                        // push de
            0xE5,                                        // push hl
            0xF5,                                        // push af
            0xCD, 0x34, 0x12,                            // call 0x1234
            0xC6, 0x20,                                  // add a, 0x20
            0xCE, 0x20,                                  // adc a, 0x20
            0xD6, 0x20,                                  // sub 0x20
            0xDE, 0x20,                                  // sbc a, 0x20
            0xE6, 0x20,                                  // and 0x20
            0xEE, 0x20,                                  // xor 0x20
            0xF6, 0x20,                                  // or 0x20
            0xFE, 0x20,                                  // cp 0x20
            0xC7,                                        // rst 0
            0xCF,                                        // rst 8
            0xD7,                                        // rst 10h
            0xDF,                                        // rst 18h
            0xE7,                                        // rst 20h
            0xEF,                                        // rst 28h
            0xF7,                                        // rst 30h
            0xFF,                                        // rst 38h
            0xCB, 0x00,                                  // rlc b
            0xCB, 0x06,                                  // rlc (HL)
            0xCB, 0x09,                                  // rrc c
            0xCB, 0x0E,                                  // rrc (HL)
            0xCB, 0x12,                                  // rl d
            0xCB, 0x16,                                  // rl (HL)
            0xCB, 0x1B,                                  // rr e
            0xCB, 0x1E,                                  // rr (hl)
            0xCB, 0x24,                                  // sla h
            0xCB, 0x26,                                  // sla (hl)
            0xCB, 0x2D,                                  // sra l
            0xCB, 0x2E,                                  // sra (hl)
            0xCB, 0x30,                                  // sll b
            0xCB, 0x36,                                  // sll (hl)
            0xCB, 0x39,                                  // srl c
            0xCB, 0x3E,                                  // srl (hl)
            0xCB, 0x68,                                  // bit 5, b
            0xCB, 0x6E,                                  // bit 5, (hl)
            0xCB, 0xA9,                                  // res 5, c
            0xCB, 0xB6,                                  // res 6, (hl)
            0xCB, 0xFA,                                  // set 7, d
            0xCB, 0xFE,                                  // set 7, (hl)
            0xED, 0x58,                                  // in e, (c)
            0xED, 0x70,                                  // in (c)
            0xED, 0x61,                                  // out (c), h
            0xED, 0x71,                                  // out (c), 0
            0xED, 0x42,                                  // sbc hl, bc
            0xED, 0x52,                                  // sbc hl, de
            0xED, 0x62,                                  // sbc hl, hl
            0xED, 0x72,                                  // sbc hl, sp
            0xED, 0x4A,                                  // adc hl, bc
            0xED, 0x5A,                                  // adc hl, de
            0xED, 0x6A,                                  // adc hl, hl
            0xED, 0x7A,                                  // adc hl, sp
            0xED, 0x43, 0x34, 0x12,                      // ld (1234h), bc
            0xED, 0x53, 0x34, 0x12,                      // ld (1234h), de
            0x22, 0x34, 0x12,  // 0xED, 0x63, 0x34, 0x12 // ld (1234h), hl
            0xED, 0x73, 0x34, 0x12,                      // ld (1234h), sp
            0xED, 0x4B, 0x34, 0x12,                      // ld bc, (0x1234)
            0xED, 0x5B, 0x34, 0x12,                      // ld de, (0x1234)
            0x2A, 0x34, 0x12,  // 0xED, 0x6B, 0x34, 0x12 // ld hl, (0x1234)
            0xED, 0x7B, 0x34, 0x12,                      // ld sp, (0x1234)
            0xED, 0x44,                                  // neg
            0xED, 0x45,                                  // retn
            0xED, 0x4D,                                  // reti
            0xED, 0x46,                                  // im 0
            0xED, 0x4E,                                  // im 0/1
            0xED, 0x56,                                  // im 1
            0xED, 0x5E,                                  // im 2
            0xED, 0x47,                                  // ld i, a
            0xED, 0x4F,                                  // ld r, a
            0xED, 0x57,                                  // ld a, i
            0xED, 0x5F,                                  // ld a, r
            0xED, 0x67,                                  // rrd
            0xED, 0x6F,                                  // rld
            0xED, 0xA0,                                  // ldi
            0xED, 0xA8,                                  // ldd
            0xED, 0xB0,                                  // ldir
            0xED, 0xB8,                                  // lddr
            0xED, 0xA1,                                  // cpi
            0xED, 0xA9,                                  // cpd
            0xED, 0xB1,                                  // cpir
            0xED, 0xB9,                                  // cpdr
            0xED, 0xA2,                                  // ini
            0xED, 0xAA,                                  // ind
            0xED, 0xB2,                                  // inir
            0xED, 0xBA,                                  // indr
            0xED, 0xA3,                                  // outi
            0xED, 0xAB,                                  // outd
            0xED, 0xB3,                                  // otir
            0xED, 0xBB,                                  // otdr
            0xDD, 0xCB, 0x20, 0x00,                      // rlc (ix+0x20), b
            0xFD, 0xCB, 0x20, 0x00,                      // rlc (iy+0x20), b
            0xDD, 0xCB, 0x20, 0x06,                      // rlc (ix+0x20)
            0xFD, 0xCB, 0x20, 0x06,                      // rlc (iy+0x20)
            0xDD, 0xCB, 0x20, 0x09,                      // rrc (ix+0x20), c
            0xFD, 0xCB, 0x20, 0x09,                      // rrc (iy+0x20), c
            0xDD, 0xCB, 0x20, 0x0E,                      // rrc (ix+0x20)
            0xFD, 0xCB, 0x20, 0x0E,                      // rrc (iy+0x20)
            0xDD, 0xCB, 0x20, 0x12,                      // rl (ix+0x20), d
            0xFD, 0xCB, 0x20, 0x12,                      // rl (iy+0x20), d
            0xDD, 0xCB, 0x20, 0x16,                      // rl (ix+0x20)
            0xFD, 0xCB, 0x20, 0x16,                      // rl (iy+0x20)
            0xDD, 0xCB, 0x20, 0x1B,                      // rr (ix+0x20), e
            0xFD, 0xCB, 0x20, 0x1B,                      // rr (iy+0x20), e
            0xDD, 0xCB, 0x20, 0x1E,                      // rr (ix+0x20)
            0xFD, 0xCB, 0x20, 0x1E,                      // rr (iy+0x20)
            0xDD, 0xCB, 0x20, 0x24,                      // sla (ix+0x20), h
            0xFD, 0xCB, 0x20, 0x24,                      // sla (iy+0x20), h
            0xDD, 0xCB, 0x20, 0x26,                      // sla (ix+0x20)
            0xFD, 0xCB, 0x20, 0x26,                      // sla (iy+0x20)
            0xDD, 0xCB, 0x20, 0x2D,                      // sra (ix+0x20), l
            0xFD, 0xCB, 0x20, 0x2D,                      // sra (iy+0x20), l
            0xDD, 0xCB, 0x20, 0x2E,                      // sra (ix+0x20)
            0xFD, 0xCB, 0x20, 0x2E,                      // sra (iy+0x20)
            0xDD, 0xCB, 0x20, 0x30,                      // sll (ix+0x20), b
            0xFD, 0xCB, 0x20, 0x30,                      // sll (iy+0x20), b
            0xDD, 0xCB, 0x20, 0x36,                      // sll (ix+0x20)
            0xFD, 0xCB, 0x20, 0x36,                      // sll (iy+0x20)
            0xDD, 0xCB, 0x20, 0x39,                      // srl (ix+0x20), c
            0xFD, 0xCB, 0x20, 0x39,                      // srl (iy+0x20), c
            0xDD, 0xCB, 0x20, 0x3E,                      // srl (ix+0x20)
            0xFD, 0xCB, 0x20, 0x3E,                      // srl (iy+0x20)
            0xDD, 0xCB, 0x20, 0x5E,                      // bit 3, (ix+0x20)
            0xFD, 0xCB, 0x20, 0x5E,                      // bit 3, (iy+0x20)
            0xDD, 0xCB, 0x20, 0xA8,                      // res 5, (ix+0x20), b
            0xFD, 0xCB, 0x20, 0xA8,                      // res 5, (iy+0x20), b
            0xDD, 0xCB, 0x20, 0xAE,                      // res 5, (ix+0x20)
            0xFD, 0xCB, 0x20, 0xAE,                      // res 5, (iy+0x20)
            0xDD, 0xCB, 0x20, 0xF1,                      // set 6, (ix+0x20), c
            0xFD, 0xCB, 0x20, 0xF1,                      // set 6, (iy+0x20), c
            0xDD, 0xCB, 0x20, 0xF6,                      // set 6, (ix+0x20)
            0xFD, 0xCB, 0x20, 0xF6,                      // set 6, (iy+0x20)
            0xDD, 0x21, 0x34, 0x12,                      // ld ix, 0x1234
            0xFD, 0x21, 0x34, 0x12,                      // ld iy, 0x1234
            0xDD, 0x09,                                  // add ix, bc
            0xFD, 0x09,                                  // add iy, bc
            0xDD, 0x19,                                  // add ix, de
            0xFD, 0x19,                                  // add iy, de
            0xDD, 0x29,                                  // add ix, ix
            0xFD, 0x29,                                  // add iy, iy
            0xDD, 0x39,                                  // add ix, sp
            0xFD, 0x39,                                  // add iy, sp
            0xDD, 0x22, 0x34, 0x12,                      // ld (0x1234), ix
            0xFD, 0x22, 0x34, 0x12,                      // ld (0x1234), iy
            0xDD, 0x2A, 0x34, 0x12,                      // ld ix, (0x1234)
            0xFD, 0x2A, 0x34, 0x12,                      // ld iy, (0x1234)
            0xDD, 0x23,                                  // inc ix
            0xFD, 0x23,                                  // inc iy
            0xDD, 0x2B,                                  // dec ix
            0xFD, 0x2B,                                  // dec iy
            0xDD, 0x24,                                  // inc ixh
            0xFD, 0x24,                                  // inc iyh
            0xDD, 0x2C,                                  // inc ixl
            0xFD, 0x2C,                                  // inc iyl
            0xDD, 0x34, 0x20,                            // inc (ix+0x20)
            0xFD, 0x34, 0x20,                            // inc (iy+0x20)
            0xDD, 0x25,                                  // dec ixh
            0xFD, 0x25,                                  // dec iyh
            0xDD, 0x2D,                                  // dec ixl
            0xFD, 0x2D,                                  // dec iyl
            0xDD, 0x35, 0x20,                            // dec (ix+0x20)
            0xFD, 0x35, 0x20,                            // dec (iy+0x20)
            0xDD, 0x26, 0x20,                            // ld ixh, 0x20
            0xDD, 0x2E, 0x20,                            // ld ixl, 0x20
            0xFD, 0x26, 0x20,                            // ld iyh, 0x20
            0xFD, 0x2E, 0x20,                            // ld iyl, 0x20
            0xDD, 0x36, 0x20, 0x20,                      // ld (ix+0x20), 0x20
            0xFD, 0x36, 0x20, 0x20,                      // ld (iy+0x20), 0x20
            0xDD, 0x60,                                  // ld ixh, b
            0xDD, 0x64,                                  // ld ixh, ixh
            0xDD, 0x65,                                  // ld ixh, ixl
            0xFD, 0x61,                                  // ld iyh, c
            0xFD, 0x64,                                  // ld iyh, iyh
            0xFD, 0x65,                                  // ld iyh, iyl
            0xDD, 0x6A,                                  // ld ixl, d
            0xDD, 0x6C,                                  // ld ixl, ixh
            0xDD, 0x6D,                                  // ld ixl, ixl
            0xFD, 0x6B,                                  // ld iyl, e
            0xFD, 0x6C,                                  // ld iyl, iyh
            0xFD, 0x6D,                                  // ld iyl, iyl
            0xDD, 0x72, 0x20,                            // ld (ix+0x20), d
            0xDD, 0x74, 0x20,                            // ld (ix+0x20), h
            0xDD, 0x75, 0x20,                            // ld (ix+0x20), l
            0xFD, 0x73, 0x20,                            // ld (iy+0x20), e
            0xFD, 0x74, 0x20,                            // ld (iy+0x20), h
            0xFD, 0x75, 0x20,                            // ld (iy+0x20), l
            0xDD, 0x7C,                                  // ld a, ixh
            0xFD, 0x7C,                                  // ld a, iyh
            0xDD, 0x7D,                                  // ld a, ixl
            0xFD, 0x7D,                                  // ld a, iyl
            0xDD, 0x7E, 0x20,                            // ld a, (ix+0x20)
            0xFD, 0x7E, 0x20,                            // ld a, (iy+0x20)
            0xDD, 0x66, 0x20,                            // ld h, (ix+0x20)
            0xDD, 0x6E, 0x20,                            // ld l, (ix+0x20)
            0xFD, 0x66, 0x20,                            // ld h, (iy+0x20)
            0xFD, 0x6E, 0x20,                            // ld l, (iy+0x20)
            0xDD, 0x84,                                  // add a, ixh
            0xDD, 0x85,                                  // add a, ixl
            0xDD, 0x86, 0x20,                            // add a, (ix + 0x20)
            0xFD, 0x84,                                  // add a, iyh
            0xFD, 0x85,                                  // add a, iyl
            0xFD, 0x86, 0x20,                            // add a, (iy+0x20)
            0xDD, 0x8C,                                  // adc a, ixh
            0xDD, 0x8D,                                  // adc a, ixl
            0xDD, 0x8E, 0x20,                            // adc a, (ix+0x20)
            0xFD, 0x8C,                                  // adc a, iyh
            0xFD, 0x8D,                                  // adc a, iyl
            0xFD, 0x8E, 0x20,                            // adc a, (iy+0x20)
            0xDD, 0x94,                                  // sub ixh
            0xDD, 0x95,                                  // sub ixl
            0xDD, 0x96, 0x20,                            // sub (ix+0x20)
            0xFD, 0x94,                                  // sub iyh
            0xFD, 0x95,                                  // sub iyl
            0xFD, 0x96, 0x20,                            // sub (iy+0x20)
            0xDD, 0x9C,                                  // sbc a, ixh
            0xDD, 0x9D,                                  // sbc a, ixl
            0xDD, 0x9E, 0x20,                            // sbc a, (ix+0x20)
            0xFD, 0x9C,                                  // sbc a, iyh
            0xFD, 0x9D,                                  // sbc a, iyl
            0xFD, 0x9E, 0x20,                            // sbc a, (iy+0x20)
            0xDD, 0xA4,                                  // and ixh
            0xDD, 0xA5,                                  // and ixl
            0xDD, 0xA6, 0x20,                            // and (ix+0x20)
            0xFD, 0xA4,                                  // and iyh
            0xFD, 0xA5,                                  // and iyl
            0xFD, 0xA6, 0x20,                            // and (iy+0x20)
            0xDD, 0xAC,                                  // xor ixh
            0xDD, 0xAD,                                  // xor ixl
            0xDD, 0xAE, 0x20,                            // xor (ix+0x20)
            0xFD, 0xAC,                                  // xor iyh
            0xFD, 0xAD,                                  // xor iyl
            0xFD, 0xAE, 0x20,                            // xor (iy+0x20)
            0xDD, 0xB4,                                  // or ixh
            0xDD, 0xB5,                                  // or ixl
            0xDD, 0xB6, 0x20,                            // or (ix+0x20)
            0xFD, 0xB4,                                  // or iyh
            0xFD, 0xB5,                                  // or iyl
            0xFD, 0xB6, 0x20,                            // or (iy+0x20)
            0xDD, 0xBC,                                  // cp ixh
            0xDD, 0xBD,                                  // cp ixl
            0xDD, 0xBE, 0x20,                            // cp (ix+0x20)
            0xFD, 0xBC,                                  // cp iyh
            0xFD, 0xBD,                                  // cp iyl
            0xFD, 0xBE, 0x20,                            // cp (iy+0x20)
            0xDD, 0xE1,                                  // pop ix
            0xFD, 0xE1,                                  // pop iy
            0xDD, 0xE9,                                  // jp (ix)
            0xFD, 0xE9,                                  // jp (iy)
            0xDD, 0xF9,                                  // ld sp, ix
            0xFD, 0xF9,                                  // ld sp, iy
            0xDD, 0xE3,                                  // ex (sp), ix
            0xFD, 0xE3,                                  // ex (sp), iy
            0xDD, 0xE5,                                  // push ix
            0xFD, 0xE5                                   // push iy
        });

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < memoryStub.getSize(); i = disassembler.getNextInstructionPosition(i)) {
            builder.append(disassembler.disassemble(i).getMnemo());
        }
        String result = builder.toString();
        assertEquals(
            "nop" +
                "ex af, af'" +
                "djnz 20h" +
                "jr 20h" +
                "jr nz, 20h" +
                "jr z, 20h" +
                "jr nc, 20h" +
                "jr c, 20h" +
                "ld bc, 1234h" +
                "ld de, 1234h" +
                "ld hl, 1234h" +
                "ld sp, 1234h" +
                "add hl, bc" +
                "add hl, de" +
                "add hl, hl" +
                "add hl, sp" +
                "ld (bc), a" +
                "ld (de), a" +
                "ld a, (bc)" +
                "ld a, (de)" +
                "ld (1234h), hl" +
                "ld (1234h), a" +
                "ld hl, (1234h)" +
                "ld a, (1234h)" +
                "inc bc" +
                "inc de" +
                "inc hl" +
                "inc sp" +
                "dec bc" +
                "dec de" +
                "dec hl" +
                "dec sp" +
                "inc b" +
                "inc c" +
                "inc d" +
                "inc e" +
                "inc h" +
                "inc l" +
                "inc a" +
                "dec b" +
                "dec c" +
                "dec d" +
                "dec e" +
                "dec h" +
                "dec l" +
                "dec a" +
                "ld b, 20h" +
                "ld c, 20h" +
                "ld d, 20h" +
                "ld e, 20h" +
                "ld h, 20h" +
                "ld l, 20h" +
                "rlca" +
                "rrca" +
                "rla" +
                "rra" +
                "daa" +
                "cpl" +
                "scf" +
                "ccf" +
                "ld b, b" +
                "ld b, c" +
                "ld b, d" +
                "ld b, e" +
                "ld b, h" +
                "ld b, l" +
                "ld b, a" +
                "ld (hl), b" +
                "ld (hl), c" +
                "ld (hl), d" +
                "ld (hl), e" +
                "ld (hl), h" +
                "ld (hl), l" +
                "ld (hl), a" +
                "halt" +
                "add a, b" +
                "add a, (hl)" +
                "adc a, c" +
                "adc a, (hl)" +
                "sub b" +
                "sub (hl)" +
                "sbc a, b" +
                "sbc a, (hl)" +
                "and b" +
                "and (hl)" +
                "xor b" +
                "xor (hl)" +
                "or b" +
                "or (hl)" +
                "cp b" +
                "cp (hl)" +
                "ret nz" +
                "ret z" +
                "ret nc" +
                "ret c" +
                "ret po" +
                "ret pe" +
                "ret p" +
                "ret m" +
                "pop bc" +
                "pop de" +
                "pop hl" +
                "pop af" +
                "ret" +
                "exx" +
                "jp (hl)" +
                "jp (hl)" +
                "ld sp, hl" +
                "jp nz, 1234h" +
                "jp z, 1234h" +
                "jp nc, 1234h" +
                "jp c, 1234h" +
                "jp po, 1234h" +
                "jp pe, 1234h" +
                "jp p, 1234h" +
                "jp m, 1234h" +
                "jp 1234h" +
                "out (20h), a" +
                "in a, (20h)" +
                "ex (sp), hl" +
                "ex de, hl" +
                "di" +
                "ei" +
                "call nz, 1234h" +
                "call z, 1234h" +
                "call nc, 1234h" +
                "call c, 1234h" +
                "call po, 1234h" +
                "call pe, 1234h" +
                "call p, 1234h" +
                "call m, 1234h" +
                "push bc" +
                "push de" +
                "push hl" +
                "push af" +
                "call 1234h" +
                "add a, 20h" +
                "adc a, 20h" +
                "sub 20h" +
                "sbc a, 20h" +
                "and 20h" +
                "xor 20h" +
                "or 20h" +
                "cp 20h" +
                "rst 00h" +
                "rst 08h" +
                "rst 10h" +
                "rst 18h" +
                "rst 20h" +
                "rst 28h" +
                "rst 30h" +
                "rst 38h" +
                "rlc b" +
                "rlc (hl)" +
                "rrc c" +
                "rrc (hl)" +
                "rl d" +
                "rl (hl)" +
                "rr e" +
                "rr (hl)" +
                "sla h" +
                "sla (hl)" +
                "sra l" +
                "sra (hl)" +
                "sll b" +
                "sll (hl)" +
                "srl c" +
                "srl (hl)" +
                "bit 5, b" +
                "bit 5, (hl)" +
                "res 5, c" +
                "res 6, (hl)" +
                "set 7, d" +
                "set 7, (hl)" +
                "in e, (c)" +
                "in (c)" +
                "out (c), h" +
                "out (c), 0" +
                "sbc hl, bc" +
                "sbc hl, de" +
                "sbc hl, hl" +
                "sbc hl, sp" +
                "adc hl, bc" +
                "adc hl, de" +
                "adc hl, hl" +
                "adc hl, sp" +
                "ld (1234h), bc" +
                "ld (1234h), de" +
                "ld (1234h), hl" +
                "ld (1234h), sp" +
                "ld bc, (1234h)" +
                "ld de, (1234h)" +
                "ld hl, (1234h)" +
                "ld sp, (1234h)" +
                "neg" +
                "retn" +
                "reti" +
                "im 0" +
                "im 0/1" +
                "im 1" +
                "im 2" +
                "ld i, a" +
                "ld r, a" +
                "ld a, i" +
                "ld a, r" +
                "rrd" +
                "rld" +
                "ldi" +
                "ldd" +
                "ldir" +
                "lddr" +
                "cpi" +
                "cpd" +
                "cpir" +
                "cpdr" +
                "ini" +
                "ind" +
                "inir" +
                "indr" +
                "outi" +
                "outd" +
                "otir" +
                "otdr" +
                "rlc (ix+20h), b" +
                "rlc (iy+20h), b" +
                "rlc (ix+20h)" +
                "rlc (iy+20h)" +
                "rrc (ix+20h), c" +
                "rrc (iy+20h), c" +
                "rrc (ix+20h)" +
                "rrc (iy+20h)" +
                "rl (ix+20h), d" +
                "rl (iy+20h), d" +
                "rl (ix+20h)" +
                "rl (iy+20h)" +
                "rr (ix+20h), e" +
                "rr (iy+20h), e" +
                "rr (ix+20h)" +
                "rr (iy+20h)" +
                "sla (ix+20h), h" +
                "sla (iy+20h), h" +
                "sla (ix+20h)" +
                "sla (iy+20h)" +
                "sra (ix+20h), l" +
                "sra (iy+20h), l" +
                "sra (ix+20h)" +
                "sra (iy+20h)" +
                "sll (ix+20h), b" +
                "sll (iy+20h), b" +
                "sll (ix+20h)" +
                "sll (iy+20h)" +
                "srl (ix+20h), c" +
                "srl (iy+20h), c" +
                "srl (ix+20h)" +
                "srl (iy+20h)" +
                "bit 3, (ix+20h)" +
                "bit 3, (iy+20h)" +
                "res 5, (ix+20h), b" +
                "res 5, (iy+20h), b" +
                "res 5, (ix+20h)" +
                "res 5, (iy+20h)" +
                "set 6, (ix+20h), c" +
                "set 6, (iy+20h), c" +
                "set 6, (ix+20h)" +
                "set 6, (iy+20h)" +
                "ld ix, 1234h" +
                "ld iy, 1234h" +
                "add ix, bc" +
                "add iy, bc" +
                "add ix, de" +
                "add iy, de" +
                "add ix, ix" +
                "add iy, iy" +
                "add ix, sp" +
                "add iy, sp" +
                "ld (1234h), ix" +
                "ld (1234h), iy" +
                "ld ix, (1234h)" +
                "ld iy, (1234h)" +
                "inc ix" +
                "inc iy" +
                "dec ix" +
                "dec iy" +
                "inc ixh" +
                "inc iyh" +
                "inc ixl" +
                "inc iyl" +
                "inc (ix+20h)" +
                "inc (iy+20h)" +
                "dec ixh" +
                "dec iyh" +
                "dec ixl" +
                "dec iyl" +
                "dec (ix+20h)" +
                "dec (iy+20h)" +
                "ld ixh, 20h" +
                "ld ixl, 20h" +
                "ld iyh, 20h" +
                "ld iyl, 20h" +
                "ld (ix+20h), 20h" +
                "ld (iy+20h), 20h" +
                "ld ixh, b" +
                "ld ixh, ixh" +
                "ld ixh, ixl" +
                "ld iyh, c" +
                "ld iyh, iyh" +
                "ld iyh, iyl" +
                "ld ixl, d" +
                "ld ixl, ixh" +
                "ld ixl, ixl" +
                "ld iyl, e" +
                "ld iyl, iyh" +
                "ld iyl, iyl" +
                "ld (ix+20h), d" +
                "ld (ix+20h), h" +
                "ld (ix+20h), l" +
                "ld (iy+20h), e" +
                "ld (iy+20h), h" +
                "ld (iy+20h), l" +
                "ld a, ixh" +
                "ld a, iyh" +
                "ld a, ixl" +
                "ld a, iyl" +
                "ld a, (ix+20h)" +
                "ld a, (iy+20h)" +
                "ld h, (ix+20h)" +
                "ld l, (ix+20h)" +
                "ld h, (iy+20h)" +
                "ld l, (iy+20h)" +
                "add a, ixh" +
                "add a, ixl" +
                "add a, (ix+20h)" +
                "add a, iyh" +
                "add a, iyl" +
                "add a, (iy+20h)" +
                "adc a, ixh" +
                "adc a, ixl" +
                "adc a, (ix+20h)" +
                "adc a, iyh" +
                "adc a, iyl" +
                "adc a, (iy+20h)" +
                "sub ixh" +
                "sub ixl" +
                "sub (ix+20h)" +
                "sub iyh" +
                "sub iyl" +
                "sub (iy+20h)" +
                "sbc a, ixh" +
                "sbc a, ixl" +
                "sbc a, (ix+20h)" +
                "sbc a, iyh" +
                "sbc a, iyl" +
                "sbc a, (iy+20h)" +
                "and ixh" +
                "and ixl" +
                "and (ix+20h)" +
                "and iyh" +
                "and iyl" +
                "and (iy+20h)" +
                "xor ixh" +
                "xor ixl" +
                "xor (ix+20h)" +
                "xor iyh" +
                "xor iyl" +
                "xor (iy+20h)" +
                "or ixh" +
                "or ixl" +
                "or (ix+20h)" +
                "or iyh" +
                "or iyl" +
                "or (iy+20h)" +
                "cp ixh" +
                "cp ixl" +
                "cp (ix+20h)" +
                "cp iyh" +
                "cp iyl" +
                "cp (iy+20h)" +
                "pop ix" +
                "pop iy" +
                "jp (ix)" +
                "jp (iy)" +
                "ld sp, ix" +
                "ld sp, iy" +
                "ex (sp), ix" +
                "ex (sp), iy" +
                "push ix" +
                "push iy",
            result
        );
    }
}
