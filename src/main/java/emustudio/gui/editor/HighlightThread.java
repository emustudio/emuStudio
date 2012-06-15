/*
 * HighlightThread.java
 *
 * Created on 21.8.2008, 8:56:32
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2001, Stephen Ostermiller 
 * http://ostermiller.org/contact.pl?regarding=Syntax+Highlighting
 * Copyright (C) 2008-2012, Peter Jakubčo
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
package emustudio.gui.editor;

import emulib.plugins.compiler.ILexer;
import emulib.plugins.compiler.IToken;
import java.io.IOException;
import java.util.*;
import javax.swing.text.SimpleAttributeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The syntax highlighting thread.
 * 
 * @author vbmacher
 */
public class HighlightThread extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(HighlightThread.class);

    /**
     * Keep a list of places in the file that it is safe to restart the
     * highlighting.  This happens whenever the lexer reports that it has
     * returned to its initial state.  Since this list needs to be sorted
     * and we need to be able to retrieve ranges from it, it is stored in a
     * balanced tree.
     */
    @SuppressWarnings("unchecked")
    private TreeSet<DocPosition> iniPositions = new TreeSet<DocPosition>(new DocPositionComparator());
    /**
     * As we go through and remove invalid positions we will also be finding
     * new valid positions. 
     * Since the position list cannot be deleted from and written to at the same
     * time, we will keep a list of the new positions and simply add it to the
     * list of positions once all the old positions have been removed.
     */
    private HashSet<DocPosition> newPositions = new HashSet<DocPosition>();

    /**
     * A simple wrapper representing something that needs to be colored.
     * Placed into an object so that it can be stored in a Vector.
     */
    private class RecolorEvent {

        public int position;
        public int adjustment;

        public RecolorEvent(int position, int adjustment) {
            this.position = position;
            this.adjustment = adjustment;
        }
    }
    /**
     * Vector that stores the communication between the two threads.
     */
    private volatile ArrayList<RecolorEvent> v = new ArrayList<RecolorEvent>();
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

    private boolean shouldStop;
    /**
     * When accessing the vector, we need to create a critical section.
     * we will synchronize on this object to ensure that we don't get
     * unsafe thread behavior.
     */
    private final Object lock = new Object();
    /**
     * Document containting the source code
     */
    protected HighLightedDocument document;
    /**
     * Reader of the document
     */
    protected DocumentReader documentReader;
    /**
     * Lexical analyzer object
     */
    protected ILexer syntaxLexer;
    /**
     * A lock for modifying the document, or for
     * actions that depend on the document not being
     * modified.
     */
    //  public static Object doclock = new Object();
    private Map<Integer, HighlightStyle> styles;

    /**
     * Create an instance of the syntax highlighting thread.
     *
     * @param lex lexical analyzer taken from the compiler
     * @param lexReader document reader object of the source code
     * @param document the document containing the source code
     * @param styles styles for particular token types
     */
    public HighlightThread(ILexer lex, DocumentReader lexReader,
            HighLightedDocument document,
            Map<Integer, HighlightStyle> styles) {
        this.syntaxLexer = lex;
        this.document = document;
        this.documentReader = lexReader;
        this.styles = styles;
        shouldStop = false;
        runMe();
    }

    private void runMe() {
        document.setThread(this);
        start();
    }

    /**
     * retrieve the style for the given type of text.
     *
     * @param styleName the label for the type of text ("tag" for example) 
     *      or null if the styleName is not known.
     * @return the style
     */
    private SimpleAttributeSet getStyle(int type) {
        return ((SimpleAttributeSet) styles.get(type));
    }

    /**
     * Tell the Syntax Highlighting thread to take another look at this
     * section of the document.  It will process this as a FIFO.
     * This method should be done inside a lock.
     * 
     * @param position a starting position in the document
     * @param adjustment range of the text block
     */
    public void color(int position, int adjustment) {
        // figure out if this adjustment effects the current run.
        // if it does, then adjust the place in the document
        // that gets highlighted.
        if (position < lastPosition) {
            if (lastPosition < position - adjustment) {
                change -= lastPosition - position;
            } else {
                change += adjustment;
            }
        }
        synchronized (lock) {
            v.add(new RecolorEvent(position, adjustment));
            if (asleep) {
                this.interrupt();
            }
        }
    }

    /**
     * The colorer runs forever and may sleep for long
     * periods of time.  It should be interrupted every
     * time there is something for it to do.
     */
    @Override
    public void run() {
        int position;
        int adjustment;
        // if we just finish, we can't go to sleep until we
        // ensure there is nothing else for us to do.
        // use try again to keep track of this.
        boolean tryAgain = false;
        while (!shouldStop) {  // forever
            synchronized (lock) {
                if (v.size() > 0) {
                    RecolorEvent re = (RecolorEvent) (v.get(0));
                    v.remove(0);
                    position = re.position;
                    adjustment = re.adjustment;
                } else {
                    tryAgain = false;
                    position = -1;
                    adjustment = 0;
                }
            }
            if (position != -1) {
                SortedSet<DocPosition> workingSet;
                Iterator<DocPosition> workingIt;
                DocPosition startRequest = new DocPosition(position);
                DocPosition endRequest = new DocPosition(position + ((adjustment >= 0) ? adjustment : -adjustment));
                DocPosition dp;
                DocPosition dpStart;
                DocPosition dpEnd;

                // find the starting position.  We must start at least one
                // token before the current position
                try {
                    // all the good positions before
                    workingSet = iniPositions.headSet(startRequest);
                    // the last of the stuff before
                    dpStart = ((DocPosition) workingSet.last());
                } catch (NoSuchElementException x) {
                    // if there were no good positions before the requested start,
                    // we can always start at the very beginning.
                    dpStart = new DocPosition(0);
                }

                // if stuff was removed, take any removed positions off the list.
                if (adjustment < 0) {
                    workingSet = iniPositions.subSet(startRequest, endRequest);
                    workingIt = workingSet.iterator();
                    while (workingIt.hasNext()) {
                        workingIt.next();
                        workingIt.remove();
                    }
                }

                // adjust the positions of everything after the insertion/removal.
                workingSet = iniPositions.tailSet(startRequest);
                workingIt = workingSet.iterator();
                while (workingIt.hasNext()) {
                    ((DocPosition) workingIt.next()).adjustPosition(adjustment);
                }

                // now go through and highlight as much as needed
                workingSet = iniPositions.tailSet(dpStart);
                workingIt = workingSet.iterator();
                dp = null;
                if (workingIt.hasNext()) {
                    dp = (DocPosition) workingIt.next();
                }
                try {
                    IToken t;
                    boolean done = false;
                    dpEnd = dpStart;
                    // we are playing some games with the lexer for efficiency.
                    // we could just create a new lexer each time here, but instead,
                    // we will just reset it so that it thinks it is starting at the
                    // beginning of the document but reporting a funny start position.
                    // Reseting the lexer causes the close() method on the reader
                    // to be called but because the close() method has no effect on the
                    // DocumentReader, we can do this.
                    syntaxLexer.reset(documentReader, 0, dpStart.getPosition(), 0);
                    // After the lexer has been set up, scroll the reader so that it
                    // is in the correct spot as well.
                    documentReader.seek(dpStart.getPosition());
                    // we will highlight tokens until we reach a good stopping place.
                    // the first obvious stopping place is the end of the document.
                    // the lexer will return null at the end of the document and wee
                    // need to stop there.
                    t = syntaxLexer.getSymbol();
                    newPositions.add(dpStart);
                    while (!done && t != null && t.getType() != IToken.TEOF) {
                        // this is the actual command that colors the stuff.
                        // Color stuff with the description of the style matched
                        // to the hash table that has been set up ahead of time.
                        int tEnd = t.getOffset() + t.getLength();
                        if (tEnd <= document.getLength()) {
                            try {
                                document.setCharacterAttributes(
                                        t.getOffset() + change,
                                        t.getLength(),
                                        getStyle(t.getType()),
                                        true);
                            } catch(Error e) {
                                // Interrupted attempt to aquire write lock
                                logger.error(new StringBuilder().append("Could not set character attributes [pos=")
                                        .append(t.getOffset() + change).append(",len=").append(t.getLength())
                                        .append(",style=").append(getStyle(t.getType()).toString()).append("]")
                                        .toString(), e);
                                continue;
                            } catch (Exception e) {
                                logger.error(new StringBuilder().append("Could not set character attributes [pos=")
                                        .append(t.getOffset() + change).append(",len=").append(t.getLength())
                                        .append(",style=").append(getStyle(t.getType()).toString()).append("]")
                                        .toString(), e);
                                continue;
                            }
                            // record the position of the last bit of text that we colored
                            dpEnd = new DocPosition(tEnd);
                        }
                        lastPosition = (tEnd + change);
                        // The other more complicated reason for doing no more highlighting
                        // is that all the colors are the same from here on out anyway.
                        // We can detect this by seeing if the place that the lexer returned
                        // to the initial state last time we highlighted is the same as the
                        // place that returned to the initial state this time.
                        // As long as that place is after the last changed text, everything
                        // from there on is fine already.
                        if (t.isInitialLexicalState()) {
                            //System.out.println(t);
                            // look at all the positions from last time that are less than or
                            // equal to the current position
                            tEnd = t.getOffset() + t.getLength();
                            while (dp != null && dp.getPosition() <= tEnd) {
                                if (dp.getPosition() == tEnd && dp.getPosition() >= endRequest.getPosition()) {
                                    // we have found a state that is the same
                                    done = true;
                                    dp = null;
                                } else if (workingIt.hasNext()) {
                                    // didn't find it, try again.
                                    dp = (DocPosition) workingIt.next();
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
                        }
                        t = syntaxLexer.getSymbol();
                    }
                    // remove all the old initial positions from the place where
                    // we started doing the highlighting right up through the last
                    // bit of text we touched.
                    workingIt = iniPositions.subSet(dpStart, dpEnd).iterator();
                    while (workingIt.hasNext()) {
                        workingIt.next();
                        workingIt.remove();
                    }

                    // Remove all the positions that are after the end of the file.:
                    workingIt = iniPositions.tailSet(new DocPosition(document.getLength())).iterator();
                    while (workingIt.hasNext()) {
                        workingIt.next();
                        workingIt.remove();
                    }

                    // and put the new initial positions that we have found on the list.
                    iniPositions.addAll(newPositions);
                    newPositions.clear();

                    /*workingIt = iniPositions.iterator();
                    while (workingIt.hasNext()){
                    System.out.println(workingIt.next());
                    }*/
                    
                    logger.debug(new StringBuilder().append("Started: ").append(dpStart.getPosition()).append(" Ended: ")
                            .append(dpEnd.getPosition()).toString());
                } catch (IOException x) {
                    logger.error("There was an exception while performing syntax highlighting", x);
                }
                lastPosition = -1;
                change = 0;
                // since we did something, we should check that there is
                // nothing else to do before going back to sleep.
                tryAgain = true;
            }
            asleep = true;
            if (!tryAgain) {
                try {
                    Thread.sleep(0xffffff);
                } catch (InterruptedException x) {
                }

            }
            asleep = false;
        }
    }

    /**
     * Method informs this thread to stop.
     */
    public void shouldStop() {
        shouldStop = true;
    }

    /**
     * Color or recolor the entire document
     */
    public void colorAll() {
        color(0, document.getLength());
    }
}
