/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.e2e;

import org.junit.Test;

public class InstrRegTest extends AbstractCompilerTest {

    @Test
    public void testINR() throws Exception {
        compile(
                "inc A\n"
                        + "inc B\n"
                        + "inc C\n"
                        + "inc D\n"
                        + "inc E\n"
                        + "inc H\n"
                        + "inc L\n"
                        + "inc (HL)\n"
        );

        assertProgram(
                0x3C, 0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C, 0x34
        );
    }

    @Test
    public void testDCR() throws Exception {
        compile(
                "dec A\n"
                        + "dec B\n"
                        + "dec C\n"
                        + "dec D\n"
                        + "dec E\n"
                        + "dec H\n"
                        + "dec L\n"
                        + "dec (HL)\n"
        );

        assertProgram(
                0x3D, 0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D, 0x35
        );
    }

    @Test
    public void testADD() throws Exception {
        compile(
                "add A, A\n"
                        + "add A,B\n"
                        + "add A,C\n"
                        + "add A,D\n"
                        + "add A,E\n"
                        + "add A,H\n"
                        + "add A,L\n"
                        + "add A,(HL)\n"
        );

        assertProgram(
                0x87, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86
        );
    }

    @Test
    public void testADC() throws Exception {
        compile(
                "adc A, A\n"
                        + "adc A,B\n"
                        + "adc A,C\n"
                        + "adc A,D\n"
                        + "adc A,E\n"
                        + "adc A,H\n"
                        + "adc A,L\n"
                        + "adc A,(HL)\n"
        );

        assertProgram(
                0x8F, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E
        );
    }

    @Test
    public void testSUB() throws Exception {
        compile(
                "sub A\n"
                        + "sub B\n"
                        + "sub C\n"
                        + "sub D\n"
                        + "sub E\n"
                        + "sub H\n"
                        + "sub L\n"
                        + "sub (HL)\n"
        );

        assertProgram(
                0x97, 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96
        );
    }

    @Test
    public void testSBC() throws Exception {
        compile(
                "sbc A,A\n"
                        + "sbc A,B\n"
                        + "sbc A,C\n"
                        + "sbc A,D\n"
                        + "sbc A,E\n"
                        + "sbc A,H\n"
                        + "sbc A,L\n"
                        + "sbc A,(HL)\n"
        );

        assertProgram(
                0x9F, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E
        );
    }

    @Test
    public void testAND() throws Exception {
        compile(
                "and A\n"
                        + "and B\n"
                        + "and C\n"
                        + "and D\n"
                        + "and E\n"
                        + "and H\n"
                        + "and L\n"
                        + "and (HL)\n"
        );

        assertProgram(
                0xA7, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6
        );
    }

    @Test
    public void testXOR() throws Exception {
        compile(
                "xor A\n"
                        + "xor B\n"
                        + "xor C\n"
                        + "xor D\n"
                        + "xor E\n"
                        + "xor H\n"
                        + "xor L\n"
                        + "xor (HL)\n"
        );

        assertProgram(
                0xAF, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE
        );
    }

    @Test
    public void testOR() throws Exception {
        compile(
                "or A\n"
                        + "or B\n"
                        + "or C\n"
                        + "or D\n"
                        + "or E\n"
                        + "or H\n"
                        + "or L\n"
                        + "or (HL)\n"
        );

        assertProgram(
                0xB7, 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6
        );
    }

    @Test
    public void testCP() throws Exception {
        compile(
                "cp A\n"
                        + "cp B\n"
                        + "cp C\n"
                        + "cp D\n"
                        + "cp E\n"
                        + "cp H\n"
                        + "cp L\n"
                        + "cp (HL)\n"
        );

        assertProgram(
                0xBF, 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE
        );
    }

    @Test
    public void testDisplacementWithMinus() {
        // ld a, (ix - 5) should be equivalent to ld a, (ix + (-5))
        // DD 7E FB  (0xFB = -5 as signed byte)
        compile("ld a, (ix - 5)");
        assertProgram(0xDD, 0x7E, 0xFB);
    }

    @Test
    public void testDisplacementWithMinusIY() {
        // ld a, (iy - 3) -> FD 7E FD (0xFD = -3 as signed byte)
        compile("ld a, (iy - 3)");
        assertProgram(0xFD, 0x7E, 0xFD);
    }

    @Test
    public void testDisplacementWithMinusInc() {
        // inc (ix - 1) -> DD 34 FF
        compile("inc (ix - 1)");
        assertProgram(0xDD, 0x34, 0xFF);
    }

    @Test
    public void testDisplacementWithMinusRlc() {
        // rlc (ix - 2) -> DD CB FE 06
        compile("rlc (ix - 2)");
        assertProgram(0xDD, 0xCB, 0xFE, 0x06);
    }

    @Test
    public void testDisplacementWithMinusLdStore() {
        // ld (iy - 10), b -> FD 70 F6
        compile("ld (iy - 10), b");
        assertProgram(0xFD, 0x70, 0xF6);
    }

    @Test
    public void testDisplacementWithMinusLdImmediate() {
        // ld (ix - 4), 0x42 -> DD 36 FC 42
        compile("ld (ix - 4), 42h");
        assertProgram(0xDD, 0x36, 0xFC, 0x42);
    }

    @Test
    public void testDisplacementWithPlusStillWorks() {
        // ld a, (ix + 5) should still work
        compile("ld a, (ix + 5)");
        assertProgram(0xDD, 0x7E, 0x05);
    }
}
