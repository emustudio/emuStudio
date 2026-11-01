/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.e2e;

import net.emustudio.emulib.plugins.compiler.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SourceEditingTest extends AbstractCompilerTest {
    @Test public void macroExtractionCompilesToSameInstructions() {
        String before = "ld a,1\ninc a\n";
        String after = compiler.createLexer().extract(before, 0, before.length(), "increment", "Macro");
        compile(after); assertProgram(0x3e, 1, 0x3c);
    }
    @Test public void subroutineUsesZ80JumpSyntax() {
        String before = "ld a,1\n";
        String after = compiler.createLexer().extract(before, 0, before.length(), "load_value", "Subroutine (uses stack)");
        assertTrue(after.contains("jp load_value_after"));
        compile(after); assertProgram(0xcd, 6, 0, 0xc3, 9, 0, 0x3e, 1, 0xc9);
    }
    @Test public void sourceTokensDescribeBlocksAndDeclarations() {
        assertTrue(compiler.createLexer().analyze("work macro p\nld a,p\nendm\n").stream().anyMatch(t -> t.getType() == Token.BLOCK_START));
        assertTrue(compiler.createLexer().analyze("target: nop\njp target\n").stream().anyMatch(t -> t.getType() == Token.SYMBOL_REFERENCE && "target".equals(t.getSymbolName())));
    }
}
