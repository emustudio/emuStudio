/**
 * ColumnInfo.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package braincpu.gui;

import plugins.cpu.IDebugColumn;

public class ColumnInfo implements IDebugColumn {
    private String name;
    private Class<?> type;
    private boolean editable;
    
    public ColumnInfo(String name, Class<?> cl, boolean editable) {
        this.name = name;
        this.type = cl;
        this.editable = editable;
    }

    public Class<?> getType() { return this.type; }
    public String getName() { return this.name; }
    public boolean isEditable() { return this.editable; }
}
