/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;

public class ModeSelector {

    private final Mode[] modes;
    private Mode currentMode;
    public ModeSelector(DrawingPanel panel, DrawingModel drawingModel) {
        modes = new Mode[]{
                new MovingMode(panel, drawingModel),
                new ModelingMode(panel, drawingModel),
                new ResizingMode(panel, drawingModel),
                new SelectingMode(panel, drawingModel)
        };
    }

    public void select(SelectMode mode) {
        currentMode = modes[mode.ordinal()];
    }

    public Mode get() {
        return currentMode;
    }

    public enum SelectMode {
        MOVING, MODELING, RESIZING, SELECTING
    }

}
