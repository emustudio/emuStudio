/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.device.abstracttape.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Public API of the abstract tape.
 */
@SuppressWarnings("unused")
@PluginContext
public interface AbstractTapeContext extends DeviceContext<TapeSymbol> {

    /**
     * Clear content of the tape.
     */
    void clear();

    /**
     * Set this tape to left-bounded or unbounded.
     *
     * @param bounded true if the tape should be left-bounded,
     *                false if unbounded.
     */
    void setLeftBounded(boolean bounded);

    /**
     * Determine if the tape is left-bounded.
     *
     * @return true - left-bounded, false - unbounded.
     */
    boolean isLeftBounded();

    /**
     * Move the tape one symbol to the left.
     * <p>
     * If the tape is left-bounded and the old position is 0, tape won't move. Otherwise the tape
     * will expand to the left - add new empty symbol to position 0 and shift the rest of the content to the right.
     *
     * @return true if the tape has been moved; false otherwise (if it is left-bounded and the position is 0).
     */
    boolean moveLeft();

    /**
     * Move tape to the right. If the tape is too short, it is expanded to the right (added new empty symbol).
     */
    void moveRight();

    /**
     * Allow or disallow to edit the tape.
     * <p>
     * If the tape is editable, the user (in GUI) can add, modify or remove symbols from the tape.
     * Otherwise it is driven only by the CPU.
     *
     * @param editable true if yes, false if not.
     */
    void setEditable(boolean editable);

    /**
     * Get symbol at the specified position.
     *
     * @param position position in the tape, starting from 0
     * @return symbol at given position; or Optional.empty() if the position is out of bounds
     */
    Optional<TapeSymbol> getSymbolAt(int position);

    /**
     * Set symbol at the specified position.
     *
     * If the position is < 0, then no symbol will be set.
     *
     * @param position position in the tape, starting from 0
     * @param symbol   symbol value
     */
    void setSymbolAt(int position, TapeSymbol symbol);

    /**
     * Remove symbol at given position
     *
     * @param position symbol position in the tape
     */
    void removeSymbolAt(int position);

    /**
     * Sets whether the symbol at which the head is pointing should be "highlighted" in GUI.
     *
     * @param highlight true if yes; false otherwise.
     */
    void setHighlightHeadPosition(boolean highlight);

    /**
     * Seths whether the tape should be cleared at emulation reset.
     *
     * @param clear true if yes; false otherwise.
     */
    void setClearAtReset(boolean clear);

    /**
     * Set title (purpose) of the tape.
     *
     * @param title title of the tape
     */
    void setTitle(String title);

    /**
     * Determines if the symbol positions should be displayed in GUI.
     *
     * @return true if yes; false otherwise
     */
    boolean getShowPositions();

    /**
     * Set whether the symbol positions should be displayed in GUI.
     *
     * @param showPositions true if yes; false otherwise.
     */
    void setShowPositions(boolean showPositions);

    /**
     * Get the tape head position.
     *
     * @return current position in the tape; starts from 0
     */
    int getHeadPosition();

    /**
     * Get the size of the tape
     *
     * @return tape size
     */
    int getSize();

    /**
     * Determine if the tape is empty.
     *
     * @return true if the tape is empty; false otherwise.
     */
    boolean isEmpty();

}
