/*
 * AbstractTapeContext.java
 *
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.ram.abstracttape;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;

@ContextType
public interface AbstractTapeContext extends DeviceContext<String> {
    /**
     * Clear content of the tape leaving only one empty string
     * symbol on the position 0.
     */
    public void clear();

    /**
     * Set this tape to left-bounded or unbounded.
     *
     * @param bounded true if the tape should be left-bounded,
     *                false if unbounded.
     */
    public void setBounded(boolean bounded);

    /**
     * Method returns if the tape is left-bounded or unbounded.
     *
     * @return true - left-bounded, false - unbounded.
     */
    public boolean isBounded();

    /**
     * Method moves the tape to the left. If the tape is left-bounded
     * and the old position is 0, tape won't move. Otherwise the tape
     * will expand to the left - add new empty symbol to position 0 and shift
     * rest content to the right.
     *
     * @return true if tape was moved, false if not (if it is left-bounded
     * and we are at position 0).
     */
    public boolean moveLeft();

    /**
     * Move tape to the right. If the tape is too short, it is expanded to
     * the right (added new empty symbol).
     */
    public void moveRight();

    /**
     * Set/unset this tape editable by the user. If the tape is editable,
     * user (in GUI) can add, modify and remove symbols from the tape.
     * Otherwise it is driven only by the CPU.
     *
     * @param editable true if yes, false if not.
     */
    public void setEditable(boolean editable);

    public String getSymbolAt(int pos);

    public void setSymbolAt(int pos, String symbol);

    public void setPosVisible(boolean visible);

    public void setClearAtReset(boolean clear);

    public void setTitle(String title);

    public boolean getDisplayRowNumbers();

    public void setDisplayRowNumbers(boolean displayRowNumbers);

}
