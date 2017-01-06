/*
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2001, Stephen Ostermiller
 * (c) Copyright 2006-2016, Peter Jakubčo
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

import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.Token;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.text.SimpleAttributeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The syntax highlighting thread.
 */
class HighlightThread extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(HighlightThread.class);

    /**
     * Keep a list of places in the file that it is safe to restart the
     * highlighting.  This happens whenever the lexer reports that it has
     * returned to its initial state.  Since this list needs to be sorted
     * and we need to be able to retrieve ranges from it, it is stored in a
     * balanced tree.
     */
    private final SortedSet<DocPosition> initPositions = new TreeSet<>();
    /**
     * As we go through and remove invalid positions we will also be finding
     * new valid positions.
     * Since the position list cannot be deleted from and written to at the same
     * time, we will keep a list of the new positions and simply add it to the
     * list of positions once all the old positions have been removed.
     */
    private final Set<DocPosition> newPositions = new HashSet<>();

    /**
     * A simple wrapper representing something that needs to be colored.
     * Placed into an object so that it can be stored in a Vector.
     */
    private static class RecolorEvent {

        public int position;
        int adjustment;

        RecolorEvent(int position, int adjustment) {
            this.position = position;
            this.adjustment = adjustment;
        }
    }
    /**
     * Vector that stores the communication between the two threads.
     */
    private volatile Queue<RecolorEvent> recolorEvents = new LinkedList<>();

    /**
     * The amount of change that has occurred before the place in the
     * document that we are currently highlighting (lastPosition).
     */
    private int change = 0;
    /**
     * The last position colored
     */
    private int lastPosition = -1;
    private volatile boolean isSleeping = false;

    private volatile boolean shouldStop;
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
    private DocumentReader documentReader;
    /**
     * Lexical analyzer object
     */
    private LexicalAnalyzer syntaxLexer;

    private final Map<Integer, HighlightStyle> styles;

    HighlightThread(LexicalAnalyzer lex, DocumentReader lexReader,
                    HighLightedDocument document,
                    Map<Integer, HighlightStyle> styles) {
        super("HighlightThread");
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

    private SimpleAttributeSet getStyle(int tokenType) {
        return styles.get(tokenType);
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
        synchronized (lock) {
            if (position < lastPosition) {
                if (adjustment < 0) {
                    change -= lastPosition - position;
                } else {
                    change += adjustment;
                }
            }
            recolorEvents.add(new RecolorEvent(position, adjustment));
            if (isSleeping) {
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
        // if we just finish, we can't go to sleep until we
        // ensure there is nothing else for us to do.
        // use try again to keep track of this.
        boolean tryAgain = false;
        while (!shouldStop) {
            final int adjustment;

            synchronized (lock) {
                if (!recolorEvents.isEmpty()) {
                    RecolorEvent recolorEvent = recolorEvents.poll();
                    position = recolorEvent.position;
                    adjustment = recolorEvent.adjustment;
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
                workingSet = initPositions.headSet(startRequest);
                if (workingSet.isEmpty()) {
                    dpStart = new DocPosition(0);
                } else {
                    dpStart = workingSet.last();
                }

                // if stuff was removed, take any removed positions off the list.
                if (adjustment < 0) {
                    initPositions.subSet(startRequest, endRequest).clear();
                }

                // adjust the positions of everything after the insertion/removal.
                initPositions.tailSet(startRequest).forEach(
                    pos -> pos.adjustPosition(adjustment)
                );

                // now go through and highlight as much as needed
                workingSet = initPositions.tailSet(dpStart);
                workingIt = workingSet.iterator();
                dp = null;
                if (workingIt.hasNext()) {
                    dp = workingIt.next();
                }
                try {
                    Token token;
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
                    token = syntaxLexer.getSymbol();
                    newPositions.add(dpStart);
                    while (!done && token != null && token.getType() != Token.TEOF) {
                        // this is the actual command that colors the stuff.
                        // Color stuff with the description of the style matched
                        // to the hash table that has been set up ahead of time.
                        int tEnd = token.getOffset() + token.getLength();

                        int tBegin;
                        synchronized (lock) {
                            tBegin = token.getOffset() + change;
                        }
                        int docLen = 0;
                        document.readLock();
                        try {
                            docLen = document.getLength();
                        } finally {
                            document.readUnlock();
                        }
                        if (tEnd <= docLen) {
                            try {
                                document.setCharacterAttributes(tBegin, token.getLength(), getStyle(token.getType()), true);
                            } catch (Exception e) {
                                logger.error(
                                    "[pos={},style={},,len={}] Could not set character attributes",
                                    tBegin, getStyle(token.getType()).toString(), token.getLength(), e
                                );
                                continue;
                            }
                            // record the position of the last bit of text that we colored
                            dpEnd = new DocPosition(tEnd);
                        }
                        synchronized (lock) {
                            lastPosition = (tEnd + change);
                        }
                        // The other more complicated reason for doing no more highlighting
                        // is that all the colors are the same from here on out anyway.
                        // We can detect this by seeing if the place that the lexer returned
                        // to the initial state last time we highlighted is the same as the
                        // place that returned to the initial state this time.
                        // As long as that place is after the last changed text, everything
                        // from there on is fine already.
                        if (token.isInitialLexicalState() && token.getType() != Token.ERROR) {
                            //System.out.println(t);
                            // look at all the positions from last time that are less than or
                            // equal to the current position
                            tEnd = token.getOffset() + token.getLength();
                            while (dp != null && dp.getPosition() <= tEnd) {
                                if (dp.getPosition() == tEnd && dp.getPosition() >= endRequest.getPosition()) {
                                    // we have found a state that is the same
                                    done = true;
                                    dp = null;
                                } else if (workingIt.hasNext()) {
                                    // didn't find it, try again.
                                    dp = workingIt.next();
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
                        token = syntaxLexer.getSymbol();
                    }
                    // remove all the old initial positions from the place where
                    // we started doing the highlighting right up through the last
                    // bit of text we touched.
                    workingIt = initPositions.subSet(dpStart, dpEnd).iterator();
                    while (workingIt.hasNext()) {
                        workingIt.next();
                        workingIt.remove();
                    }

                    // Remove all the positions that are after the end of the file.:
                    document.readLock();
                    int docLen = 0;
                    try {
                        docLen = document.getLength();
                    } finally {
                        document.readUnlock();
                    }
                    workingIt = initPositions.tailSet(new DocPosition(docLen)).iterator();
                    while (workingIt.hasNext()) {
                        workingIt.next();
                        workingIt.remove();
                    }

                    // and put the new initial positions that we have found on the list.
                    initPositions.addAll(newPositions);
                    newPositions.clear();
                } catch (Exception x) { // catch all runtime exceptions as well, such as NumberFormatException
                    logger.error("There was an exception while performing syntax highlighting", x);
                }
                synchronized (lock) {
                    lastPosition = -1;
                    change = 0;
                }
                // since we did something, we should check that there is
                // nothing else to do before going back to sleep.
                tryAgain = true;
            }
            if (!tryAgain) {
                isSleeping = true;
                try {
                    Thread.sleep(0xffffff);
                } catch (InterruptedException x) {
                } finally {
                    isSleeping = false;
                }
            }
        }
    }

    void stopMe() {
        shouldStop = true;
        synchronized (lock) {
            recolorEvents.clear();
            if (isSleeping) {
                this.interrupt();
            }
            try {
                this.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            document.setThread(null);
        }
    }

    void colorAll() {
        synchronized (lock) {
            recolorEvents.clear();
        }
        int docLen = 0;
        document.readLock();
        try {
            docLen = document.getLength();
        } finally {
            document.readUnlock();
        }
        color(0, docLen);
    }
}
