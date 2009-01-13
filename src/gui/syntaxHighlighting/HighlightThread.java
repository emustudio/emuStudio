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
    private SortedSet<Integer> tokensPos = Collections.synchronizedSortedSet(new TreeSet<Integer>());
    private Hashtable<Integer,Integer> tokenTypes = new Hashtable<Integer,Integer>();
    
    private ILexer lex;
    private DocumentReader lexReader;
    private DefaultStyledDocument doc;
    
    public static Object doclock = new Object();
    private Object lock = new Object();
    
    private volatile boolean running = false;
    private volatile boolean pause = false;
    private volatile Vector<DocumentEvent> work = new Vector<DocumentEvent>();
    private HighlightThread tthis;
    private Hashtable<?, ?> styles;
    private volatile Vector<RecolorEvent> recolorWork = new Vector<RecolorEvent>();

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
            synchronized(doclock) {
                doc.setCharacterAttributes(offset,length,style,true);
            }
        }
    }
    
    public HighlightThread(ILexer lex, DocumentReader lexReader, Hashtable<?, ?> styles) {
        this.lex = lex;
        this.doc = (DefaultStyledDocument)lexReader.getDocument();   
        this.setName("highlightThread");
        this.lexReader = lexReader;
        this.styles = styles;
        this.tthis = this;
        
        doc.addDocumentListener(new DocumentListener() {
            /**
             * 1. add e.length to position in tokensPos that begins from e.offset
             * 2. reset lexical analyzer from before-last token
             * 3. read lex tokensPos until they are equal
             */
            public void insertUpdate(DocumentEvent e) {
                synchronized(lock) {
                    work.add(e);
                    try {
                        if (!pause) tthis.interrupt();
                    } catch(Exception ex) {}
                }
            };

            public void removeUpdate(DocumentEvent e) {
                synchronized(lock) {
                    work.add(e);
                    try {
                        if (!pause) tthis.interrupt();
                    } catch(Exception ex) {}
                }
            }

            public void changedUpdate(DocumentEvent e) {}
            
        });
        running = true;
        this.start();
    }
    
    public void run() {
        boolean shouldTry = true;
        DocumentEvent e;
        while (running) {
            while (pause) {
                try {sleep(0xffffff); }
                catch(InterruptedException ex) {}
            }
            synchronized(lock) {
                shouldTry = !work.isEmpty();
            }
            if (!shouldTry) {
                try { sleep(0xffffff); }
                catch(InterruptedException ex) {}
            }
            synchronized(lock) {
                if (work.isEmpty()) continue;
                e = work.elementAt(0);
                work.removeElementAt(0);
            }
            try {
                synchronized(doclock) {
                    lex.reset(lexReader,0,0,0);
                    lexReader.seek(0);
                }
            } catch(IOException ex) {}
            int len = (e.getType() == DocumentEvent.EventType.INSERT) ?
                e.getLength() : -e.getLength();
            int bl = updateTokenPositions(e.getOffset(),len);
            try {
                if (bl >= 0) {
                    synchronized(doclock) {
                        lex.reset(lexReader, 0, bl, 0);
                        lexReader.seek(bl);
                    }
                }
                IToken t;
                synchronized(doclock) {
                    t = lex.getSymbol();
                }
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
                    tokensPos.add(t.getCharBegin());
                    tokenTypes.put(t.getCharBegin(), t.getID());

                    len = t.getCharEnd() - t.getCharBegin();
                    SimpleAttributeSet style = (SimpleAttributeSet)
                                styles.get(t.getType());
                    if (style == null)
                        style = (SimpleAttributeSet)
                                styles.get(IToken.ERROR);
                    recolorWork.add(new RecolorEvent(style,t.getCharBegin(),len));                                
                    bl = t.getCharBegin(); // before last token on the end of while cycle
                    synchronized(doclock) {
                        t = lex.getSymbol();
                    }
                }
                if ((e.getType() == DocumentEvent.EventType.REMOVE) 
                        && (t.getType() == IToken.TEOF)) {
                    // remove rest tokens in tokenPos and tokenTypes
                    try { removeTokens(bl+1,tokensPos.last()+1);}
                    catch(NoSuchElementException exx) {}
                }
            } catch (IOException xe) {}
            for (Iterator<RecolorEvent> cit = recolorWork.iterator(); cit.hasNext();) {
                RecolorEvent r = cit.next();
                try { r.reColor();}
                catch(Error exx) {}
                cit.remove();
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
        for (Iterator<Integer> i = tokensPos.iterator();i.hasNext();) {
            p = i.next();
            if (p >= from) break;
            beforeLast = p;
        }
        // treeset contains no elements or from is last position
        if ((p == -1) || (p < from)) return beforeLast;
        SortedSet<Integer> s = tokensPos.tailSet(p);
        Integer[] poss = s.toArray(new Integer[0]);
        Vector<Integer> types = new Vector<Integer>();
        for (int i = 0; i < poss.length; i++) {
            tokensPos.remove(poss[i]);
            types.add(tokenTypes.get(poss[i]));
            tokenTypes.remove(poss[i]);
        }
        for (int i = 0; i < poss.length; i++) {
            int l = poss[i] + length;
            if (l < 0) continue;
            tokensPos.add(l);
            tokenTypes.put(l, types.elementAt(i));
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
        for (Iterator<Integer> i = tokensPos.iterator();i.hasNext();) {
            p = i.next();
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
