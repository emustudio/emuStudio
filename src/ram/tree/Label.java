package ram.tree;

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
    	if (!evaluated) 
    		throw new IndexOutOfBoundsException();
    	return address; 
    }
    
    public String getValue() {
    	return value;
    }
    
}
