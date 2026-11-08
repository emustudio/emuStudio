/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.e2e;

import net.emustudio.emulib.runtime.annotations.MemoryAnnotations.Entry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class Assembler8080Test extends AbstractCompilerTest {

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }

    @Test
    public void testCompilationAddsSourceCodeAnnotations() {
        compile("nop\nmov a,b");

        assertEquals(1, annotations.at(0).get(0).source.line);
        assertEquals(2, annotations.at(1).get(0).source.line);
    }

    @Test
    public void compilationPublishesResolvedLabelsAlongsideMappings() {
        compile("constant equ 1\nstart:\nnop\nnext: mov a,b");

        assertEquals("start", annotations.at(0).stream().filter(entry -> entry.kind.equals("label"))
                .findFirst().orElseThrow().text);
        assertEquals("next", annotations.at(1).stream().filter(entry -> entry.kind.equals("label"))
                .findFirst().orElseThrow().text);
        assertEquals(2, annotations.all().stream().filter(entry -> entry.kind.equals("label")).count());
        assertEquals(2, annotations.all().stream().filter(entry -> entry.kind.equals("source")).count());
    }

    @Test
    public void compilesWithoutAnnotationService() {
        annotations = null;

        compile("first: nop\nmov a,b");

        assertProgram(0x00, 0x78);
    }

    @Test
    public void recompilationReplacesCompilerMappingsAndKeepsUserAnnotations() {
        annotations.put(new Entry(-1, 1, 1, 2, "label", "user", null));
        compile("nop\nmov a,b");

        compile("nop");

        assertEquals(1, annotations.at(0).size());
        assertEquals("user", annotations.at(1).get(0).text);
        assertEquals(1, annotations.at(1).size());
    }

    @Test
    public void failedCompilationKeepsPreviousMappings() {
        compile("first: nop\nmov a,b");

        assertThrows(RuntimeException.class, () -> compile("rst 10"));

        assertEquals(2, annotations.at(1).get(0).source.line);
        assertEquals("first", annotations.at(0).stream().filter(entry -> entry.kind.equals("label"))
                .findFirst().orElseThrow().text);
    }

    @Test
    public void testForwardAbsoluteJump() {
        compile(
                "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "jz ler\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x06, 0x00, 0x77
        );
    }

    @Test
    public void testBackwardAbsoluteJump() {
        compile(
                "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "jz now\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x78, 0xFE, 0x43, 0xCA, 0x00, 0x00, 0x77
        );
    }

    @Test
    public void testCallBackward() {
        compile(
                "dcx sp\n" +
                        "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "call now\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x3B, 0x78, 0xFE, 0x43, 0xCD, 0x01, 0x00, 0x77
        );
    }

    @Test
    public void testCallForward() {
        compile(
                "dcx sp\n" +
                        "now: mov a,b\n" +
                        "cpi 'C'\n" +
                        "call ler\n" +
                        "ler: mov m, a"
        );

        assertProgram(
                0x3B, 0x78, 0xFE, 0x43, 0xCD, 0x07, 0x00, 0x77
        );
    }

    @Test(expected = Exception.class)
    public void testRSTtooBigArgument() {
        compile("rst 10");
    }

    @Test
    public void testDCXwithLXI() {
        compile(
                "dcx sp\n"
                        + "lxi h, text\n"
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
                "jmp sample\n"
                        + "in 10h\n"
                        + "sample:\n"
                        + "mov a, b\n"
        );

        assertProgram(
                0xC3, 0x5, 0, 0xDB, 0x10, 0x78
        );
    }

    @Test
    public void testGetChar() {
        compile(
                "jmp sample\n"
                        + "getchar:\n"
                        + "in 10h\n"
                        + "ani 1\n"
                        + "jz getchar\n"
                        + "in 11h\n"
                        + "out 11h\n"
                        + "ret\n"
                        + "sample:\n"
                        + "mov a, b"
        );

        assertProgram(
                0xC3, 0x0F, 0, 0xDB, 0x10, 0xE6, 1, 0xCA, 0x03, 0, 0xDB, 0x11, 0xD3, 0x11, 0xC9, 0x78
        );
    }
}
