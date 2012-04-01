/**
 * TapeContext.java
 *
 *   KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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
 *
 */
package abstracttape.impl;

import interfaces.C50E67F515A7C87A67947F8FB0F82558196BE0AC7;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;

/**
 * Moznosti pasky:
 * ---------------
 *   - R
 *   - RW
 *   - W
 *   - smer: len dolava
 *   - smer: len doprava
 *   - smer: dolava aj doprava
 *   Staci, ak tento vyznam paske priradi CPU.. Nemusim to riesit
 *   tu v paske.
 *
 *   Treba vsak riesit ohranicenost - zabezpecit, aby sa paska vedela
 *   rozmahat dolava, aj doprava ak je neohranicena..
 *   Zabezpecenie ohranicenosti (len z jednej strany) musi byt riesene
 *   tu v paske. Defaultne je paska neohranicena.
 *
 * @author vbmacher
 */
public class TapeContext implements C50E67F515A7C87A67947F8FB0F82558196BE0AC7 {

    private ArrayList<String> tape; // tape is an array of strings
    private int pos; // actual tape position
    private boolean bounded; // tape is bounded form the left?
    private boolean editable; // if tape is editable by user
    private TapeListener listener;
    private EventObject changeEvent;
    private boolean showPos;
    private boolean clearAtReset = true;
    private AbstractTape abst;

    public TapeContext(AbstractTape abst) {
        this.abst = abst;
        changeEvent = new EventObject(this);
        listener = null;
        tape = new ArrayList<String>();
        pos = 0;
        bounded = false;
        editable = true;
        showPos = true;
    }

    @Override
    public void setTitle(String title) {
        abst.setGUITitle(title);
    }

    public interface TapeListener extends EventListener {

        public void tapeChanged(EventObject evt);
    }

    @Override
    public Class<?> getDataType() {
        return String.class;
    }

    /**
     * Clears tape and set head position to 0
     */
    @Override
    public void clear() {
        tape.clear();
        pos = 0;
        fireChange();
    }

    public void reset() {
        pos = 0;
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
        if (pos > 0) {
            pos--;
            fireChange();
            return true;
        } else if (bounded == false) {
            pos = 0;
            tape.add(0, "");
            fireChange();
            return true;
        }
        return false;
    }

    @Override
    public void moveRight() {
        pos++;
        if (pos >= tape.size()) {
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
        pos++;
        fireChange();
    }

    public void addSymbolLast(String symbol) {
        if (symbol == null) {
            symbol = "";
        }
        tape.add(symbol);
        fireChange();
    }

    public void removeSymbol(int pos) {
        if (pos >= tape.size()) {
            return;
        }
        tape.remove(pos);
        if (this.pos >= pos) {
            this.pos--;
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
        fireChange();
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean getEditable() {
        return editable;
    }

    public int getPos() {
        return pos;
    }

    /**
     * Used by GUI, too - TapeDialog.
     * @param pos
     * @return
     */
    @Override
    public String getSymbolAt(int pos) {
        if (pos >= tape.size() || (pos < 0)) {
            return "";
        }
        return tape.get(pos);
    }

    /**
     *
     * @param pos HAS TO BE > 0
     * @param symbol
     */
    @Override
    public void setSymbolAt(int pos, String symbol) {
        if (pos >= tape.size()) {
            while (pos > tape.size()) {
                tape.add("");
            }
            tape.add(symbol);
        } else if ((pos < tape.size()) && (pos >= 0)) {
            tape.set(pos, symbol);
        }
        fireChange();
    }

    @Override
    public void setPosVisible(boolean visible) {
        showPos = visible;
    }

    @Override
    public void setClearAtReset(boolean clear) {
        this.clearAtReset = clear;
    }

    /**
     * Used by GUI.
     * @return
     */
    public boolean getPosVisible() {
        return showPos;
    }

    public int getSize() {
        return tape.size();
    }

    @Override
    public Object read() {
        if (pos >= tape.size() || (pos < 0)) {
            return "";
        }
        return tape.get(pos);
    }

    @Override
    public void write(Object val) {
        if (pos >= tape.size()) {
            tape.add(pos, val.toString());
        } else {
            tape.set(pos, val.toString());
        }
        fireChange();
    }

    @Override
    public String getID() {
        return "abstract-tape-context";
    }

    public void setListener(TapeListener listener) {
        this.listener = listener;
    }

    private void fireChange() {
        if (listener != null) {
            listener.tapeChanged(changeEvent);
        }
    }
}
