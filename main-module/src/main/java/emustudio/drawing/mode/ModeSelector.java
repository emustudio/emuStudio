/*
 * KISS, DRY, YAGNI
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package emustudio.drawing.mode;

import emustudio.drawing.DrawingPanel;
import emustudio.drawing.Model;

public class ModeSelector {

    public enum SelectMode {
        MOVING, MODELING, RESIZING, SELECTING
    }

    private final Mode[] modes;
    private Mode currentMode;

    public ModeSelector(DrawingPanel panel, Model model) {
        modes = new Mode[] {
            new MovingMode(panel, model),
            new ModelingMode(panel, model),
            new ResizingMode(panel, model),
            new SelectingMode(panel, model)
        };
    }

    public void select(SelectMode mode) {
        currentMode = modes[mode.ordinal()];
    }

    public Mode get() {
        return currentMode;
    }

}
