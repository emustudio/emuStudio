/*
 * KISS, YAGNI, DRY
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

import java.io.IOException;
import java.io.Reader;
import javax.swing.text.BadLocationException;

/**
 * Reader of the source code. It is used by syntax highlighter.
 */
public class DocumentReader extends Reader {
    private final long mark = -1;
    private long position = 0;

    protected HighLightedDocument document;

    public DocumentReader(HighLightedDocument document) {
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
    public synchronized void update(int position, int adjustment) {
        try {
            document.readLock();

            if (position < this.position) {
                if (this.position < position - adjustment) {
                    this.position = position;
                } else {
                    this.position += adjustment;
                }
            }
        } finally {
            document.readUnlock();
        }
    }

    public long getPosition() { return position; }

    @Override
    public int read() {
        document.readLock();
        try {
            if (position < document.getLength()) {
                try {
                    char c = document.getText((int) position, 1).charAt(0);
                    position++;
                    return c;
                } catch (BadLocationException x) {
                    return -1;
                }
            } else {
                return -1;
            }
        } finally {
            document.readUnlock();
        }
    }

    @Override
    public synchronized int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public int read(char[] cbuf, int off, int len) {
        document.readLock();
        try {
            int docLen = document.getLength();
            if (position < docLen) {
                int length = len;
                if (position + length >= docLen) {
                    length = docLen - (int) position;
                }
                if (off + length >= cbuf.length) {
                    length = cbuf.length - off;
                }
                try {
                    String s = document.getText((int) position, length);
                    position += length;
                    for (int i = 0; i < length; i++) {
                        cbuf[off + i] = s.charAt(i);
                    }
                    return length;
                } catch (BadLocationException x) {
                    return -1;
                }
            } else {
                return -1;
            }
        } finally {
            document.readUnlock();
        }
    }

    @Override
    public boolean ready() {
        return true;
    }

    @Override
    public void reset() {
        if (mark == -1){
            position = 0;
        } else {
            position = mark;
        }
    }

    @Override
    public long skip(long n){
        document.readLock();
        int docLen = 0;
        try {
            docLen = document.getLength();
        } finally {
            document.readUnlock();
        }
        if (position + n <= docLen){
            position += n;
            return n;
        } else {
            long oldPos = position;
            position = docLen;
            return (docLen - oldPos);
        }
    }

    public void seek(long n){
        document.readLock();
        int docLen = 0;
        try {
            docLen = document.getLength();
        } finally {
            document.readUnlock();
        }
        if (n <= docLen){
            position = n;
        } else {
            position = docLen;
        }
    }

    @Override
    public void close() {
    }

}
