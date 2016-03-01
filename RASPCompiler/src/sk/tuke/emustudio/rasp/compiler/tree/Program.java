/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler.tree;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author miso
 */
public class Program implements ASTNode{
    private final List<Row> rows = new ArrayList<Row>();
    
    public void addRow(Row r){
        rows.add(r);
    }

    @Override
    public void accept(ASTVisitor visitor) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
