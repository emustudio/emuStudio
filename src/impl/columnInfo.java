/*
 * debugInteraction.java
 *
 * Created on Piatok, 2007, október 26, 10:51
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package impl;

import plugins.cpu.*;
/**
 *
 * @author vbmacher
 */
public class columnInfo implements IDebugColumn {
    private String name;
    private Class type;
    private boolean editable;
    
    /** Creates a new instance of debugInteraction */
    public columnInfo(String name, Class cl, boolean editable) {
        this.name = name;
        this.type = cl;
        this.editable = editable;
    }

    public Class getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public boolean isEditable() {
        return this.editable;
    }

}
