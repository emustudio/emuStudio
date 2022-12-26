/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
