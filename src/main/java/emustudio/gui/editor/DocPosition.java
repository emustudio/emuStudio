/*
 * DocPosition.java
 *
 * KISS, YAGNI
 * 
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
 *
 */
package emustudio.gui.editor;

/**
 * A wrapper for a position in a document appropriate for storing
 * in a collection.
 */
class DocPosition {

    /**
     * The actual position
     */
    private int position;

    /**
     * Get the position represented by this DocPosition
     *
     * @return the position
     */
    int getPosition(){
        return position;
    }

    /**
     * Construct a DocPosition from the given offset into the document.
     *
     * @param position The position this DocObject will represent
     */
    public DocPosition(int position){
        this.position = position;
    }

    /**
     * Adjust this position.
     * This is useful in cases that an amount of text is inserted
     * or removed before this position.
     *
     * @param adjustment amount (either positive or negative) to adjust this position.
     * @return the DocPosition, adjusted properly.
     */
    public DocPosition adjustPosition(int adjustment){
        position += adjustment;
        return this;
    }

    /**
     * Two DocPositions are equal iff they have the same internal position.
     *
     * @return if this DocPosition represents the same position as another.
     */
    @Override
    public boolean equals(Object obj){
        if (obj instanceof DocPosition){
            DocPosition d = (DocPosition)(obj);
            if (this.position == d.position){
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * A string representation useful for debugging.
     *
     * @return A string representing the position.
     */
    @Override
    public String toString(){
        return "" + position;
    }
}
