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
import emustudio.drawing.mode.ModeSelector.SelectMode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

public class SelectingMode extends AbstractMode {

    public SelectingMode(DrawingPanel panel, Model model) {
        super(panel, model);
    }

    @Override
    public void drawTemporaryGraphics(Graphics2D graphics) {
        if ((model.selectionStart != null) && (model.selectionEnd != null)) {
            graphics.setColor(Color.BLUE);
            graphics.setStroke(DrawingPanel.DASHED_LINE);

            int x = model.selectionStart.x;
            int y = model.selectionStart.y;

            if (model.selectionEnd.x < x) {
                x = model.selectionEnd.x;
            }
            if (model.selectionEnd.y < y) {
                y = model.selectionEnd.y;
            }
            int w = model.selectionEnd.x - model.selectionStart.x;
            int h = model.selectionEnd.y - model.selectionStart.y;

            if (w < 0) {
                w = -w;
            }
            if (h < 0) {
                h = -h;
            }
            graphics.drawRect(x, y, w, h);
        }
    }

    @Override
    public SelectMode mouseClicked(MouseEvent e) {
        return SelectMode.SELECTING;
    }

    @Override
    public SelectMode mousePressed(MouseEvent e) {
        return SelectMode.SELECTING;
    }

    @Override
    public SelectMode mouseReleased(MouseEvent e) {
        Point p = e.getPoint();

        int x = model.selectionStart.x;
        int y = model.selectionStart.y;

        if (model.selectionEnd == null) {
            model.selectionEnd = p;
        }

        if (model.selectionEnd.x < x) {
            x = model.selectionEnd.x;
        }
        if (model.selectionEnd.y < y) {
            y = model.selectionEnd.y;
        }
        int w = model.selectionEnd.x - model.selectionStart.x;
        int h = model.selectionEnd.y - model.selectionStart.y;

        if (w < 0) {
            w = -w;
        }
        if (h < 0) {
            h = -h;
        }

        schema.selectElements(x, y, w, h);

        model.selectionStart = null;
        model.selectionEnd = null;
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mouseDragged(MouseEvent e) {
        model.selectionEnd = e.getPoint();
        return SelectMode.SELECTING;
    }

    @Override
    public SelectMode mouseMoved(MouseEvent e) {
        return SelectMode.SELECTING;
    }

}
