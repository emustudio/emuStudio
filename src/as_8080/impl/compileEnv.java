/*
 * compileEnv.java
 *
 * Created on Pondelok, 2007, október 8, 18:08
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Compile environment stores needed values for all compiler passes
 * This is something like symbol table
 */

package as_8080.impl;

import java.util.Vector;

import as_8080.tree8080.EquPseudoNode;
import as_8080.tree8080.InstructionNode;
import as_8080.tree8080.LabelNode;
import as_8080.tree8080.MacroPseudoNode;
import as_8080.tree8080.SetPseudoNode;

/**
 *
 * @author vbmacher
 */
/*
 * sets, macros and equs are pseudoinstructions that arent added to symbol table
 * in pass1. This means that if eg. equ wasnt defined before first use
 * error comes.
 */
public class compileEnv {
    private Vector<LabelNode> defLabels;        // labelnode objects
    private Vector<MacroPseudoNode> defMacros;  // all macros
    private Vector<EquPseudoNode> defEqus;      // all equs
    private Vector<SetPseudoNode> defSets;      // all sets
    private Vector<InstructionNode> passNeed;   // objects that need more passes
    
    /** Creates a new instance of compileEnv */
    public compileEnv() {
        defLabels = new Vector<LabelNode>();
        defMacros = new Vector<MacroPseudoNode>();
        defEqus = new Vector<EquPseudoNode>();
        defSets = new Vector<SetPseudoNode>();
        passNeed = new Vector<InstructionNode>();
    }
    
    // check if id is already defined (as whatever)
    private boolean idExists(String name) {        
        for (int i = defLabels.size()-1; i >= 0; i--) {
            LabelNode in = (LabelNode)defLabels.get(i);
            if (in.getName().equals(name)) return true;
        }
        for (int i = defMacros.size()-1; i >= 0; i--) {
            MacroPseudoNode mn = (MacroPseudoNode)defMacros.get(i);
            if (mn.getName().equals(name)) return true;
        }
        for (int i = defEqus.size()-1; i >= 0; i--) {
            EquPseudoNode mn = (EquPseudoNode)defEqus.get(i);
            if (mn.getName().equals(name)) return true;
        }
        for (int i = defSets.size()-1; i >= 0; i--) {
            SetPseudoNode mn = (SetPseudoNode)defSets.get(i);
            if (mn.getName().equals(name)) return true;
        }
        return false;
    }
    
    //----
    
    public boolean addLabelDef(LabelNode l) { 
        if (idExists(l.getName()) == true) return false;
        else defLabels.addElement(l);
        return true;
    }
    
    public LabelNode getLabel(String name) {
        for (int i = 0; i < defLabels.size(); i++) {
            LabelNode lab = (LabelNode)defLabels.get(i);
            if (lab.getName().equals(name)) return lab;
        }
        return null;
    }
    
    //-----
    
    public boolean addMacroDef(MacroPseudoNode m) {
        if (idExists(m.getName()) == true) return false;
        else defMacros.addElement(m);
        return true;
    }
    
    // search for macro definition in symbol table
    public MacroPseudoNode getMacro(String name) {
        for (int i = 0; i < defMacros.size(); i++) {
            MacroPseudoNode mac = (MacroPseudoNode)defMacros.get(i);
            if (mac.getName().equals(name)) return mac;
        }
        return null;
    }
    
    //---
    
    public boolean addEquDef(EquPseudoNode e) {
        if (idExists(e.getName()) == true) return false;
        else defEqus.addElement(e);
        return true;
    }
    
    public EquPseudoNode getEqu(String name) {
        for (int i = 0; i < defEqus.size(); i++) {
            EquPseudoNode equ = (EquPseudoNode)defEqus.get(i);
            if (equ.getName().equals(name)) return equ;
        }
        return null;
    }
    
    //---
    
    // prida alebo prepise existujucu definiciu
    // pridava sa samozrejme az v pass2
    public boolean addSetDef(SetPseudoNode s) {
        SetPseudoNode exs = getSet(s.getName());
        if (exs != null) defSets.remove(exs);
        if (idExists(s.getName()) == true) return false;
        defSets.addElement(s);
        return true;
    }
    
    public SetPseudoNode getSet(String name) {
        for (int i = 0; i < defSets.size(); i++) {
            SetPseudoNode set = (SetPseudoNode)defSets.get(i);
            if (set.getName().equals(name)) return set;
        }
        return null;
    }
    
    //---
    
    // odstrani vsetky existujuce definicie s danym nazvom
    // vyuziva sa pri bloku macro
    public void removeAllDefinitions(String name) {
        for (int i = defLabels.size()-1; i >= 0; i--) {
            LabelNode in = (LabelNode)defLabels.get(i);
            if (in.getName().equals(name)) defLabels.remove(i);
        }
        for (int i = defMacros.size()-1; i >= 0; i--) {
            MacroPseudoNode mn = (MacroPseudoNode)defMacros.get(i);
            if (mn.getName().equals(name)) defMacros.remove(i);
        }
        for (int i = defEqus.size()-1; i >= 0; i--) {
            EquPseudoNode mn = (EquPseudoNode)defEqus.get(i);
            if (mn.getName().equals(name)) defEqus.remove(i);
        }
        for (int i = defSets.size()-1; i >= 0; i--) {
            SetPseudoNode mn = (SetPseudoNode)defSets.get(i);
            if (mn.getName().equals(name)) defSets.remove(i);
        }
    }
    
    public boolean copyTo(compileEnv env) {
        boolean r = true;
        for (int i = 0; i < defLabels.size(); i++)
            r &= env.addLabelDef((LabelNode)defLabels.get(i));
        for (int i = 0; i < defMacros.size(); i++)
            r &= env.addMacroDef((MacroPseudoNode)defMacros.get(i));
        for (int i = 0; i < defEqus.size(); i++)
            r &= env.addEquDef((EquPseudoNode)defEqus.get(i));
        for (int i = 0; i < defSets.size(); i++)
            r &= env.addSetDef((SetPseudoNode)defSets.get(i));
        return r;
    }
    //---
    
    public void addPassNeed(InstructionNode n) {
        passNeed.addElement(n);
    }
    public int getPassNeedCount() { return passNeed.size(); }
    public InstructionNode getPassNeed(int index) {
        return (InstructionNode)passNeed.get(index);
    }
    public void removePassNeed(InstructionNode n) {
        passNeed.removeElement(n);
    }
    public void removePassNeed(int index) {
        passNeed.removeElementAt(index);
    }
    public void clearPassNeeds() { passNeed.clear(); }
    
}
