/*
 * KISS, YAGNI, DRY
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

import emustudio.drawing.mode.ModeSelector.SelectMode;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

public interface Mode {

    public void drawTemporaryGraphics(Graphics2D graphics);

    public SelectMode mouseClicked(MouseEvent e);

    public SelectMode mousePressed(MouseEvent e);

    public SelectMode mouseReleased(MouseEvent e);

    public SelectMode mouseDragged(MouseEvent e);

    public SelectMode mouseMoved(MouseEvent e);

}
