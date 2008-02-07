/*
 * HighlightThread.java
 *
 * Created on 7.2.2008, 9:56:03
 * hold to: KISS, YAGNI
 *
 */

package emu8.gui.syntaxHighlighting;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import plugins.compiler.ILexer;
import plugins.compiler.IToken;

/**
 *
 * @author vbmacher
 */
public class HighlightThread extends Thread {
    // if highlighting is running
    private boolean iamRunning;
    private ILexer syntaxLexer;
    private DocumentReader reader;
    private DefaultStyledDocument document;
    private Hashtable styles;
    private Object doclock;
    
    /**
     * Keep a list of places in the file that it is safe to restart the
     * highlighting.  This happens whenever the lexer reports that it has
     * returned to its initial state.  Since this list needs to be sorted
     * and we need to be able to retrieve ranges from it, it is stored in a
     * balanced tree.
     */
    private TreeSet iniPositions = new TreeSet(new DocPositionComparator());

    /**
     * As we go through and remove invalid positions we will also be finding
     * new valid positions. 
     * Since the position list cannot be deleted from and written to at the same
     * time, we will keep a list of the new positions and simply add it to the
     * list of positions once all the old positions have been removed.
     */
    private HashSet newPositions = new HashSet();

    /**
     * A simple wrapper representing something that needs to be colored.
     * Placed into an object so that it can be stored in a Vector.
     */
    private class RecolorEvent {
        public int position;
        public int adjustment;
        public RecolorEvent(int position, int adjustment){
            this.position = position;
            this.adjustment = adjustment;
        }
    }

    /**
     * Vector that stores the communication between the two threads.
     */
    private volatile Vector v = new Vector();

    /**
     * The amount of change that has occurred before the place in the
     * document that we are currently highlighting (lastPosition).
     */
    private volatile int change = 0;

    /**
     * The last position colored
     */
    private volatile int lastPosition = -1;

    private volatile boolean asleep = false;

    /**
     * When accessing the vector, we need to create a critical section.
     * we will synchronize on this object to ensure that we don't get
     * unsafe thread behavior.
     */
    private Object lock = new Object();
    
    public HighlightThread(ILexer syntaxLexer, DocumentReader reader,
            DefaultStyledDocument document, Hashtable styles, Object doclock) {
        this.doclock = doclock;
        this.syntaxLexer = syntaxLexer;
        this.reader = reader;
        this.document = document;
        this.styles = styles;
        iamRunning = false; 
    }
    
