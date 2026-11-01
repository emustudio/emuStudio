/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.e2e;

import net.emustudio.emulib.plugins.compiler.*;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class SourceEditingTest extends AbstractCompilerTest {
    @Test public void semanticTokensDistinguishMacroScopeAndIncludes() {
        String source = "value equ 1\nwork macro value\nlocal: mvi a,value\nendm\ninclude 'other.asm'\nwork value\n";
        List<Token> tokens = compiler.createLexer().analyze(source);
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == Token.SYMBOL_DEFINITION && "value".equals(t.getSymbolName()) && "".equals(t.getSymbolScope())));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == Token.SYMBOL_DEFINITION && "value".equals(t.getSymbolName()) && "macro/work".equals(t.getSymbolScope())));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == Token.SYMBOL_REFERENCE && "value".equals(t.getSymbolName()) && "macro/work".equals(t.getReferenceScope())));
        assertTrue(tokens.stream().anyMatch(t -> "other.asm".equals(t.getReferencedFile())));
    }
    @Test public void macroExtractionCompilesToSameInstructions() {
        String before = "mvi a,1\ninr a\n";
        String after = compiler.createLexer().extract(before, 0, before.length(), "increment", "Macro");
        compile(after); assertProgram(0x3e, 1, 0x3c);
    }
    @Test public void subroutineExtractionSkipsRoutineOnReturnAndCompiles() {
        String before = "mvi a,1\n";
        String after = compiler.createLexer().extract(before, 0, before.length(), "load_value", "Subroutine (uses stack)");
        compile(after); assertProgram(0xcd, 6, 0, 0xc3, 9, 0, 0x3e, 1, 0xc9);
    }
    @Test public void extractionRejectsDeclarationsAndStackInstructions() {
        LexicalAnalyzer support = compiler.createLexer();
        assertThrows(IllegalArgumentException.class, () -> support.extract("local: nop\n", 0, 11, "work", "Macro"));
        String stack = "push b\n";
        assertThrows(IllegalArgumentException.class, () -> support.extract(stack, 0, stack.length(), "work", "Subroutine (uses stack)"));
    }
    @Test public void semanticAnalysisRejectsSyntaxErrors() {
        assertThrows(IllegalArgumentException.class, () -> compiler.createLexer().analyze("mvi a,\n"));
    }
    @Test public void namesRejectReservedWordsAndAcceptValidIdentifiers() {
        assertFalse(compiler.createLexer().isValidSymbolName("mvi"));
        assertTrue(compiler.createLexer().isValidSymbolName("next_value"));
    }
    @Test public void metadataOffsetsMatchJavaStringsAfterSupplementaryCharacters() {
        String source = "; 😀 comment\nvalue: nop\njmp value\n";
        List<Token> tokens = compiler.createLexer().analyze(source);
        Token definition = tokens.stream().filter(t -> t.getType() == Token.SYMBOL_DEFINITION).findFirst().orElseThrow();
        assertEquals(source.indexOf("value:"), definition.getOffset());
        Token reference = tokens.stream().filter(t -> t.getType() == Token.SYMBOL_REFERENCE).findFirst().orElseThrow();
        assertEquals(source.lastIndexOf("value"), reference.getOffset());
    }
}
