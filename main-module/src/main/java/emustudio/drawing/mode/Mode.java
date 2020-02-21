/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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

import emustudio.drawing.mode.ModeSelector.SelectMode;

import java.awt.*;
import java.awt.event.MouseEvent;

public interface Mode {

    void drawTemporaryGraphics(Graphics2D graphics);

    SelectMode mouseClicked(MouseEvent e);

    SelectMode mousePressed(MouseEvent e);

    SelectMode mouseReleased(MouseEvent e);

    SelectMode mouseDragged(MouseEvent e);

    SelectMode mouseMoved(MouseEvent e);

}
