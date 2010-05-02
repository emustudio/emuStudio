/*
 * Namespace.java
 *
 * Created on Pondelok, 2007, október 8, 18:08
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Compile environment stores needed values for all compiler passes
 * This is something like symbol table
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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import as_z80.treeZ80.Label;
import as_z80.treeZ80.PseudoMACRO;
import as_z80.treeZ80.PseudoEQU;
import as_z80.treeZ80.PseudoVAR;
import as_z80.treeZ80.Row;

/**
 *
 * @author vbmacher
 */
/*
 * sets, macros and equs are pseudoinstructions that arent added to symbol table
 * in pass1. This means that if eg. equ wasnt defined before first use
 * error comes.
 */
public class Namespace {
    private Hashtable<String,Label> defLabels;  // labelnode objects
    private Hashtable<String,PseudoMACRO> defMacros;  // all macros
    private Hashtable<String,PseudoEQU> defEqus;    // all equs
    private Hashtable<String,PseudoVAR> defVars;    // all sets
    private Vector<Row> passNeed;   // objects that need more passes
    
    /** Creates a new instance of Namespace */
    public Namespace() {
        defLabels = new Hashtable<String,Label>();
        defMacros = new Hashtable<String,PseudoMACRO>();
        defEqus = new Hashtable<String,PseudoEQU>();
        defVars = new Hashtable<String,PseudoVAR>();
        passNeed = new Vector<Row>();
    }
    
    // check if id is already defined (as whatever)
    private boolean idExists(String name) {
        if (defLabels.containsKey(name)) return true;
        if (defMacros.containsKey(name)) return true;
        if (defEqus.containsKey(name)) return true;
        if (defVars.containsKey(name)) return true;
        return false;
    }
    
    //----
    
    public boolean addLabelDef(Label l) { 
        String n = l.getName();
        if (idExists(n) == true) return false;
        else defLabels.put(n, l);
        return true;
    }
    
    public Label getLabel(String name) {
        return defLabels.get(name);
    }
    
    //-----
    
    public boolean addMacroDef(PseudoMACRO m) {
        String n = m.getName();
        if (idExists(n) == true) return false;
        else defMacros.put(n,m);
        return true;
    }
    
    // search for macro definition in symbol table
    public PseudoMACRO getMacro(String name) {
        return defMacros.get(name);
    }
    
    //---
    
    public boolean addEquDef(PseudoEQU e) {
        String n = e.getName();
        if (idExists(n) == true) return false;
        else defEqus.put(n, e);
        return true;
    }
    
    public PseudoEQU getEqu(String name) {
        return defEqus.get(name);
    }
    
    //---
    
    // prida alebo prepise existujucu definiciu
    // pridava sa samozrejme az v pass2
    public boolean addVarDef(PseudoVAR s) {
        defVars.put(s.getName(),s);
        return true;
    }
    
    public PseudoVAR getVar(String name) {
        return defVars.get(name);
    }
    
    //---
    
    // odstrani vsetky existujuce definicie s danym nazvom
    // vyuziva sa pri bloku macro
    public void removeAllDefinitions(String name) {
        defLabels.remove(name);
        defMacros.remove(name);
        defEqus.remove(name);
        defVars.remove(name);
    }
    
    public void copyTo(Namespace env) {
        Iterator<?> i = defLabels.values().iterator();
        while (i.hasNext()) env.addLabelDef((Label)i.next());
        i = defMacros.values().iterator();
        while (i.hasNext()) env.addMacroDef((PseudoMACRO)i.next());
        i = defEqus.values().iterator();
        while (i.hasNext()) env.addEquDef((PseudoEQU)i.next());
        i = defVars.values().iterator();
        while (i.hasNext()) env.addVarDef((PseudoVAR)i.next());
    }
    //---
    
    public void addPassNeed(Row n) {
        passNeed.addElement(n);
    }
    public int getPassNeedCount() { return passNeed.size(); }
    
    public Row getPassNeed(int index) {
        return (Row)passNeed.get(index);
    }
    public void removePassNeed(Row n) {
        passNeed.removeElement(n);
    }
    public void removePassNeed(int index) {
        passNeed.removeElementAt(index);
    }
    public void clearPassNeeds() { passNeed.clear(); }
    
}
