/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80;

import net.emustudio.emulib.plugin.compiler.AbstractCompiler;

public class Runner {

    public static void main(String... args) {
        System.exit(AbstractCompiler.runCommandLine("as-z80", args, AssemblerZ80::new));
    }
}
