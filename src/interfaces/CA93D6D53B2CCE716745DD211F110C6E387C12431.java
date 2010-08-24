/**
 * IRAMMemoryContext.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package interfaces;

import java.util.Vector;

import plugins.memory.IMemoryContext;

public interface CA93D6D53B2CCE716745DD211F110C6E387C12431 extends IMemoryContext {
	public void addLabel(int pos, String label);
	public String getLabel(int pos);
	
	// from Compiler
	public void addInputs(Vector<String> inputs);
	
	// for CPU
	public Vector<String> getInputs();
}
