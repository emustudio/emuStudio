/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.cpu.AbstractCPUContext;
import net.emustudio.emulib.plugins.cpu.CPUSnapshot;

final class CpuContextImpl extends AbstractCPUContext {
    @Override public boolean isPassedCyclesSupported() { return false; }
    void publishState(CPUSnapshot snapshot) { publishSnapshot(snapshot); }
}
