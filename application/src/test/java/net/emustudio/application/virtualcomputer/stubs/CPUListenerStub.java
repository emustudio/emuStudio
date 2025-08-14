/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer.stubs;

import net.emustudio.emulib.plugins.cpu.CPU;

public class CPUListenerStub implements CPU.CPUListener {

    @Override
    public void runStateChanged(CPU.RunState runState) {
    }

    @Override
    public void internalStateChanged() {
    }
}
