/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.Schema;

import java.util.Objects;

abstract class AbstractMode implements Mode {
    protected final DrawingPanel panel;
    protected final DrawingModel drawingModel;
    protected final Schema schema;

    AbstractMode(DrawingPanel panel, DrawingModel drawingModel) {
        this.panel = Objects.requireNonNull(panel);
        this.drawingModel = Objects.requireNonNull(drawingModel);
        this.schema = Objects.requireNonNull(panel.getSchema());
    }
}
