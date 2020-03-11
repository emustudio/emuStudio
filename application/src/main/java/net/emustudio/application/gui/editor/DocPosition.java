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
package net.emustudio.application.gui.editor;

/**
 * A wrapper for a position in a document appropriate for storing
 * in a collection.
 */
class DocPosition implements Comparable<DocPosition> {
    private int position;

    int getPosition() {
        return position;
    }

    DocPosition(int position) {
        this.position = position;
    }

    /**
     * Adjust this position.
     * This is useful in cases that an amount of text is inserted
     * or removed before this position.
     *
     * @param adjustment amount (either positive or negative) to adjust this position.
     */
    void adjustPosition(int adjustment) {
        position += adjustment;
    }

    /**
     * Two DocPositions are equal iff they have the same internal position.
     *
     * @return if this DocPosition represents the same position as another.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DocPosition) {
            DocPosition d = (DocPosition) (obj);
            return this.position == d.position;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + this.position;
        return hash;
    }

    @Override
    public String toString() {
        return "" + position;
    }

    @Override
    public int compareTo(DocPosition o) {
        return position - o.getPosition();
    }
}
