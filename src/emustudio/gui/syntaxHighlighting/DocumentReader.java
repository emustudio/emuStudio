/*
 * DocumentReader.java
 *
 * Created on 7.2.2008, 9:59:19
 * hold to: KISS, YAGNI
 *
 * It is used for syntax highlighting
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.gui.syntaxHighlighting;

import java.io.IOException;
import java.io.Reader;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Reader of the source code. It is used by syntax highlighter.
 *
 * @author vbmacher
 */
public class DocumentReader extends Reader {
    /* pouziva sa na oznacenie miesta v dokumente, v ktorom je mozne bezpecne
     * resetovat lex. analyzator  */
    private long mark = -1;
    protected long position = 0;
    protected volatile Document document;

    /**
     * Create an instance of the document reader.
     *
     * @param document the source code document
     */
    public DocumentReader(Document document) {
        this.document = document;
    }

    /**
     * Modifying the document while the reader is working is like
     * pulling the rug out from under the reader.  Alerting the
     * reader with this method (in a nice thread safe way, this
     * should not be called at the same time as a read) allows
     * the reader to compensate.
     *
     * @param position the position
     * @param adjustment the adjustment
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

    /**
     * Get the position within the source code, where the reader points.
     * 
     * @return the position within the source code
     */
    public long getPosition() { return position; }
    
    /**
     * Read a single character.
     *
     * @return the character or -1 if the end of the document has been reached.
     */
    @Override
    public int read(){
        if (position < document.getLength()){
            try {
                char c = document.getText((int)position, 1).charAt(0);
                position++;
                return c;
            } catch (BadLocationException x){
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * Read and fill the buffer.
     * This method will always fill the buffer unless the end of the document is reached.
     *
     * @param cbuf the buffer to fill.
     * @return the number of characters read or -1 if no more characters are available in the document.
     */
    @Override
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }


    /**
     * Read and fill the buffer.
     * This method will always fill the buffer unless the end of the document is reached.
     *
     * @param cbuf the buffer to fill.
     * @param off offset into the buffer to begin the fill.
     * @param len maximum number of characters to put in the buffer.
     * @return the number of characters read or -1 if no more characters are available in the document.
     */
    @Override
    public int read(char[] cbuf, int off, int len){
        if (position < document.getLength()){
            int length = len;
            if (position + length >= document.getLength()){
                length = document.getLength() - (int)position;
            }
            if (off + length >= cbuf.length){
                length = cbuf.length - off;
            }
            try {
                String s = document.getText((int)position, length);
                position += length;
                for (int i=0; i<length; i++){
                    cbuf[off+i] = s.charAt(i);
                }
                return length;
            } catch (BadLocationException x){
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * Determine if the reader is ready or not.
     *
     * @return true
     */
    @Override
    public boolean ready() {
        return true;
    }

    /**
     * Reset this reader to the last mark, or the beginning of the document if a mark has not been set.
     */
    @Override
    public void reset(){
        if (mark == -1){
            position = 0;
        } else {
            position = mark;
        }
        mark = -1;
    }

    /**
     * Skip characters of input.
     * This method will always skip the maximum number of characters unless
     * the end of the file is reached.
     *
     * @param n number of characters to skip.
     * @return the actual number of characters skipped.
     */
    @Override
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

    /**
     * Seek to the given position in the document.
     *
     * @param n the offset to which to seek.
     */
    public void seek(long n){
        if (n <= document.getLength()){
            position = n;
        } else {
            position = document.getLength();
        }
    }
    
    /**
     * Has no effect.  This reader can be used even after
     * it has been closed.
     */
    @Override
    public void close() {
    }
    
}
