/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LexicalAnalyzerImplTest {

    @Test
    public void testAllTokensArePresent() {
        for (int i = 1; i < LexicalAnalyzerImpl.tokenMap.length; i++) {
            int token = LexicalAnalyzerImpl.tokenMap[i];
            assertTrue("Token " + i + " is missing", token != 0);
        }
    }
}
