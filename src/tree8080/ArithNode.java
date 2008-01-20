/*
 * ArithNode.java
 *
 * Created on Sobota, 2007, september 22, 8:35
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080;

import compiler8080.*;
import tree8080Abstract.*;

/**
 *
 * @author vbmacher
 */
public class ArithNode extends ExprNode {
    private ExprNode left;
    private ExprNode right;
    private String operator;
    
    /** Creates a new instance of ArithNode */
    public ArithNode(ExprNode left, ExprNode right, String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }
    
    /// compile time ///
       
    public int eval(compileEnv env, int curr_addr) throws Exception {
        int lv = left.eval(env,curr_addr);;
        int rv = 0;
        if (right != null) rv = right.eval(env, curr_addr);
 
        this.value = 0;
        if (operator == "or") this.value = lv | rv;
        else if (operator == "xor") this.value = lv ^ rv;
        else if (operator == "and") this.value = lv & rv;
        else if (operator == "not") this.value = ~lv;
        else if (operator == "+") this.value = lv + rv;
        else if (operator == "-") this.value = lv - rv;
        else if (operator == "*") this.value = lv * rv;
        else if (operator == "/") this.value = lv / rv;
        else if (operator == "mod") this.value = lv % rv;
        else if (operator == "shr") this.value = lv >>> rv;
        else if (operator == "shl") this.value = lv << rv; // it works! (tested)
        return this.value;
    }

}
