/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.drawing.mode;

import emustudio.drawing.Model;
import emustudio.drawing.DrawingPanel;
import emustudio.drawing.Schema;
import java.awt.Point;

abstract class AbstractMode implements Mode {
    protected final DrawingPanel panel;
    protected final Model model;
    protected final Schema schema;

    AbstractMode(DrawingPanel panel, Model model) {
        this.panel = panel;
        this.model = model;
        this.schema = panel.getSchema();
    }

    /**
     * This method searchs for the nearest point that crosses the grid. If the grid is not used, it just return the
     * point represented by the parameter.
     *
     * @param old Point that needs to be corrected by the grid
     * @return nearest grid point from the parameter, or the old point, if grid is not used.
     */
    Point searchGridPoint(Point old) {
        boolean useGrid = schema.isGridUsed();
        int gridGap = schema.getGridGap();
        if (!useGrid || gridGap <= 0) {
            return old;
        }
        int dX = (int) Math.round(old.x / (double) gridGap);
        int dY = (int) Math.round(old.y / (double) gridGap);
        return new Point(dX * gridGap, dY * gridGap);
    }
}
