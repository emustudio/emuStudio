/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp.api;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;

public interface RaspCpuContext extends CPUContext {

    AbstractTapeContext getInputTape();

    AbstractTapeContext getOutputTape();
}
