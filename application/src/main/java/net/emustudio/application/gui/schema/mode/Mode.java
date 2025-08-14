/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import java.awt.*;
import java.awt.event.MouseEvent;

public interface Mode {

    void drawTemporaryGraphics(Graphics2D graphics);

    ModeSelector.SelectMode mouseClicked(MouseEvent e);

    ModeSelector.SelectMode mousePressed(MouseEvent e);

    ModeSelector.SelectMode mouseReleased(MouseEvent e);

    ModeSelector.SelectMode mouseDragged(MouseEvent e);

    ModeSelector.SelectMode mouseMoved(MouseEvent e);

}
