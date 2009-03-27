/**
 * TapeContext.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *   KISS, YAGNI
 *   
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
 *   
 */
package abstracttape.impl;

import interfaces.IAbstractTapeContext;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;

public class TapeContext implements IAbstractTapeContext {
	private ArrayList<String> tape; // tape is an array of strings
	private int pos; // actual tape position
	private boolean bounded; // tape is bounded form the left? 
	private boolean editable; // if tape is editable by user
	private TapeListener listener;
    private EventObject changeEvent;
    private boolean showPos;
	
	public interface TapeListener extends EventListener {
		public void tapeChanged(EventObject evt);
	}
	
	public TapeContext() {
		changeEvent = new EventObject(this);
		listener = null;
		tape = new ArrayList<String>();
		pos = 0;
		bounded = false;
		editable = true;
		showPos = true;
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

	@Override
	public void setBounded(boolean bounded) {
		this.bounded = bounded;
	}
	
	@Override
	public boolean isBounded() { return bounded; }
	
	@Override
	public boolean moveLeft() {
		if (pos > 0) {
			pos--;
			fireChange();
			return true;
		}
		else if (bounded == false) {
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
		if (pos >= tape.size())
			tape.add("");
		fireChange();
    }
	
	public void addSymbolFirst(String symbol) {
		if (bounded) return;
		if (symbol == null) symbol = "";
		tape.add(0,symbol);
		pos++;
		fireChange();
	}

	public void addSymbolLast(String symbol) {
		if (symbol == null) symbol = "";
		tape.add(symbol);
		fireChange();
	}
	
	public void removeSymbol(int pos) {
		if (pos >= tape.size()) return;
		tape.remove(pos);
		if (this.pos >= pos) this.pos--;
		fireChange();
	}
	
	public void editSymbol(int pos, String symbol) {
		if (pos >= tape.size()) return;
		if (symbol == null) symbol = "";
		tape.set(pos, symbol);
		fireChange();
	}
	
	@Override
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean getEditable() { return editable; }

	public int getPos() { return pos; }
	
	/**
	 * Used by GUI, too - TapeDialog.
	 * @param pos
	 * @return
	 */
	@Override
	public String getSymbolAt(int pos) {
		if (pos >= tape.size() || (pos < 0))
			return "";
		return tape.get(pos);
	}

	/**
	 * 
	 * @param pos HAS TO BE > 0
	 * @param symbol
	 */
	@Override
	public void setSymbolAt(int pos, String symbol) {
		if (pos < tape.size() && pos >= 0)
			tape.set(pos, symbol);
		else if (pos >= tape.size())
			tape.add(pos,symbol);
		fireChange();
	}
	
	@Override
	public void setPosVisible(boolean visible) {
		showPos = visible;
	}
	
	/**
	 * Used by GUI.
	 * @return
	 */
	public boolean getPosVisible() { return showPos; }
	
	public int getSize() {
		return tape.size();
	}
	
	@Override
	public Object in(EventObject evt) {
		if (pos >= tape.size() || (pos < 0))
			return "";
		return tape.get(pos);
	}

	@Override
	public void out(EventObject evt, Object val) {
		if (pos >= tape.size()) 
			tape.add(pos, val.toString());
		else
			tape.set(pos, val.toString());
		fireChange();
	}

	@Override
	public String getHash() {
		return "c642e5f1dc280113ccd8739f3c01a06d";
	}

	@Override
	public String getID() {
		return "abstract-tape-context";
	}
	
	public void setListener(TapeListener listener) {
		this.listener = listener;
	}
	
    private void fireChange() {
    	if (listener != null)
    		listener.tapeChanged(changeEvent);
    }
	

}