    /**
     * Tell the Syntax Highlighting thread to take another look at this
     * section of the document.  It will process this as a FIFO.
     * This method should be done inside a doclock.
     */
    public void color(int position, int adjustment){
        // figure out if this adjustment effects the current run.
        // if it does, then adjust the place in the document
        // that gets highlighted.
        if (position < lastPosition){
            if (lastPosition < position - adjustment){
                change -= lastPosition - position;
            } else {
                change += adjustment;
            }
        }
        synchronized(lock){
            v.add(new RecolorEvent(position, adjustment));
            if (asleep){
                this.interrupt();
            }
        }
    }

    
    /**
     * The colorer runs forever and may sleep for long
     * periods of time.  It should be interrupted every
     * time there is something for it to do.
     */
    public void run() {
        if (syntaxLexer == null ||
                (syntaxLexer instanceof ILexer) == false) return;
        
        int position = -1;
        int adjustment = 0;
        // if we just finish, we can't go to sleep until we
        // ensure there is nothing else for us to do.
        // use try again to keep track of this.
        boolean tryAgain = false;
        iamRunning = true;
        while (iamRunning) {  // forever
            synchronized(lock){
                if (v.size() > 0){
                    RecolorEvent re = (RecolorEvent)(v.elementAt(0));
                    v.removeElementAt(0);
                    position = re.position;
                    adjustment = re.adjustment;
                } else {
                    tryAgain = false;
                    position = -1;
                    adjustment = 0;
                }
            }
            if (position != -1){
                SortedSet workingSet;
                Iterator workingIt;
                DocPosition startRequest = new DocPosition(position);
                DocPosition endRequest = new DocPosition(position + ((adjustment>=0)?adjustment:-adjustment));
                DocPosition dp;
                DocPosition dpStart = null;
                DocPosition dpEnd = null;

                // find the starting position.  We must start at least one
                // token before the current position
                try {
                    // all the good positions before
                    workingSet = iniPositions.headSet(startRequest);
                    // the last of the stuff before
                    dpStart = ((DocPosition)workingSet.last());
                } catch (NoSuchElementException x){
                    // if there were no good positions before the requested start,
                    // we can always start at the very beginning.
                    dpStart = new DocPosition(0);
                }
                // if stuff was removed, take any removed positions off the list.
                if (adjustment < 0){
                    workingSet = iniPositions.subSet(startRequest, endRequest);
                    workingIt = workingSet.iterator();
                    while (workingIt.hasNext()){
                        workingIt.next();
                        workingIt.remove();
                    }
                }
                // adjust the positions of everything after the insertion/removal.
                workingSet = iniPositions.tailSet(startRequest);
                workingIt = workingSet.iterator();
                while (workingIt.hasNext()){
                    ((DocPosition)workingIt.next()).adjustPosition(adjustment);
                }
                // now go through and highlight as much as needed
                workingSet = iniPositions.tailSet(dpStart);
                workingIt = workingSet.iterator();
                dp = null;
                if (workingIt.hasNext()) dp = (DocPosition)workingIt.next();

                try {
                    IToken t;
                    boolean done = false;
                    dpEnd = dpStart;
                    synchronized (doclock){
                        // we are playing some games with the lexer for efficiency.
                        // we could just create a new lexer each time here, but instead,
                        // we will just reset it so that it thinks it is starting at the
                        // beginning of the document but reporting a funny start position.
                        // Reseting the lexer causes the close() method on the reader
                        // to be called but because the close() method has no effect on the
                        // DocumentReader, we can do this.
                        syntaxLexer.reset(reader, 0, dpStart.getPosition(), 0);
                        // After the lexer has been set up, scroll the reader so that it
                        // is in the correct spot as well.
                        reader.seek(dpStart.getPosition());
                        // we will highlight tokens until we reach a good stopping place.
                        // the first obvious stopping place is the end of the document.
                        // the lexer will return null at the end of the document and wee
                        // need to stop there.
                        t = syntaxLexer.getSymbol();
                    }
                    newPositions.add(dpStart);
                    while (!done && t.getType() != IToken.TEOF){
                        // this is the actual command that colors the stuff.
                        // Color stuff with the description of the style matched
                        // to the hash table that has been set up ahead of time.
                        synchronized (doclock){
                            if (t.getCharEnd() <= document.getLength()) {
//                                System.out.println("recoloring from: " 
  //                                      + String.valueOf(t.getCharBegin()+change)
    //                                    + ", len: " + String.valueOf(t.getCharEnd()-t.getCharBegin())
      //                                  + ", type: " + t.getType()
        //                                + ", change: " + change
          //                              + ", charB: " + t.getCharBegin()
            //                            + ", charE:" + t.getCharEnd() 
              //                          + ", doc.len = " + document.getLength());
                                reColor(t.getCharBegin() + change, 
                                        t.getCharEnd()-t.getCharBegin(),
                                        t.getType());
                                // record the position of the last bit of text that we colored
                                dpEnd = new DocPosition(t.getCharEnd());
                            }
                            lastPosition = (t.getCharEnd() + change);
                        }
                        yield();
                        // The other more complicated reason for doing no more highlighting
                        // is that all the colors are the same from here on out anyway.
                        // We can detect this by seeing if the place that the lexer returned
                        // to the initial state last time we highlighted is the same as the
                        // place that returned to the initial state this time.
                        // As long as that place is after the last changed text, everything
                        // from there on is fine already.
//                        if (t.getState() == Token.INITIAL_STATE){
                            // look at all the positions from last time that are less than or
                            // equal to the current position
                            while (dp != null && dp.getPosition() <= t.getCharEnd()){
                                if (dp.getPosition() == t.getCharEnd() 
                                        && dp.getPosition() >= endRequest.getPosition()){
                                    // we have found a state that is the same
                                    done = true;
                                    dp = null;
                                } else if (workingIt.hasNext()){
                                    // didn't find it, try again.
                                    dp = (DocPosition)workingIt.next();
                                } else {
                                    // didn't find it, and there is no more info from last
                                    // time.  This means that we will just continue
                                    // until the end of the document.
                                    dp = null;
                                }
                            }
                            // so that we can do this check next time, record all the
                            // initial states from this time.
                            newPositions.add(dpEnd);
                       // }
                        synchronized (doclock){
                            t = syntaxLexer.getSymbol();
                        }
                    }
                    // remove all the old initial positions from the place where
                    // we started doing the highlighting right up through the last
                    // bit of text we touched.
                    workingIt = iniPositions.subSet(dpStart, dpEnd).iterator();
                    while (workingIt.hasNext()){
                        workingIt.next();
                        workingIt.remove();
                    }

                    // Remove all the positions that are after the end of the file.:
                    workingIt = iniPositions.tailSet(new DocPosition(document.getLength())).iterator();
                    while (workingIt.hasNext()){
                        workingIt.next();
                        workingIt.remove();
                    }

                    // and put the new initial positions that we have found on the list.
                    iniPositions.addAll(newPositions);
                    newPositions.clear();
                } catch (IOException x){}
                
                synchronized (doclock){
                    lastPosition = -1;
                    change = 0;
                }
                // since we did something, we should check that there is
                // nothing else to do before going back to sleep.
                tryAgain = true;
            }                
            asleep = true;
            if (!tryAgain){
                try { sleep (0xffffff); }
                catch (InterruptedException x){}
            }
            asleep = false;
        }
    }
    
