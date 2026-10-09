/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio.api;

import net.emustudio.emulib.plugins.Context;
import net.emustudio.emulib.plugins.annotations.PluginContext;

/** Pin-level connection to 88-4PIO. Channels are PIA1-A, PIA1-B, PIA2-A, PIA2-B, etc.
 * Implementations serialize accesses and peripheral callbacks on the context monitor.
 * Peripherals must use that same monitor for state shared with external threads.
 */
@PluginContext
public interface PioContext extends Context {
    int getChannelCount();
    void attachPeripheral(Peripheral peripheral);
    void detachPeripheral(Peripheral peripheral);
    void setInputPins(int channel, int data);
    void setControlLine1(int channel, boolean high);
    void setControlLine2(int channel, boolean high);
    int getOutputPins(int channel);
    int getOutputMask(int channel);

    interface Peripheral {
        void outputChanged(int channel, int data, int outputMask);
        void controlOutputChanged(int channel, boolean high);
        void reset();
    }
}
