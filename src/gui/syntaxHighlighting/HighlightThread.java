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
public class HighlightThread extends Thread{
    private final String threadName = "highlightThread";

    private SortedSet<Integer> tokensPos = Collections.synchronizedSortedSet(new TreeSet<Integer>());
    private Hashtable<Integer,IToken> tokens = new Hashtable<Integer,IToken>();
  //  private Hashtable<Integer,Integer> tokenLengths = new Hashtable<Integer,Integer>();
    
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
      		// following method is thread safe !!
            doc.setCharacterAttributes(offset,length,style,true);
        }
    }
    
    public HighlightThread(ILexer lex, DocumentReader lexReader, Hashtable<?, ?> styles) {
        this.lex = lex;
        this.doc = (DefaultStyledDocument)lexReader.getDocument();   
        this.setName(threadName);
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
                }
                try { if (!pause) tthis.interrupt(); }
                catch(Exception ex) {}
            };

            public void removeUpdate(DocumentEvent e) {
                synchronized(lock) {
                    work.add(e);
                }
                try { if (!pause) tthis.interrupt(); }
                catch(Exception ex) {}
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
                try { //printTokens(); 
                	sleep(0xffffff);
                }
                catch(InterruptedException ex) {}
            }
            synchronized(lock) {
                shouldTry = !work.isEmpty();
            }
            if (!shouldTry) {
                try { //printTokens();
                	sleep(0xffffff);
                }
                catch(InterruptedException ex) {}
            }
            synchronized(lock) {
                if (work.isEmpty()) continue;
                e = work.elementAt(0);
                work.removeElementAt(0);
            }
//    		System.out.println("Highlight.run.Entering(1)");
            synchronized(doclock) {
  //      		System.out.println("    Highlight.run.IN(1)");
                lexReader.seek(0);
                lex.reset(lexReader,0,0,0);
    //    		System.out.println("    Highlight.run.Exiting(1)");
            }
//    		System.out.println("Highlight.run.OUT(1)");
            int len = (e.getType() == DocumentEvent.EventType.INSERT) ?
                e.getLength() : -e.getLength();
            int bl = updatePositions(e.getOffset(),len);
//            System.out.print("Seek at:" + bl);
            try {
                if (bl >= 0) {
  //          		System.out.println("Highlight.run.Entering(2)");
                    synchronized(doclock) {
    //            		System.out.println("    Highlight.run.IN(2)");
                        lexReader.seek(bl);
                        lex.reset(lexReader,0, bl, 0);
      //          		System.out.println("    Highlight.run.Exiting(2)");
                    }
        //    		System.out.println("Highlight.run.OUT(2)");
                }
                IToken t;
        	//	System.out.println("Highlight.run.Entering(3)");
                synchronized(doclock) {
            	//	System.out.println("    Highlight.run.IN(3)");
                    t = lex.getSymbol();
            		//System.out.println("    Highlight.run.Exiting(3)");
                }
//        		System.out.println("Highlight.run.OUT(3)");
//                System.out.println("; <" + t.getID() + "> [" + t.getOffset() + ";" + t.getLength() + "]");
                while (t.getType() != IToken.TEOF) {
                    if (tokensPos.contains(t.getOffset())) {
                    	IToken tok = tokens.get(t.getOffset());
                        int tid = tok.getType();
                        int tlen = tok.getLength();
  //                  	System.out.print("    found: ("+(t.getOffset() > e.getOffset()
    //                			&& (tid == t.getID())) + ")");
                        // ak je token ZA kurzorom v dokumente a tokeny su
                        // zhodne (rovnake ID a dlzku) tak uz viac netreba
                        // skenovat
                        if ((t.getOffset() > e.getOffset())
                        		&& (tid == t.getID()) && (tlen == t.getLength())) {
//                        	System.out.println("the same: " + t.getText());
                        	break;
                        }
                        else {
//                        	System.out.println("not equal: <" + t.getID() + "> != saved<" + tid + ">, "
  //                      			+ tlen + " != " + t.getLength());
                        	// inak odstranujem token
                        	tokensPos.remove(t.getOffset());
                        	tokens.remove(t.getOffset());
                        }
                    }
//                    System.out.println("Removing tokens from: " + t.getOffset() 
  //                  		+ " to " + (t.getOffset() + t.getLength()));
                    removeTokens(t.getOffset(),t.getOffset() + t.getLength());
                    tokensPos.add(t.getOffset());
                    tokens.put(t.getOffset(), t);

                    SimpleAttributeSet style = (SimpleAttributeSet)
                                styles.get(t.getType());
                    if (style == null)
                        style = (SimpleAttributeSet)
                                styles.get(IToken.ERROR);
                    recolorWork.add(new RecolorEvent(style,t.getOffset(),t.getLength()));                                
                    bl = t.getOffset(); // before last token on the end of while cycle
                    synchronized(doclock) {
                        t = lex.getSymbol();
                    }
                }
                if ((e.getType() == DocumentEvent.EventType.REMOVE) 
                        && (t.getType() == IToken.TEOF)) {
                    // remove rest tokens in tokenPos and tokenTypes and tokenLengths
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
     * Method remove tokens from offset "from" and length "length". Length
     * can be both positive or negative.
     * @return first "before" token in initial lexical state
     *         -1 otherwise
     */
    private int updatePositions(int from, int length) {
        int pos = -1;
        int i;
        int old_from = from;

        try {
        	do {
        		pos = tokensPos.subSet(0, from).last();
        		from = pos;
        	} while (!tokens.get(pos).isInitialLexicalState());
        }
        catch(NoSuchElementException e) { return -1; }
        
        from = old_from;
        // Now I'm going to "adjust" tokens - move
        // tokens positions to appropriate offset:
        //   new_position = old_position + length 
        
        // tokeny od lex-pozicie
        SortedSet<Integer> s = tokensPos.tailSet(from);
        Integer[] poss = s.toArray(new Integer[0]);
        
        Vector<IToken> saved_tokens = new Vector<IToken>();
        
        // remove old positions saving types and lengths
        for (i = 0; i < poss.length; i++) {
//        	System.out.println("   " + i + ". R(" +tokenTypes.get(poss[i]) + ") "+ poss[i]);      	
            tokensPos.remove(poss[i]);
            saved_tokens.add(tokens.get(poss[i]));
            tokens.remove(poss[i]);
        }
        
        // add new positions with saved types and lengths
        for (i = 0; i < poss.length; i++) {
            int l = poss[i] + length;
            if (l < 0) continue;
//        	System.out.println("   " + i + ". A(" +types.elementAt(i) + ") "+ l);
            tokensPos.add(l);
            tokens.put(l, saved_tokens.elementAt(i));
        }
    	return pos;
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
            	tokens.remove(p);
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