    public void stopRun() { iamRunning = false; }
    private void reColor(int position, int length, int tokenType) {
        SimpleAttributeSet style = (SimpleAttributeSet)styles.get(tokenType);
        if (style == null)
            style = (SimpleAttributeSet)styles.get(IToken.ERROR);
        document.setCharacterAttributes(position,length,style,true);         
    }

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
        int getPosition() { return position; }

        /**
         * Construct a DocPosition from the given offset into the document.
         *
         * @param position The position this DocObject will represent
         */
        public DocPosition(int position) { this.position = position; }

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
    }

    /**
     * A comparator appropriate for use with Collections of
     * DocPositions.
     */
    class DocPositionComparator implements Comparator{
        /**
         * Does this Comparator equal another?
         * Since all DocPositionComparators are the same, they
         * are all equal.
         *
         * @return true for DocPositionComparators, false otherwise.
         */
        public boolean equals(Object obj) {
            if (obj instanceof DocPositionComparator) return true;
            else return false;
        }

        /**
         * Compare two DocPositions
         *
         * @param o1 first DocPosition
         * @param o2 second DocPosition
         * @return negative if first < second, 0 if equal, positive if first > second
         */
        public int compare(Object o1, Object o2){
            if (o1 instanceof DocPosition && o2 instanceof DocPosition) {
                DocPosition d1 = (DocPosition)(o1);
                DocPosition d2 = (DocPosition)(o2);
                return (d1.getPosition() - d2.getPosition());
            } else if (o1 instanceof DocPosition) return -1;
            else if (o2 instanceof DocPosition) return 1;
            else if (o1.hashCode() < o2.hashCode()) return -1;
            else if (o2.hashCode() > o1.hashCode()) return 1;
            else return 0;   
        }
    }
}
