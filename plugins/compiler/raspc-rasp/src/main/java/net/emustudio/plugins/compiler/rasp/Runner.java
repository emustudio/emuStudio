/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugin.compiler.AbstractCompiler;

public class Runner {

    public static void main(String... args) {
        System.exit(AbstractCompiler.runCommandLine("raspc-rasp", args, CompilerRASP::new));
    }
}
