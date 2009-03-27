/**
 * CompilerEnvironment.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *   KISS, YAGNI
 */
package ram.compiled;

import java.util.ArrayList;

import ram.tree.Label;

public class CompilerEnvironment {
    private static ArrayList<Label> labels = new ArrayList<Label>();
    
    public static void addLabel(Label label){
    	labels.add(label);
    }
    
    public static int getLabelAddr(String label) {
    	String l = label.toUpperCase() + ":";
    	for (int i = 0; i < labels.size(); i++) {
    		Label lab = labels.get(i);
    		String ll = lab.getValue().toUpperCase();
    		if (ll.equals(l))
    			return lab.getAddress();
    	}
    	// throw new ...label undefined
    	return -1;
    }
    
    public static Label[] getLabels() {
    	return labels.toArray(new Label[0]);
    }
}
