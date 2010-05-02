/*
 * NeedMorePassException.java
 *
 * Created on Štvrtok, 2007, október 11, 11:28
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * this exception is throwed while compiling forward references that are in
 * expressions. Expression with forward reference for label can't be evaulated
 * without knowing a value of the label (its address that label is pointing at).
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package as_z80.impl;

/**
 *
 * @author vbmacher
 */
public class NeedMorePassException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Object obj;
    private int line;
    private int column;
    
    /** Creates a new instance of NeedMorePassException */
    public NeedMorePassException(Object o, int line, int column) {
        this.obj = o;
        this.line = line;
        this.column = column;
    }
    
    public Object getObject() { return obj; }
    public int getLine() { return this.line; }
    public int getColumn() { return this.column; }
}
