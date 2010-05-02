/**
 * CompilerEnvironment.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *   KISS, YAGNI
 */
package ramc_ram.compiled;

import java.util.Vector;

import ramc_ram.tree.Label;

public class CompilerEnvironment {
    private static Vector<Label> labels = new Vector<Label>();
    private static Vector<String> inputs = new Vector<String>();
    
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
    
    public static void addInputs(Vector<String> inp) {
    	inputs.addAll(inp);
    }
    
    public static Vector<String> getInputs() {
    	return inputs;
    }
    
    public static void clear() {
    	inputs.clear();
    	labels.clear();
    }
}
