/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.exceptions;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.CompileError;

public class FatalError extends CompileException {

    public FatalError(SourceCodePosition position, String why) {
        super(position, "Fatal error (cannot continue): " + why);
    }

    public static void now(SourceCodePosition position, String why) {
        throw new FatalError(position, why);
    }

    public static void now(CompileError error) {
        now(error.position, error.msg);
    }
}
