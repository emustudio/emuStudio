/**
 * IRAMMemoryContext.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */
package interfaces;

import plugins.memory.IMemoryContext;

public interface IRAMMemoryContext extends IMemoryContext {
	public void addLabel(int pos, String label);
	public String getLabel(int pos);
}
