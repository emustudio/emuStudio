/*
 * DataNode.java
 *
 * Created on Streda, 2008, august 13, 11:35
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.Namespace;
import java.util.Vector;
import plugins.compiler.IMessageReporter;
import treeZ80Abstract.InstrData;
import treeZ80Abstract.DataValue;

/**
 *
 * @author vbmacher
 */
public class DataNode extends InstrData {
    private Vector<DataValue> list; // this vector stores only data values

    public void addElement(DataValue node) {
        list.addElement(node);
    }
    
    public void addAll(Vector<DataValue> vec) {
        list.addAll(vec);
    }
    
    /** Creates a new instance of DataNode */
    public DataNode(int line , int column) {
        super(line, column);
        this.list = new Vector<DataValue>();
    }

    /// compile time ///
    
    public int getSize() {
        DataValue dv;
        int size = 0;
        for (int i =0; i < list.size(); i++) {
            dv = (DataValue)list.get(i);
            size += dv.getSize();
        }
        return size;
    }

    public void pass1(IMessageReporter rep) throws Exception {
        for (int i = 0; i < list.size(); i++) {
            DataValue n = (DataValue)list.elementAt(i);
            n.pass1();
        }
    }
    
    public int pass2(Namespace env, int addr_start) throws Exception {
        DataValue dv;
        for (int i =0; i < list.size(); i++) {
            dv = (DataValue)list.get(i);
            addr_start = dv.pass2(env, addr_start);
        }
        return addr_start;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        DataValue dv;
        for (int i =0; i < list.size(); i++) {
            dv = (DataValue)list.get(i);
            dv.pass4(hex);
        }
    }

}
