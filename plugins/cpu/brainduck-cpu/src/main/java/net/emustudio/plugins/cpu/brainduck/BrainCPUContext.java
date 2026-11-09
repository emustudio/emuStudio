/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.device.DeviceContext;

public interface BrainCPUContext extends CPUContext {
    void attachDevice(DeviceContext<Byte> device);

    void detachDevice();
}
