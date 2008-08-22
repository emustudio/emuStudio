/*
 * DocumentReader.java
 *
 * Created on 7.2.2008, 9:59:19
 * hold to: KISS, YAGNI
 *
 * It is used for syntax highlighting 
 */

package gui.syntaxHighlighting;

import java.io.Reader;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 *
 * @author vbmacher
 */
public class DocumentReader extends Reader {
    /* pouziva sa na oznacenie miesta v dokumente, v ktorom je mozne bezpecne
     * resetovat lex. analyzator  */
    private long mark = -1;
    protected long position = 0;
    protected volatile Document document;

    public DocumentReader(Document document) {
        this.document = document;
    }

    public long getPosition() { return position; }
    
    public int read(){
//        synchronized(document) {
            if (position < document.getLength()){
                try {
                    char c = document.getText((int)position, 1).charAt(0);
                    position++;
                    return c;
                } catch (BadLocationException x) {
                    return -1;
                }
            } else return -1;
  //      }
    }

    public int read(char[] cbuf, int off, int len) {
    //    synchronized(document) {
            if (position < document.getLength()){
                int length = len;
                if (position + length >= document.getLength())
                    length = document.getLength() - (int)position;
                if (off + length >= cbuf.length)
                    length = cbuf.length - off;
                try {
                    String s = document.getText((int)position, length);
                    position += length;
                    for (int i=0; i<length; i++)
                        cbuf[off+i] = s.charAt(i);
                    return length;
                } catch (BadLocationException x){
                    return -1;
                }
            } else return -1;
      //  }
    }

    /* Skip characters of input */
    public long skip(long n) {
        //synchronized(document) {
            if (position + n <= document.getLength()){
                position += n;
                return n;
            } else {
                long oldPos = position;
                position = document.getLength();
                return (document.getLength() - oldPos);
            }
    //    }
    }
   
    public void close() {}
    
    public boolean ready() { return true; }    
    public void mark(int readAheadLimit) { 
        mark = position;
    }
    
    public boolean markSupported() { return true; }

    /* Seek to the given position in the document.  */
    public void seek(long n) {
      //  synchronized(document) {
            if (n <= document.getLength())
                position = n;
            else position = document.getLength();
        //}
    }

    public void reset(){
        if (mark == -1) position = 0;
        else position = mark;
        mark = -1;
    }

    public Document getDocument() {
        return document;
    }
}
