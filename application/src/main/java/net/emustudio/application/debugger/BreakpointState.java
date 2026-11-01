/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

/** Execution breakpoint states used by the instruction table. All enabled states inspect memory. */
public enum BreakpointState {
    OFF("Off. Click to set breakpoint."),
    ON("Breakpoint. Click to keep inspection while stepping."),
    KEEP_INSPECTION("Breakpoint + keep inspection. Click to remove breakpoint.");

    public final String tooltip;

    BreakpointState(String tooltip) { this.tooltip = tooltip; }

    public BreakpointState next() {
        return this == OFF ? ON : this == ON ? KEEP_INSPECTION : OFF;
    }
}
