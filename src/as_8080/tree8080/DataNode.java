/*
 * DataNode.java
 *
 * Created on Sobota, 2007, september 22, 9:05
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package as_8080.tree8080;

import as_8080.impl.HEXFileHandler;
import as_8080.impl.compileEnv;
import as_8080.tree8080Abstract.CodeNode;
import as_8080.tree8080Abstract.DataValueNode;

import java.util.Vector;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
public class DataNode extends CodeNode {
    private Vector<DataValueNode> list; // this vector stores only data values

    public void addElement(DataValueNode node) {
        list.addElement(node);
    }
    
    public void addAll(Vector<DataValueNode> vec) {
        list.addAll(vec);
    }
    
    /** Creates a new instance of DataNode */
    public DataNode(int line , int column) {
        super(line, column);
        this.list = new Vector<DataValueNode>();
    }

    /// compile time ///
    
    public int getSize() {
        DataValueNode dv;
        int size = 0;
        for (int i =0; i < list.size(); i++) {
            dv = (DataValueNode)list.get(i);
            size += dv.getSize();
        }
        return size;
    }

    public void pass1(IMessageReporter r) throws Exception {
        for (int i = 0; i < list.size(); i++) {
            DataValueNode n = (DataValueNode)list.elementAt(i);
            n.pass1();
        }
    }
    
    public int pass2(compileEnv env, int addr_start) throws Exception {
        DataValueNode dv;
        for (int i =0; i < list.size(); i++) {
            dv = (DataValueNode)list.get(i);
            addr_start = dv.pass2(env, addr_start);
        }
        return addr_start;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        DataValueNode dv;
        for (int i =0; i < list.size(); i++) {
            dv = (DataValueNode)list.get(i);
            dv.pass4(hex);
        }
    }

}
