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
package net.emustudio.plugins.compiler.as8080.e2e;

import org.junit.Test;

public class InstrRegTest extends AbstractCompilerTest {

    @Test
    public void testINR() throws Exception {
        compile(
            "inr A\n"
                + "inr B\n"
                + "inr C\n"
                + "inr D\n"
                + "inr E\n"
                + "inr H\n"
                + "inr L\n"
                + "inr M\n"
        );

        assertProgram(
            0x3C, 0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C, 0x34
        );
    }

    @Test
    public void testDCR() throws Exception {
        compile(
            "dcr A\n"
                + "dcr B\n"
                + "dcr C\n"
                + "dcr D\n"
                + "dcr E\n"
                + "dcr H\n"
                + "dcr L\n"
                + "dcr M\n"
        );

        assertProgram(
            0x3D, 0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D, 0x35
        );
    }

    @Test
    public void testADD() throws Exception {
        compile(
            "add A\n"
                + "add B\n"
                + "add C\n"
                + "add D\n"
                + "add E\n"
                + "add H\n"
                + "add L\n"
                + "add M\n"
        );

        assertProgram(
            0x87, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86
        );
    }

    @Test
    public void testADC() throws Exception {
        compile(
            "adc A\n"
                + "adc B\n"
                + "adc C\n"
                + "adc D\n"
                + "adc E\n"
                + "adc H\n"
                + "adc L\n"
                + "adc M\n"
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
                + "sub M\n"
        );

        assertProgram(
            0x97, 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96
        );
    }

    @Test
    public void testSBB() throws Exception {
        compile(
            "sbb A\n"
                + "sbb B\n"
                + "sbb C\n"
                + "sbb D\n"
                + "sbb E\n"
                + "sbb H\n"
                + "sbb L\n"
                + "sbb M\n"
        );

        assertProgram(
            0x9F, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E
        );
    }

    @Test
    public void testANA() throws Exception {
        compile(
            "ana A\n"
                + "ana B\n"
                + "ana C\n"
                + "ana D\n"
                + "ana E\n"
                + "ana H\n"
                + "ana L\n"
                + "ana M\n"
        );

        assertProgram(
            0xA7, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6
        );
    }

    @Test
    public void testXRA() throws Exception {
        compile(
            "xra A\n"
                + "xra B\n"
                + "xra C\n"
                + "xra D\n"
                + "xra E\n"
                + "xra H\n"
                + "xra L\n"
                + "xra M\n"
        );

        assertProgram(
            0xAF, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE
        );
    }

    @Test
    public void testORA() throws Exception {
        compile(
            "ora A\n"
                + "ora B\n"
                + "ora C\n"
                + "ora D\n"
                + "ora E\n"
                + "ora H\n"
                + "ora L\n"
                + "ora M\n"
        );

        assertProgram(
            0xB7, 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6
        );
    }

    @Test
    public void testCMP() throws Exception {
        compile(
            "cmp A\n"
                + "cmp B\n"
                + "cmp C\n"
                + "cmp D\n"
                + "cmp E\n"
                + "cmp H\n"
                + "cmp L\n"
                + "cmp M\n"
        );

        assertProgram(
            0xBF, 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE
        );
    }
}
