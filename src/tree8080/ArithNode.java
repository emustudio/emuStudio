/*
 * ArithNode.java
 *
 * Created on Sobota, 2007, september 22, 8:35
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080;

import compiler8080.compileEnv;
import tree8080Abstract.ExprNode;

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
        int lv = left.eval(env,curr_addr);
        int rv = 0;
        if (right != null) rv = right.eval(env, curr_addr);
 
        this.value = 0;
        if (operator.equals("or")) this.value = lv | rv;
        else if (operator.equals("xor")) this.value = lv ^ rv;
        else if (operator.equals("and")) this.value = lv & rv;
        else if (operator.equals("not")) this.value = ~lv;
        else if (operator.equals("+")) this.value = lv + rv;
        else if (operator.equals("-")) this.value = lv - rv;
        else if (operator.equals("*")) this.value = lv * rv;
        else if (operator.equals("/")) this.value = lv / rv;
        else if (operator.equals("mod")) this.value = lv % rv;
        else if (operator.equals("shr")) this.value = lv >>> rv;
        else if (operator.equals("shl")) this.value = lv << rv; // it works! (tested)
        else if (operator.equals("=")) this.value = (lv == rv) ? 1 : 0;
        
        return this.value;
    }

}
