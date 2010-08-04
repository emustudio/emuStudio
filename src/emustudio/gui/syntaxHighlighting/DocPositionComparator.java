/**
 * DocPositionComparator.java
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
 */
package emustudio.gui.syntaxHighlighting;

import java.util.Comparator;

/**
 * A comparator appropriate for use with Collections of
 * DocPositions.
 */
@SuppressWarnings("unchecked")
class DocPositionComparator implements Comparator{
    /**
     * Does this Comparator equal another?
     * Since all DocPositionComparators are the same, they
     * are all equal.
     *
     * @return true for DocPositionComparators, false otherwise.
     */
    public boolean equals(Object obj){
        if (obj instanceof DocPositionComparator){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Compare two DocPositions
     *
     * @param o1 first DocPosition
     * @param o2 second DocPosition
     * @return negative if first < second, 0 if equal, positive if first > second
     */
    public int compare(Object o1, Object o2){
        if (o1 instanceof DocPosition && o2 instanceof DocPosition){
            DocPosition d1 = (DocPosition)(o1);
            DocPosition d2 = (DocPosition)(o2);
            return (d1.getPosition() - d2.getPosition());
        } else if (o1 instanceof DocPosition){
            return -1;
        } else if (o2 instanceof DocPosition){
            return 1;
        } else if (o1.hashCode() < o2.hashCode()){
            return -1;
        } else if (o2.hashCode() > o1.hashCode()){
            return 1;
        } else {
            return 0;
        }
    }
}
