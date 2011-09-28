/**
 * IRAMMemoryContext.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2011 Peter Jakubčo <pjakubco at gmail.com>
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

import java.util.ArrayList;

import emuLib8.plugins.memory.IMemoryContext;

public interface C8E258161A30C508D5E8ED07CE943EEF7408CA508 extends IMemoryContext {
	public void addLabel(int pos, String label);
	public String getLabel(int pos);
	
	// from Compiler
	public void addInputs(ArrayList<String> inputs);
	
	// for CPU
	public ArrayList<String> getInputs();
}
