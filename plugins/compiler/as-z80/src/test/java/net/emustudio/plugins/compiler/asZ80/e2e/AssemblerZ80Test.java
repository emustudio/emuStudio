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
package net.emustudio.plugins.compiler.asZ80.e2e;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class AssemblerZ80Test extends AbstractCompilerTest {

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }

    @Test
    public void testForwardAbsoluteJump() {
        compile(
            "now: ld a,b\n" +
                "cp 'C'\n" +
                "jp z, ler\n" +
                "ler: ld (HL), a"
        );

        assertProgram(
            0x78, 0xFE, 0x43, 0xCA, 0x06, 0x00, 0x77
        );
    }

    @Test
    public void testBackwardAbsoluteJump() {
        compile(
            "now: ld a,b\n" +
                "cp 'C'\n" +
                "jp z, now\n" +
                "ler: ld (HL), a"
        );

        assertProgram(
            0x78, 0xFE, 0x43, 0xCA, 0x00, 0x00, 0x77
        );
    }

    @Test
    public void testCallBackward() {
        compile(
            "dec sp\n" +
                "now: ld a,b\n" +
                "cp 'C'\n" +
                "call now\n" +
                "ler: ld (HL), a"
        );

        assertProgram(
            0x3B, 0x78, 0xFE, 0x43, 0xCD, 0x01, 0x00, 0x77
        );
    }

    @Test
    public void testCallForward() {
        compile(
            "dec sp\n" +
                "now: ld a,b\n" +
                "cp 'C'\n" +
                "call ler\n" +
                "ler: ld (HL), a"
        );

        assertProgram(
            0x3B, 0x78, 0xFE, 0x43, 0xCD, 0x07, 0x00, 0x77
        );
    }

    @Test
    public void testCP() {
        compile("cp 'C'");
        assertProgram(0xFE, 'C');
    }

    @Test(expected = Exception.class)
    public void testRSTtooBigArgument() {
        compile("rst 14");
    }


    @Test
    public void testDECwithLD() {
        compile(
            "dec sp\n"
                + "ld hl, text\n"
                + "text:\n"
                + "db 'ahoj'"
        );

        assertProgram(
            0x3B, 0x21, 0x04, 0, 'a', 'h', 'o', 'j'
        );
    }

    @Test
    public void testINthenJMP() {
        compile(
            "jp sample\n"
                + "in a, (10h)\n"
                + "sample:\n"
                + "ld a, b\n"
        );

        assertProgram(
            0xC3, 0x5, 0, 0xDB, 0x10, 0x78
        );
    }

    @Test
    public void testGetChar() {
        compile(
            "jp sample\n"
                + "getchar:\n"
                + "in a, (10h)\n"
                + "and 1\n"
                + "jp z, getchar\n"
                + "in a, (11h)\n"
                + "out (11h), a\n"
                + "ret\n"
                + "sample:\n"
                + "ld a, b"
        );

        assertProgram(
            0xC3, 0x0F, 0, 0xDB, 0x10, 0xE6, 1, 0xCA, 0x03, 0, 0xDB, 0x11, 0xD3, 0x11, 0xC9, 0x78
        );
    }

    @Test
    public void testRET_PE() {
        compile("ret pe");
        assertProgram(0xE8);
    }

    @Test
    public void testRET_PO() {
        compile("ret po");
        assertProgram(0xE0);
    }

    @Test
    public void testRET_P() {
        compile("ret p");
        assertProgram(0xF0);
    }

    @Test
    public void testAllInstructions() {
        compile(
            "nop\n"
                + "ex af, af'\n"
                + "djnz 20h\n"
                + "jr 20h\n"
                + "jr nz, 20h\n" // 6
                + "jr z, 20h\n"
                + "jr nc, 20h\n"
                + "jr c, 20h\n"
                + "ld bc, 0x1234\n"
                + "ld de, 0x1234\n"
                + "ld hl, 0x1234\n"
                + "ld sp, 0x1234\n"
                + "add hl, bc\n"
                + "add hl, de\n"
                + "add hl, hl\n"
                + "add hl, sp\n"
                + "ld (bc), a\n"
                + "ld (de), a\n"
                + "ld a, (bc)\n"
                + "ld a, (de)\n"
                + "ld (0x1234), hl\n"  //  0x22
                + "ld (0x1234), a\n"
                + "ld hl, (0x1234)\n"
                + "ld a, (0x1234)\n"
                + "inc bc\n"
                + "inc de\n"
                + "inc hl\n"
                + "inc sp\n"
                + "dec bc\n"
                + "dec de\n"
                + "dec hl\n"
                + "dec sp\n"
                + "inc b\n"
                + "inc c\n"
                + "inc d\n"
                + "inc e\n"
                + "inc h\n"
                + "inc l\n"
                + "inc a\n"
                + "dec b\n"
                + "dec c\n"
                + "dec d\n"
                + "dec e\n"
                + "dec h\n"
                + "dec l\n"
                + "dec a\n"
                + "ld b, 0x20\n"
                + "ld c, 0x20\n"
                + "ld d, 0x20\n"
                + "ld e, 0x20\n"
                + "ld h, 0x20\n"
                + "ld l, 0x20\n"
                + "rlca\n"
                + "rrca\n"
                + "rla\n"
                + "rra\n"
                + "daa\n"
                + "cpl\n"
                + "scf\n"
                + "ccf\n"
                + "ld b, b\n"
                + "ld b, c\n"
                + "ld b, d\n"
                + "ld b, e\n"
                + "ld b, h\n"
                + "ld b, l\n"
                + "ld b, a\n"
                + "ld (hl), b\n"
                + "ld (hl), c\n"
                + "ld (hl), d\n"
                + "ld (hl), e\n"
                + "ld (hl), h\n"
                + "ld (hl), l\n"
                + "ld (hl), a\n"
                + "halt\n"
                + "add a, b\n"
                + "add a, (hl)\n"
                + "adc a, c\n"
                + "adc a, (hl)\n"
                + "sub b\n"
                + "sub (hl)\n"
                + "sbc a, b\n"
                + "sbc a, (hl)\n"
                + "and b\n"
                + "and (hl)\n"
                + "xor b\n"
                + "xor (hl)\n"
                + "or b\n"
                + "or (hl)\n"
                + "cp b\n"
                + "cp (hl)\n"
                + "ret nz\n"
                + "ret z\n"
                + "ret nc\n"
                + "ret c\n"
                + "ret po\n"
                + "ret pe\n"
                + "ret p\n"
                + "ret m\n"
                + "pop bc\n"
                + "pop de\n"
                + "pop hl\n"
                + "pop af\n"
                + "ret\n"
                + "exx\n"
                + "jp hl\n"
                + "jp (hl)\n"
                + "ld sp, hl\n"
                + "jp nz, 0x1234\n"
                + "jp z, 0x1234\n"
                + "jp nc, 0x1234\n"
                + "jp c, 0x1234\n"
                + "jp po, 0x1234\n"
                + "jp pe, 0x1234\n"
                + "jp p, 0x1234\n"
                + "jp m, 0x1234\n"
                + "jp 0x1234\n"
                + "out (0x20), a\n"
                + "in a, (0x20)\n"
                + "ex (sp), hl\n"
                + "ex de, hl\n"
                + "di\n"
                + "ei\n"
                + "call nz, 0x1234\n"
                + "call z, 0x1234\n"
                + "call nc, 0x1234\n"
                + "call c, 0x1234\n"
                + "call po, 0x1234\n"
                + "call pe, 0x1234\n"
                + "call p, 0x1234\n"
                + "call m, 0x1234\n"
                + "push bc\n"
                + "push de\n"
                + "push hl\n"
                + "push af\n"
                + "call 0x1234\n"
                + "add a, 0x20\n"
                + "adc a, 0x20\n"
                + "sub 0x20\n"
                + "sbc a, 0x20\n"
                + "and 0x20\n"
                + "xor 0x20\n"
                + "or 0x20\n"
                + "cp 0x20\n"
                + "rst 0\n"
                + "rst 8\n"
                + "rst 10h\n"
                + "rst 18h\n"
                + "rst 20h\n"
                + "rst 28h\n"
                + "rst 30h\n"
                + "rst 38h\n"
                + "rlc b\n"
                + "rlc (HL)\n"
                + "rrc c\n"
                + "rrc (HL)\n"
                + "rl d\n"
                + "rl (HL)\n"
                + "rr e\n"
                + "rr (hl)\n"
                + "sla h\n"
                + "sla (hl)\n"
                + "sra l\n"
                + "sra (hl)\n"
                + "sll b\n"
                + "sll (hl)\n"
                + "srl c\n"
                + "srl (hl)\n"
                + "bit 5, b\n"
                + "bit 5, (hl)\n"
                + "res 5, c\n"
                + "res 6, (hl)\n"
                + "set 7, d\n"
                + "set 7, (hl)\n"
                + "in e, (c)\n"
                + "in (c)\n"
                + "out (c), h\n"
                + "out (c), 0\n"
                + "sbc hl, bc\n"
                + "sbc hl, de\n"
                + "sbc hl, hl\n"
                + "sbc hl, sp\n"
                + "adc hl, bc\n"
                + "adc hl, de\n"
                + "adc hl, hl\n"
                + "adc hl, sp\n"
                + "ld (1234h), bc\n"
                + "ld (1234h), de\n"
                + "ld (1234h), hl\n"
                + "ld (1234h), sp\n"
                + "ld bc, (0x1234)\n"
                + "ld de, (0x1234)\n"
                + "ld hl, (0x1234)\n"
                + "ld sp, (0x1234)\n"
                + "neg\n"
                + "retn\n"
                + "reti\n"
                + "im 0\n"
                + "im 0/1\n"
                + "im 1\n"
                + "im 2\n"
                + "ld i, a\n"
                + "ld r, a\n"
                + "ld a, i\n"
                + "ld a, r\n"
                + "rrd\n"
                + "rld\n"
                + "ldi\n"
                + "ldd\n"
                + "ldir\n"
                + "lddr\n"
                + "cpi\n"
                + "cpd\n"
                + "cpir\n"
                + "cpdr\n"
                + "ini\n"
                + "ind\n"
                + "inir\n"
                + "indr\n"
                + "outi\n"
                + "outd\n"
                + "otir\n"
                + "otdr\n"
                + "rlc (ix+0x20), b\n"
                + "rlc (iy+0x20), b\n"
                + "rlc (ix+0x20)\n"
                + "rlc (iy+0x20)\n"
                + "rrc (ix+0x20), c\n"
                + "rrc (iy+0x20), c\n"
                + "rrc (ix+0x20)\n"
                + "rrc (iy+0x20)\n"
                + "rl (ix+0x20), d\n"
                + "rl (iy+0x20), d\n"
                + "rl (ix+0x20)\n"
                + "rl (iy+0x20)\n"
                + "rr (ix+0x20), e\n"
                + "rr (iy+0x20), e\n"
                + "rr (ix+0x20)\n"
                + "rr (iy+0x20)\n"
                + "sla (ix+0x20), h\n"
                + "sla (iy+0x20), h\n"
                + "sla (ix+0x20)\n"
                + "sla (iy+0x20)\n"
                + "sra (ix+0x20), l\n"
                + "sra (iy+0x20), l\n"
                + "sra (ix+0x20)\n"
                + "sra (iy+0x20)\n"
                + "sll (ix+0x20), b\n"
                + "sll (iy+0x20), b\n"
                + "sll (ix+0x20)\n"
                + "sll (iy+0x20)\n"
                + "srl (ix+0x20), c\n"
                + "srl (iy+0x20), c\n"
                + "srl (ix+0x20)\n"
                + "srl (iy+0x20)\n"
                + "bit 3, (ix+0x20)\n"
                + "bit 3, (iy+0x20)\n"
                + "res 5, (ix+0x20), b\n"
                + "res 5, (iy+0x20), b\n"
                + "res 5, (ix+0x20)\n"
                + "res 5, (iy+0x20)\n"
                + "set 6, (ix+0x20), c\n"
                + "set 6, (iy+0x20), c\n"
                + "set 6, (ix+0x20)\n"
                + "set 6, (iy+0x20)\n"
                + "ld ix, 0x1234\n"
                + "ld iy, 0x1234\n"
                + "add ix, bc\n"
                + "add iy, bc\n"
                + "add ix, de\n"
                + "add iy, de\n"
                + "add ix, ix\n"
                + "add iy, iy\n"
                + "add ix, sp\n"
                + "add iy, sp\n"
                + "ld (0x1234), ix\n"
                + "ld (0x1234), iy\n"
                + "ld ix, (0x1234)\n"
                + "ld iy, (0x1234)\n"
                + "inc ix\n"
                + "inc iy\n"
                + "dec ix\n"
                + "dec iy\n"
                + "inc ixh\n"
                + "inc iYh\n"
                + "inc ixl\n"
                + "inc iyl\n"
                + "inc (ix+0x20)\n"
                + "inc (iy+0x20)\n"
                + "dec ixh\n"
                + "dec iyh\n"
                + "dec ixl\n"
                + "dec iyl\n"
                + "dec (ix+0x20)\n"
                + "dec (iy+0x20)\n"
                + "ld ixh, 0x20\n"
                + "ld ixl, 0x20\n"
                + "ld iyh, 0x20\n"
                + "ld iyl, 0x20\n"
                + "ld (ix+0x20), 0x20\n"
                + "ld (iy+0x20), 0x20\n"
                + "ld ixh, b\n"
                + "ld ixh, ixh\n"
                + "ld ixh, ixl\n"
                + "ld iyh, c\n"
                + "ld iyh, iyh\n"
                + "ld iyh, iyl\n"
                + "ld ixl, d\n"
                + "ld ixl, ixh\n"
                + "ld ixl, ixl\n"
                + "ld iyl, e\n"
                + "ld iyl, iyh\n"
                + "ld iyl, iyl\n"
                + "ld (ix+0x20), d\n"
                + "ld (ix+0x20), h\n"
                + "ld (ix+0x20), l\n"
                + "ld (iy+0x20), e\n"
                + "ld (iy+0x20), h\n"
                + "ld (iy+0x20), l\n"
                + "ld a, ixh\n"
                + "ld a, iyh\n"
                + "ld a, ixl\n"
                + "ld a, iyl\n"
                + "ld a, (ix+0x20)\n"
                + "ld a, (iy+0x20)\n"
                + "ld h, (ix+0x20)\n"
                + "ld l, (ix+0x20)\n"
                + "ld h, (iy+0x20)\n"
                + "ld l, (iy+0x20)\n"
                + "add a, ixh\n"
                + "add a, ixl\n"
                + "add a, (ix + 0x20)\n"
                + "add a, iyh\n"
                + "add a, iyl\n"
                + "add a, (iy+0x20)\n"
                + "adc a, ixh\n"
                + "adc a, ixl\n"
                + "adc a, (ix+0x20)\n"
                + "adc a, iyh\n"
                + "adc a, iyl\n"
                + "adc a, (iy+0x20)\n"
                + "sub ixh\n"
                + "sub ixl\n"
                + "sub (ix+0x20)\n"
                + "sub iyh\n"
                + "sub iyl\n"
                + "sub (iy+0x20)\n"
                + "sbc a, ixh\n"
                + "sbc a, ixl\n"
                + "sbc a, (ix+0x20)\n"
                + "sbc a, iyh\n"
                + "sbc a, iyl\n"
                + "sbc a, (iy+0x20)\n"
                + "and ixh\n"
                + "and ixl\n"
                + "and (ix+0x20)\n"
                + "and iyh\n"
                + "and iyl\n"
                + "and (iy+0x20)\n"
                + "xor ixh\n"
                + "xor ixl\n"
                + "xor (ix+0x20)\n"
                + "xor iyh\n"
                + "xor iyl\n"
                + "xor (iy+0x20)\n"
                + "or ixh\n"
                + "or ixl\n"
                + "or (ix+0x20)\n"
                + "or iyh\n"
                + "or iyl\n"
                + "or (iy+0x20)\n"
                + "cp ixh\n"
                + "cp ixl\n"
                + "cp (ix+0x20)\n"
                + "cp iyh\n"
                + "cp iyl\n"
                + "cp (iy+0x20)\n"
                + "pop ix\n"
                + "pop iy\n"
                + "jp (ix)\n"
                + "jp (iy)\n"
                + "ld sp, ix\n"
                + "ld sp, iy\n"
                + "ex (sp), ix\n"
                + "ex (sp), iy\n"
                + "push ix\n"
                + "push iy\n"
        );

        assertProgram(
            0x00,
            0x08,
            0x10, 0x20,
            0x18, 0x20,
            0x20, 0x20,
            0x28, 0x20,
            0x30, 0x20,
            0x38, 0x20,
            0x01, 0x34, 0x12,  // 10
            0x11, 0x34, 0x12,
            0x21, 0x34, 0x12,
            0x31, 0x34, 0x12,
            0x09,
            0x19,
            0x29,
            0x39,
            0x02,
            0x12,
            0x0A, // 20
            0x1A,
            0x22, 0x34, 0x12,
            0x32, 0x34, 0x12,
            0x2A, 0x34, 0x12,
            0x3A, 0x34, 0x12,
            0x03,
            0x13,
            0x23, // 30
            0x33,
            0x0B,
            0x1B,
            0x2B,
            0x3B,
            0x04,
            0x0C,
            0x14,
            0x1C,
            0x24,
            0x2C,
            0x3C,
            0x05,
            0x0D,
            0x15,
            0x1D, // 40
            0x25,
            0x2D,
            0x3D,
            0x06, 0x20,
            0x0E, 0x20,
            0x16, 0x20,
            0x1E, 0x20,
            0x26, 0x20,
            0x2E, 0x20,
            0x07, // 50
            0x0F,
            0x17,
            0x1F,
            0x27,
            0x2F,
            0x37,
            0x3F,
            0x40,
            0x41,
            0x42,
            0x43,
            0x44,
            0x45,
            0x47,
            0x70,
            0x71, // 60
            0x72,
            0x73,
            0x74,
            0x75,
            0x77,
            0x76,
            0x80,
            0x86,
            0x89,
            0x8E,
            0x90,
            0x96,
            0x98,
            0x9E,
            0xA0,
            0xA6, // 70
            0xA8,
            0xAE,
            0xB0,
            0xB6,
            0xB8,
            0xBE,
            0xC0,
            0xC8,
            0xD0,
            0xD8,
            0xE0, // 7b
            0xE8,
            0xF0,
            0xF8,
            0xC1,
            0xD1, // 80
            0xE1,
            0xF1,
            0xC9,
            0xD9,
            0xE9,
            0xE9,
            0xF9,
            0xC2, 0x34, 0x12,
            0xCA, 0x34, 0x12,
            0xD2, 0x34, 0x12,
            0xDA, 0x34, 0x12,
            0xE2, 0x34, 0x12,
            0xEA, 0x34, 0x12,
            0xF2, 0x34, 0x12,
            0xFA, 0x34, 0x12,
            0xC3, 0x34, 0x12,
            0xD3, 0x20,
            0xDB, 0x20,
            0xE3,
            0xEB,
            0xF3,
            0xFB,
            0xC4, 0x34, 0x12,
            0xCC, 0x34, 0x12,
            0xD4, 0x34, 0x12,
            0xDC, 0x34, 0x12,
            0xE4, 0x34, 0x12,
            0xEC, 0x34, 0x12,
            0xF4, 0x34, 0x12,
            0xFC, 0x34, 0x12,
            0xC5,
            0xD5,
            0xE5,
            0xF5,
            0xCD, 0x34, 0x12,
            0xC6, 0x20,
            0xCE, 0x20,
            0xD6, 0x20,
            0xDE, 0x20,
            0xE6, 0x20,
            0xEE, 0x20,
            0xF6, 0x20,
            0xFE, 0x20,
            0xC7,
            0xCF,
            0xD7,
            0xDF,
            0xE7,
            0xEF,
            0xF7,
            0xFF,
            0xCB, 0x00,
            0xCB, 0x06,
            0xCB, 0x09,
            0xCB, 0x0E,
            0xCB, 0x12,
            0xCB, 0x16,
            0xCB, 0x1B,
            0xCB, 0x1E,
            0xCB, 0x24,
            0xCB, 0x26,
            0xCB, 0x2D,
            0xCB, 0x2E,
            0xCB, 0x30,
            0xCB, 0x36,
            0xCB, 0x39,
            0xCB, 0x3E,
            0xCB, 0x68,
            0xCB, 0x6E,
            0xCB, 0xA9,
            0xCB, 0xB6,
            0xCB, 0xFA,
            0xCB, 0xFE,
            0xED, 0x58,
            0xED, 0x70,
            0xED, 0x61,
            0xED, 0x71,
            0xED, 0x42,
            0xED, 0x52,
            0xED, 0x62,
            0xED, 0x72,
            0xED, 0x4A,
            0xED, 0x5A,
            0xED, 0x6A,
            0xED, 0x7A,
            0xED, 0x43, 0x34, 0x12,
            0xED, 0x53, 0x34, 0x12,
            0x22, 0x34, 0x12,  // 0xED, 0x63, 0x34, 0x12
            0xED, 0x73, 0x34, 0x12,
            0xED, 0x4B, 0x34, 0x12,
            0xED, 0x5B, 0x34, 0x12,
            0x2A, 0x34, 0x12,  // 0xED, 0x6B, 0x34, 0x12
            0xED, 0x7B, 0x34, 0x12,
            0xED, 0x44,
            0xED, 0x45,
            0xED, 0x4D,
            0xED, 0x46,
            0xED, 0x4E,
            0xED, 0x56,
            0xED, 0x5E,
            0xED, 0x47,
            0xED, 0x4F,
            0xED, 0x57,
            0xED, 0x5F,
            0xED, 0x67,
            0xED, 0x6F,
            0xED, 0xA0,
            0xED, 0xA8,
            0xED, 0xB0,
            0xED, 0xB8,
            0xED, 0xA1,
            0xED, 0xA9,
            0xED, 0xB1,
            0xED, 0xB9,
            0xED, 0xA2,
            0xED, 0xAA,
            0xED, 0xB2,
            0xED, 0xBA,
            0xED, 0xA3,
            0xED, 0xAB,
            0xED, 0xB3,
            0xED, 0xBB,
            0xDD, 0xCB, 0x20, 0x00,
            0xFD, 0xCB, 0x20, 0x00,
            0xDD, 0xCB, 0x20, 0x06,
            0xFD, 0xCB, 0x20, 0x06,
            0xDD, 0xCB, 0x20, 0x09,
            0xFD, 0xCB, 0x20, 0x09,
            0xDD, 0xCB, 0x20, 0x0E,
            0xFD, 0xCB, 0x20, 0x0E,
            0xDD, 0xCB, 0x20, 0x12,
            0xFD, 0xCB, 0x20, 0x12,
            0xDD, 0xCB, 0x20, 0x16,
            0xFD, 0xCB, 0x20, 0x16,
            0xDD, 0xCB, 0x20, 0x1B,
            0xFD, 0xCB, 0x20, 0x1B,
            0xDD, 0xCB, 0x20, 0x1E,
            0xFD, 0xCB, 0x20, 0x1E,
            0xDD, 0xCB, 0x20, 0x24,
            0xFD, 0xCB, 0x20, 0x24,
            0xDD, 0xCB, 0x20, 0x26,
            0xFD, 0xCB, 0x20, 0x26,
            0xDD, 0xCB, 0x20, 0x2D,
            0xFD, 0xCB, 0x20, 0x2D,
            0xDD, 0xCB, 0x20, 0x2E,
            0xFD, 0xCB, 0x20, 0x2E,
            0xDD, 0xCB, 0x20, 0x30,
            0xFD, 0xCB, 0x20, 0x30,
            0xDD, 0xCB, 0x20, 0x36,
            0xFD, 0xCB, 0x20, 0x36,
            0xDD, 0xCB, 0x20, 0x39,
            0xFD, 0xCB, 0x20, 0x39,
            0xDD, 0xCB, 0x20, 0x3E,
            0xFD, 0xCB, 0x20, 0x3E,
            0xDD, 0xCB, 0x20, 0x5E,
            0xFD, 0xCB, 0x20, 0x5E,
            0xDD, 0xCB, 0x20, 0xA8,
            0xFD, 0xCB, 0x20, 0xA8,
            0xDD, 0xCB, 0x20, 0xAE,
            0xFD, 0xCB, 0x20, 0xAE,
            0xDD, 0xCB, 0x20, 0xF1,
            0xFD, 0xCB, 0x20, 0xF1,
            0xDD, 0xCB, 0x20, 0xF6,
            0xFD, 0xCB, 0x20, 0xF6,
            0xDD, 0x21, 0x34, 0x12,
            0xFD, 0x21, 0x34, 0x12,
            0xDD, 0x09,
            0xFD, 0x09,
            0xDD, 0x19,
            0xFD, 0x19,
            0xDD, 0x29,
            0xFD, 0x29,
            0xDD, 0x39,
            0xFD, 0x39,
            0xDD, 0x22, 0x34, 0x12,
            0xFD, 0x22, 0x34, 0x12,
            0xDD, 0x2A, 0x34, 0x12,
            0xFD, 0x2A, 0x34, 0x12,
            0xDD, 0x23,
            0xFD, 0x23,
            0xDD, 0x2B,
            0xFD, 0x2B,
            0xDD, 0x24,
            0xFD, 0x24,
            0xDD, 0x2C,
            0xFD, 0x2C,
            0xDD, 0x34, 0x20,
            0xFD, 0x34, 0x20,
            0xDD, 0x25,
            0xFD, 0x25,
            0xDD, 0x2D,
            0xFD, 0x2D,
            0xDD, 0x35, 0x20,
            0xFD, 0x35, 0x20,
            0xDD, 0x26, 0x20,
            0xDD, 0x2E, 0x20,
            0xFD, 0x26, 0x20,
            0xFD, 0x2E, 0x20,
            0xDD, 0x36, 0x20, 0x20,
            0xFD, 0x36, 0x20, 0x20,
            0xDD, 0x60,
            0xDD, 0x64,
            0xDD, 0x65,
            0xFD, 0x61,
            0xFD, 0x64,
            0xFD, 0x65,
            0xDD, 0x6A,
            0xDD, 0x6C,
            0xDD, 0x6D,
            0xFD, 0x6B,
            0xFD, 0x6C,
            0xFD, 0x6D,
            0xDD, 0x72, 0x20,
            0xDD, 0x74, 0x20,
            0xDD, 0x75, 0x20,
            0xFD, 0x73, 0x20,
            0xFD, 0x74, 0x20,
            0xFD, 0x75, 0x20,
            0xDD, 0x7C,
            0xFD, 0x7C,
            0xDD, 0x7D,
            0xFD, 0x7D,
            0xDD, 0x7E, 0x20,
            0xFD, 0x7E, 0x20,
            0xDD, 0x66, 0x20,
            0xDD, 0x6E, 0x20,
            0xFD, 0x66, 0x20,
            0xFD, 0x6E, 0x20,
            0xDD, 0x84,
            0xDD, 0x85,
            0xDD, 0x86, 0x20,
            0xFD, 0x84,
            0xFD, 0x85,
            0xFD, 0x86, 0x20,
            0xDD, 0x8C,
            0xDD, 0x8D,
            0xDD, 0x8E, 0x20,
            0xFD, 0x8C,
            0xFD, 0x8D,
            0xFD, 0x8E, 0x20,
            0xDD, 0x94,
            0xDD, 0x95,
            0xDD, 0x96, 0x20,
            0xFD, 0x94,
            0xFD, 0x95,
            0xFD, 0x96, 0x20,
            0xDD, 0x9C,
            0xDD, 0x9D,
            0xDD, 0x9E, 0x20,
            0xFD, 0x9C,
            0xFD, 0x9D,
            0xFD, 0x9E, 0x20,
            0xDD, 0xA4,
            0xDD, 0xA5,
            0xDD, 0xA6, 0x20,
            0xFD, 0xA4,
            0xFD, 0xA5,
            0xFD, 0xA6, 0x20,
            0xDD, 0xAC,
            0xDD, 0xAD,
            0xDD, 0xAE, 0x20,
            0xFD, 0xAC,
            0xFD, 0xAD,
            0xFD, 0xAE, 0x20,
            0xDD, 0xB4,
            0xDD, 0xB5,
            0xDD, 0xB6, 0x20,
            0xFD, 0xB4,
            0xFD, 0xB5,
            0xFD, 0xB6, 0x20,
            0xDD, 0xBC,
            0xDD, 0xBD,
            0xDD, 0xBE, 0x20,
            0xFD, 0xBC,
            0xFD, 0xBD,
            0xFD, 0xBE, 0x20,
            0xDD, 0xE1,
            0xFD, 0xE1,
            0xDD, 0xE9,
            0xFD, 0xE9,
            0xDD, 0xF9,
            0xFD, 0xF9,
            0xDD, 0xE3,
            0xFD, 0xE3,
            0xDD, 0xE5,
            0xFD, 0xE5
        );
    }
}
