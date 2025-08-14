/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer.stubs;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;

@SuppressWarnings("unused")
@PluginContext
public interface CPUContextStub extends CPUContext {
    void testMethod();
}
