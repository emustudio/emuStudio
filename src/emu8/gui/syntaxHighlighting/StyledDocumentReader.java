/*
 * StyledDocumentReader.java
 *
 * Created on 7.2.2008, 9:59:19
 * hold to: KISS, YAGNI
 *
 * It is used for syntax highlighting 
 */

package emu8.gui.syntaxHighlighting;

import emu8.gui.utils.DocumentReader;
import javax.swing.text.Document;

/**
 *
 * @author vbmacher
 */
public class StyledDocumentReader extends DocumentReader {
    /* pouziva sa na oznacenie miesta v dokumente, v ktorom je mozne bezpecne
     * resetovat lex. analyzator  */
    private long mark = -1;

    public StyledDocumentReader(Document document) {
        super(document);
    }

    public boolean ready() { return true; }    
    public void mark(int readAheadLimit) { mark = position; }
    public boolean markSupported() { return true; }

    /**
     * Modifying the document while the reader is working is like
     * pulling the rug out from under the reader.  Alerting the
     * reader with this method (in a nice thread safe way, this
     * should not be called at the same time as a read) allows
     * the reader to compensate.
     */
    public void update(int position, int adjustment){
        if (position < this.position){
            if (this.position < position - adjustment){
                this.position = position;
            } else {
                this.position += adjustment;
            }
        }
    }

    /* Seek to the given position in the document.  */
    public void seek(long n){
        if (n <= document.getLength())
            position = n;
        else position = document.getLength();
    }

    public void reset(){
        if (mark == -1) position = 0;
        else position = mark;
        mark = -1;
    }

}
