/*
 * DocumentReader.java
 *
 * Created on 14.2.2008, 18:09:32
 * hold to: KISS, YAGNI
 * 
 * It is used for compiling - lexer reads text
 *
 */

package emu8.gui.utils;

import java.io.Reader;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 *
 * @author vbmacher
 */
public class DocumentReader extends Reader {
    protected long position = 0;
    protected Document document;

    public DocumentReader(Document document) {
        this.document = document;
    }
    
    public int read(){
        if (position < document.getLength()){
            try {
                char c = document.getText((int)position, 1).charAt(0);
                position++;
                return c;
            } catch (BadLocationException x) {
                return -1;
            }
        } else return -1;
    }

    public int read(char[] cbuf, int off, int len){
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
    }

    /* Skip characters of input */
    public long skip(long n){
        if (position + n <= document.getLength()){
            position += n;
            return n;
        } else {
            long oldPos = position;
            position = document.getLength();
            return (document.getLength() - oldPos);
        }
    }
   
    public void close() {}

}
