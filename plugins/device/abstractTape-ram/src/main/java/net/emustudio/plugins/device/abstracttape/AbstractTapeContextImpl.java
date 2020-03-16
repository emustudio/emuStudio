/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.abstracttape;

import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Tape used by abstract machines.
 * <p>
 * Tape options are:
 * - (R)ead
 * - (R)ead (W)rite
 * - (W)rite
 * - direction:
 * - only left
 * - only right
 * - both
 * <p>
 * The CPU must assign all the details to this tape using the tape context.
 * <p>
 * By default, the tape is unbounded. However, it is possible to change.
 */
public class AbstractTapeContextImpl implements AbstractTapeContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTapeContextImpl.class);
    private final List<String> tape; // tape is an array of strings
    private int currentPosition; // actual tape position
    private boolean bounded; // tape is bounded form the left?
    private boolean editable; // if tape is editable by user
    private TapeListener listener;
    private boolean highlightCurrentPosition;
    private boolean clearAtReset = true;
    private final AbstractTape abst;
    private Writer outw;
    private boolean displayRowNumbers = false;

    public interface TapeListener extends EventListener {

        void tapeChanged();
    }

    AbstractTapeContextImpl(AbstractTape abst) {
        this.abst = abst;
        listener = null;
        tape = new ArrayList<>();
        currentPosition = 0;
        bounded = false;
        editable = true;
        highlightCurrentPosition = true;
    }

    @Override
    public void setTitle(String title) {
        abst.setGUITitle(title);
    }

    @Override
    public boolean showPositions() {
        return displayRowNumbers;
    }

    @Override
    public void setShowPositions(boolean showPositions) {
        this.displayRowNumbers = showPositions;
        fireChange();
    }

    /**
     * Clears tape and set head position to 0
     */
    @Override
    public void clear() {
        tape.clear();
        currentPosition = 0;
        fireChange();
    }

    void reset() {
        currentPosition = 0;
        if (clearAtReset) {
            clear();
        }
        fireChange();
    }

    @Override
    public void setBounded(boolean bounded) {
        this.bounded = bounded;
    }

    @Override
    public boolean isBounded() {
        return bounded;
    }

    @Override
    public boolean moveLeft() {
        if (currentPosition > 0) {
            currentPosition--;
            fireChange();
            return true;
        } else if (!bounded) {
            currentPosition = 0;
            tape.add(0, "");
            fireChange();
            return true;
        }
        return false;
    }

    @Override
    public void moveRight() {
        currentPosition++;
        if (currentPosition >= tape.size()) {
            tape.add("");
        }
        fireChange();
    }

    public void addSymbolFirst(String symbol) {
        if (bounded) {
            return;
        }
        if (symbol == null) {
            symbol = "";
        }
        tape.add(0, symbol);
        writeSymbol(0, symbol);
        currentPosition++;
        fireChange();
    }

    public void addSymbolLast(String symbol) {
        if (symbol == null) {
            symbol = "";
        }
        tape.add(symbol);
        writeSymbol(tape.size() - 1, symbol);
        fireChange();
    }

    public void removeSymbol(int pos) {
        if (pos >= tape.size()) {
            return;
        }
        tape.remove(pos);
        writeSymbol(pos, "");

        if (this.currentPosition >= pos) {
            this.currentPosition--;
        }
        fireChange();
    }

    public void editSymbol(int pos, String symbol) {
        if (pos >= tape.size()) {
            return;
        }
        if (symbol == null) {
            symbol = "";
        }
        tape.set(pos, symbol);
        writeSymbol(pos, symbol);
        fireChange();
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean getEditable() {
        return editable;
    }

    @Override
    public String getSymbolAt(int pos) {
        if (pos >= tape.size() || (pos < 0)) {
            return "";
        }
        return tape.get(pos);
    }

    @Override
    public void setSymbolAt(int pos, String symbol) {
        if (pos >= tape.size()) {
            while (pos > tape.size()) {
                tape.add("");
            }
            tape.add(symbol);
            writeSymbol(pos, symbol);
        } else if (pos >= 0) {
            tape.set(pos, symbol);
            writeSymbol(pos, symbol);
        }
        fireChange();
    }

    @Override
    public void setHighlightHeadPosition(boolean visible) {
        highlightCurrentPosition = visible;
    }

    @Override
    public void setClearAtReset(boolean clear) {
        this.clearAtReset = clear;
    }

    public boolean highlightCurrentPosition() {
        return highlightCurrentPosition;
    }

    @Override
    public int getSize() {
        return tape.size();
    }

    @Override
    public int getHeadPosition() {
        return currentPosition;
    }

    @Override
    public boolean isEmpty() {
        return tape.isEmpty();
    }

    @Override
    public String readData() {
        if (currentPosition >= tape.size() || (currentPosition < 0)) {
            return "";
        }
        return tape.get(currentPosition);
    }

    @Override
    public void writeData(String data) {
        if (currentPosition >= tape.size()) {
            tape.add(currentPosition, data);
        } else {
            tape.set(currentPosition, data);
        }
        writeSymbol(currentPosition, data);
        fireChange();
    }

    @Override
    public Class<String> getDataType() {
        return String.class;
    }

    private void writeSymbol(int position, String symbol) {
        if (outw != null) {
            try {
                outw.write(position + " ");
                outw.write(symbol + "\n");
                outw.flush();
            } catch (IOException e) {
                LOGGER.error("Could not write to the output file", e);
            }
        }
    }

    private String createValidFileName(String str) {
        return str.trim().toLowerCase().replaceAll("[*.#%&\\s+!~/?<>,|{}\\[\\]\\\\\"'`=]", "_");
    }

    /**
     * Set verbose mode. If verbose mode is set, the output
     * is redirected also to a file.
     *
     * @param verbose set/reset/unset verbose mode
     */
    void setVerbose(boolean verbose) {
        if (outw != null) {
            try {
                outw.close();
            } catch (IOException e) {
                LOGGER.error("Could not close output file", e);
            }
            outw = null;
        }
        if (verbose) {
            String fileName = createValidFileName(abst.getTitle().trim());
            LOGGER.info("Being verbose. Writing to file:" + fileName + ".out");
            File f = new File(fileName + ".out");
            try {
                outw = new FileWriter(f);
            } catch (IOException e) {
                LOGGER.error("Could not create FileWriter", e);
            }
        }
    }

    public void setListener(TapeListener listener) {
        this.listener = listener;
    }

    private void fireChange() {
        if (listener != null) {
            listener.tapeChanged();
        }
    }
}
