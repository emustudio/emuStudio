/*
 * LabelNode.java
 *
 * Created on Streda, 2007, október 10, 16:52
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package assembler8080.tree8080;

/**
 *
 * @author vbmacher
 */
public class LabelNode {
    private String name;
    private Integer address;
    
    private int line;
    private int column;
    
    /** Creates a new instance of LabelNode */
    public LabelNode(String name, int line, int column) {
        this.name = name;
        this.address = null;
        
        this.line = line;
        this.column = column;
    }
    
    public void setAddress(Integer address) { this.address = address; }
    public Integer getAddress() { return this.address; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    
    public String getName() { return name; }
}
