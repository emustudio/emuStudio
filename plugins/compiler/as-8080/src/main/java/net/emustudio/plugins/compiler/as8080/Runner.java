/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080;

import net.emustudio.emulib.plugins.compiler.AbstractCompiler;

public class Runner {

    public static void main(String... args) {
        System.exit(AbstractCompiler.runCommandLine("as-8080", args, Assembler8080::new));
    }
}
