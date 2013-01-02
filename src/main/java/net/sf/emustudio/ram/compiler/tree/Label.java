/*
 * Label.java
 *
 * Copyright (C) 2009-2012 Peter Jakubčo
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
package net.sf.emustudio.ram.compiler.tree;

public class Label {
    private int address;
    private String value;
    private boolean evaluated = false;
    
    public Label(String text) {
    	this.value = text.toUpperCase();
    }
    
    public int pass1(int addr) {
    	this.address = addr;
    	this.evaluated = true;
    	return addr;
    }
    
    public int getAddress() { 
    	if (!evaluated) {
            throw new IndexOutOfBoundsException();
        }
    	return address; 
    }
    
    public String getValue() {
    	return value;
    }
    
}
