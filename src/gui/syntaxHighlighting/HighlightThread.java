/*
 * HighlightThread.java
 *
 * Created on 21.8.2008, 8:56:32
 * hold to: KISS, YAGNI
 *
 */

package gui.syntaxHighlighting;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import plugins.compiler.ILexer;
import plugins.compiler.IToken;

/**
 *
 * @author vbmacher
 */
public class HighlightThread extends Thread {
    private SortedSet tokensPos = Collections.synchronizedSortedSet(new TreeSet());
    private Hashtable<Integer,Integer> tokenTypes = new Hashtable<Integer,Integer>();
    
    private ILexer lex;
    private DocumentReader lexReader;
    
    private boolean running = false;
    private boolean pause = false;
    private HashSet<DocumentEvent> work = new HashSet<DocumentEvent>();
    private HighlightThread tthis;
    private Hashtable styles;
    private HashSet<RecolorEvent> recolorWork = new HashSet<RecolorEvent>();

    private class RecolorEvent {
        private SimpleAttributeSet style;
        private int offset;
        private int length;
        
        public RecolorEvent(SimpleAttributeSet style, int offset, int length) {
            this.style = style;
            this.offset = offset;
            this.length = length;
        }
        
        public void reColor() {
            DefaultStyledDocument doc = (DefaultStyledDocument)lexReader.getDocument();   
            synchronized (doc) {
                doc.setCharacterAttributes(offset,length,style,true);
            }
        }
    }
    
    public HighlightThread(ILexer lex, DocumentReader lexReader, Hashtable styles) {
        this.lex = lex;
        this.setName("highlightThread");
        this.lexReader = lexReader;
        this.styles = styles;
        this.tthis = this;
        lexReader.reset();
        lexReader.getDocument().addDocumentListener(new DocumentListener() {
            /**
             * 1. add e.length to position in tokensPos that begins from e.offset
             * 2. reset lexical analyzer from before-last token
             * 3. read lex tokensPos until they are equal
             */
            public void insertUpdate(DocumentEvent e) {
                synchronized(work) {
                    work.add(e);
                }
                if (!pause) tthis.interrupt();
            };

            public void removeUpdate(DocumentEvent e) {
                synchronized(work) {
                    work.add(e);
                }
                if (!pause) tthis.interrupt();
            }

            public void changedUpdate(DocumentEvent e) {}
            
        });
        running = true;
        this.start();
    }
    
    public void run() {
        boolean shouldTry = true;
        while (running) {
            while (pause) {
                try {sleep(0xffffff); }
                catch(InterruptedException ex) {}
            }
            if (!shouldTry) {
                try {sleep(0xffffff); }
                catch(InterruptedException ex) {}
            }
            synchronized(work) {
                try {
                    lex.reset(lexReader,0,0,0);
                    lexReader.seek(0);
                } catch(IOException ex) {}
                for (Iterator wit = work.iterator(); wit.hasNext();) {
                    DocumentEvent e = (DocumentEvent)wit.next();
                    int len = (e.getType() == DocumentEvent.EventType.INSERT) ?
                        e.getLength() : -e.getLength();
                    int bl = updateTokenPositions(e.getOffset(),len);
                    try {
                        if (bl >= 0) {
                            lex.reset(lexReader, 0, bl, 0);
                            lexReader.seek(bl);
                        }                    
                        IToken t = lex.getSymbol();
                        while (t.getType() != IToken.TEOF) {
                            if (tokensPos.contains(t.getCharBegin())) {
                                int tid = tokenTypes.get(t.getCharBegin());
                                // ak je token ZA kurzorom v dokumente a tokeny su
                                // zhodne
                                if ((t.getCharBegin() > e.getOffset()) 
                                        && (tid == t.getID())) {
                                    break;
                                }
                                else tokensPos.remove(t.getCharBegin());
                            }
                            removeTokens(t.getCharBegin(),t.getCharEnd());
                            tokensPos.add(Integer.valueOf(t.getCharBegin()));
                            tokenTypes.put(Integer.valueOf(t.getCharBegin()), 
                                    Integer.valueOf(t.getID()));
                            
                            len = t.getCharEnd() - t.getCharBegin();
                            SimpleAttributeSet style = (SimpleAttributeSet)
                                        styles.get(t.getType());
                            if (style == null)
                                style = (SimpleAttributeSet)
                                        styles.get(IToken.ERROR);
                            recolorWork.add(new RecolorEvent(style,t.getCharBegin(),len));                                
                            bl = t.getCharBegin(); // before last token on the end of while cycle
                            t = lex.getSymbol();
                        }
                        if ((e.getType() == DocumentEvent.EventType.REMOVE) 
                                && (t.getType() == IToken.TEOF)) {
                            // remove rest tokens in tokenPos and tokenTypes
                            try { removeTokens(bl+1,(Integer)tokensPos.last()+1);}
                            catch(NoSuchElementException exx) {}
                        }
                    } catch (IOException xe) {}
                    wit.remove();
                }
                for (Iterator cit = recolorWork.iterator(); cit.hasNext();) {
                    RecolorEvent r = (RecolorEvent)cit.next();
                    r.reColor();
                    cit.remove();
                }
                
                shouldTry = !work.isEmpty();
            }
        }
    }

    /**
     * Add to tokensPos from position "from" length "length". Length can be
     * both positive or negative.
     * @return before-last(from) token position if change was made, -1 otherwise
     */
    private int updateTokenPositions(int from, int length) {
        int p = -1;
        int beforeLast = -1;
        // search for nearest "from"
        for (Iterator i = tokensPos.iterator();i.hasNext();) {
            p = (Integer)i.next();
            if (p >= from) break;
            beforeLast = p;
        }
        // treeset contains no elements or from is last position
        if ((p == -1) || (p < from)) return beforeLast;
        SortedSet s = tokensPos.tailSet(p);
        Integer[] poss = (Integer[])s.toArray(new Integer[0]);
        Vector types = new Vector();
        for (int i = 0; i < poss.length; i++) {
            tokensPos.remove(poss[i]);
            types.add(tokenTypes.get(poss[i]));
            tokenTypes.remove(poss[i]);
        }
        for (int i = 0; i < poss.length; i++) {
            int l = poss[i] + length;
            if (l < 0) continue;
            tokensPos.add(l);
            tokenTypes.put(l, (Integer)types.elementAt(i));
        }
        return beforeLast;
    }

    
    /**
     * Method remove tokens between from-to. Used when typed char "joines"
     * many tokens together and therefore creates one big token. Original smaller
     * tokens has to be therefore removed.
     */
    private void removeTokens(int from, int to) {
        int p = -1;
        // search for nearest "from"
        for (Iterator i = tokensPos.iterator();i.hasNext();) {
            p = (Integer)i.next();
            if ((p >= from) && (p < to)) {
                tokenTypes.remove(p);
                i.remove();
            }
            if (p >= to) break; // no more tokens in range
        }
    }
    
//    public void printTokens() {
//        int j = 1;
//        for (Iterator i = tokensPos.iterator(); i.hasNext();j++) {
//            int pos = (Integer)i.next();
//            System.out.println(j + ". =("+pos + ") " + tokenTypes.get(pos));
//        }
//    }
    
    public void stopRun() {
        running = false;
    }
    
    public void pauseRun() {
        pause = true;
    }
    public void continueRun() {
        pause = false;
        this.interrupt();
    }
}
