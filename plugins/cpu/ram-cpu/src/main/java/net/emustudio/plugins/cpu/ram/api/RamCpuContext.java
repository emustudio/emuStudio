/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.api;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;

public interface RamCpuContext extends CPUContext {

    AbstractTapeContext getStorageTape();

    AbstractTapeContext getInputTape();

    AbstractTapeContext getOutputTape();
}
