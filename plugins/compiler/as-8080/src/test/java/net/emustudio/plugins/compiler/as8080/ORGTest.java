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
package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.tree.OrgPseudoNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ORGTest extends AbstractCompilerTest {

    @Test
    public void testORGwithInclude() throws Exception {
        File includeFile = new File(getClass().getResource("/sample.asm").toURI());
        compile(
            "org 3\n"
                + "call sample\n"
                + "include '" + includeFile.getAbsolutePath() + "'\n"
        );

        assertProgram(
            0, 0, 0, 0xCD, 6, 0, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testORGwithDoubleInclude() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
            "org 3\n"
                + "call sample\n"
                + "include '" + second.getAbsolutePath() + "'\n"
                + "include '" + first.getAbsolutePath() + "'\n"
        );

        assertProgram(
            0, 0, 0, 0xCD, 0x09, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9
        );
    }

    @Test
    public void testORGwithDoubleIncludeAndJMPafter() throws Exception {
        File first = new File(getClass().getResource("/sample.asm").toURI());
        File second = new File(getClass().getResource("/sample2.asm").toURI());
        compile(
            "org 3\n"
                + "jmp next\n"
                + "include '" + first.getAbsolutePath() + "'\n"
                + "include '" + second.getAbsolutePath() + "'\n"
                + "next:\n"
                + "mov a, b\n"
        );

        assertProgram(
            0, 0, 0, 0xC3, 0x0C, 0, 0x3E, 0, 0xC9, 0x3E, 0, 0xC9, 0x78
        );
    }

    @Test
    public void testORGwithDB() throws Exception {
        compile(
            "org 3\n"
                + "lxi h, text\n"
                + "text:\n"
                + "db 'ahoj'"
        );

        assertProgram(
            0, 0, 0, 0x21, 0x06, 0, 'a', 'h', 'o', 'j'
        );
    }

    @Test
    public void testORG() throws Exception {
        compile(
            "org 2\n" +
                "now: mov a,b\n" +
                "ds 2\n" +
                "cpi 'C'\n" +
                "jz now\n" +
                "ler: mov m, a"
        );

        assertProgram(
            0, 0, 0x78, 0, 0, 0xFE, 0x43, 0xCA, 0x02, 0x00, 0x77
        );
    }

    @Test
    public void testORGwithJumpBackwards() throws Exception {
        compile(
            "sample:\n"
                + "org 2\n"
                + "jmp sample"
        );

        assertProgram(
            0, 0, 0xC3, 0, 0
        );
    }

    @Test
    public void testORGwithJumpForwards() throws Exception {
        compile(
            "jmp sample\n"
                + "org 5\n"
                + "sample:\n"
                + "mov a, b"
        );

        assertProgram(
            0xC3, 0x05, 0, 0, 0, 0x78
        );
    }

    @Test
    public void testORGdoesNotBreakPreviousMemoryContent() throws Exception {
        memoryStub.write(0, (short) 0x10);
        memoryStub.write(1, (short) 0x11);

        compile(
            "org 2\n" + "now: mov a,b\n"
        );

        assertProgram(
            0x10, 0x11, 0x78
        );
    }

    @Test
    public void testORGthenDSdoNotOverlap() throws Exception {
        compile(
            "org 2\nds 2\nmov a,b"
        );
        assertProgram(
            0, 0, 0, 0, 0x78
        );
    }

    @Test(expected = Exception.class)
    public void testORGisAmbiguous() throws Exception {
        compile(
            "org text\nmvi a, 4\ntext: db 4\n"
        );
    }

    @Test
    public void testORGhasSizeZero() {
        OrgPseudoNode node = new OrgPseudoNode(new ExprNode() {
            @Override
            public int eval(Namespace env, int curr_addr) {
                return 0;
            }
        }, 0, 0);

        assertEquals(0, node.getSize());
        assertEquals("", node.getName());
    }
}
