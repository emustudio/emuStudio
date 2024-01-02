/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2024  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus.LINE_CYCLES;
import static net.emustudio.plugins.device.zxspectrum.ula.ZxParameters.*;

/**
 * Triggers actions based on passed CPU cycles.
 * <p>
 * For <a href="https://worldofspectrum.org/faq/reference/48kreference.htm">ZX Spectrum 48K</a> the actions are:
 * <p>
 * - 0: CPU interrupt
 * - 0 - 14336: first 64 lines. From those, at least 48 are border-lines, others are either border or vertical retraces
 * After an interrupt occurs, 64 line times (14336 T states; see below for exact timings) pass before
 * - 14337 - 57344: 192 screen lines are displayed
 * - 57345 - 69888: 56 border-lines are displayed
 * <p>
 * This means a frame is (64+192+56)*224=69888 T states long, which means that the CPU interrupt occurs
 * at 3.5MHz/69888=50.08 Hz.
 */
public class PassedCyclesMediator implements CPUContext.PassedCyclesListener {
    private static final long FRAME_CYCLES = (PRE_SCREEN_LINES + SCREEN_HEIGHT + POST_SCREEN_LINES) * LINE_CYCLES;  // 69888;

    private long frameCycles = 0;
    private long lineCycles = 0;
    private int lastLinePainted = 0;

    private final AtomicReference<DisplayCanvas> canvas = new AtomicReference<>();
    private final ULA ula;

    public PassedCyclesMediator(ULA ula) {
        this.ula = Objects.requireNonNull(ula);
    }

    public void setCanvas(DisplayCanvas canvas) {
        this.canvas.set(canvas);
    }

    @Override
    public void passedCycles(long cycles) {
        frameCycles += cycles;
        lineCycles += cycles;

        DisplayCanvas canvas = this.canvas.get();
        if (canvas != null) {
            if (lineCycles >= LINE_CYCLES) {
                canvas.drawNextLine(lastLinePainted++);
            }
        }
        lineCycles = lineCycles % LINE_CYCLES;
        if (frameCycles >= FRAME_CYCLES) {
            lastLinePainted = 0;
            ula.onNextFrame();
            frameCycles = frameCycles % FRAME_CYCLES;
            if (canvas != null) {
                canvas.runPaintCycle(); // expensive operation
            }
        }
    }
}
