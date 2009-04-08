package gui.syntaxHighlighting;

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
