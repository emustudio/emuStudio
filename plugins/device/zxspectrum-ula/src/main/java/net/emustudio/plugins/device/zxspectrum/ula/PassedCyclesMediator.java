/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.plugins.device.zxspectrum.bus.api.TimingProfile;
import net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas;

import java.util.Objects;

import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.SCREEN_IMAGE_HEIGHT;

/**
 * Converts CPU T-state notifications into ULA frame, line, paint, and interrupt events.
 *
 * <p>The ZX Spectrum 48K video timing is regular enough that the mediator can work purely from
 * accumulated T-states:
 * <ul>
 * <li>one display line = {@code 224} T-states</li>
 * <li>one frame = {@code (64 + 192 + 56) * 224 = 69888} T-states</li>
 * <li>frame rate = {@code 3_500_000 / 69888 ~= 50.08 Hz}</li>
 * <li>the ULA holds {@code /INT} low for {@code 32} T-states at each frame boundary</li>
 * </ul>
 *
 * <p>Mechanically, each {@link #passedCycles(long)} call does four things in order:
 * <ol>
 * <li>forwards the same cycle count to {@link ULA#passedCycles(long)} so audio stays aligned with
 * the CPU clock</li>
 * <li>adds the cycles to {@code lineCycles} and {@code frameCycles}</li>
 * <li>when a line boundary is crossed, draws the next line and keeps only the modulo remainder</li>
 * <li>when a frame boundary is crossed, starts the next frame, requests a repaint, and later clears
 * the interrupt exactly {@code INTERRUPT_TSTATES} after the boundary</li>
 * </ol>
 *
 * <p>The modulo operations are the same idea as in fixed-point resampling: they preserve leftover
 * partial-line and partial-frame time across callbacks so timing does not drift when instructions
 * end between display boundaries.
 *
 * <p>Not thread-safe – called only from the CPU execution thread.  Memory-contention side-effects
 * from non-CPU readers (e.g. the disassembler on the AWT thread) are blocked at the source by
 * {@code EmulatorEngine.addExecutedCyclesPerTimeSlice}, so no concurrent calls reach this class.
 *
 * <p>References:
 * <ul>
 * <li><a href="https://worldofspectrum.org/faq/reference/48kreference.htm">World of Spectrum:
 * 48K ZX Spectrum Technical Information</a></li>
 * <li><a href="https://worldofspectrum.org/faq/reference/z80reference.htm">World of Spectrum:
 * Z80 Technical Information</a></li>
 * </ul>
 */
public class PassedCyclesMediator implements CPUContext.PassedCyclesListener {
    private long frameCycles = 0;
    private long lineCycles = 0;
    private int lastLinePainted = 0;
    private boolean interruptActive = false;

    private volatile DisplayCanvas canvas;
    private final ULA ula;
    private final TimingProfile timing;

    public PassedCyclesMediator(ULA ula) {
        this.ula = Objects.requireNonNull(ula);
        this.timing = ula.getProfile();
    }

    public void setCanvas(DisplayCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Resets all internal timing state to the beginning of a new frame.
     * <p>
     * Call this when the device is reset so that residual cycle counts from a previous emulation
     * run cannot push {@code lastLinePainted} past the valid line range.
     */
    public void reset() {
        frameCycles = 0;
        lineCycles = 0;
        lastLinePainted = 0;
        interruptActive = false;
    }

    /**
     * Accumulates CPU time and dispatches any line, frame, or interrupt events whose thresholds
     * were crossed by this batch.
     */
    @Override
    public void passedCycles(long cycles) {
        ula.passedCycles(cycles);
        frameCycles += cycles;
        lineCycles += cycles;

        // Draw completed lines in batch
        DisplayCanvas canvas = this.canvas; // Read volatile once
        if (canvas != null) {
            if (lineCycles >= timing.displayLineTstates && lastLinePainted < SCREEN_IMAGE_HEIGHT) {
                canvas.drawNextLine(lastLinePainted++);
            }
        }
        lineCycles = lineCycles % timing.displayLineTstates;
        if (frameCycles >= timing.displayFrameTstates) {
            lastLinePainted = 0;
            ula.onNextFrame();
            frameCycles = frameCycles % timing.displayFrameTstates;
            lineCycles = 0; // keep line timing in sync with frame timing
            interruptActive = true;
            if (canvas != null) {
                canvas.repaint();
            }
        }
        // ULA releases INT exactly INT_DURATION T-states after frame boundary.
        if (interruptActive && frameCycles >= timing.interruptTstates) {
            ula.clearInterrupt();
            interruptActive = false;
        }
    }
}
