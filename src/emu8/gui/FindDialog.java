/*
 * FindDialog.java
 *
 * Created on ${date}, 11:26
 * hold to: KISS, YAGNI
 *
 */

package emu8.gui;

import emu8.Main;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.ComboBoxModel;
import javax.swing.JTextPane;
import javax.swing.event.ListDataListener;

/**
 *
 * @author  vbmacher
 */
public class FindDialog extends javax.swing.JDialog {
    private static Matcher matcher;
    private static int radioDirection = 0;
    
    private static final int cCASE = 1;
    private static final int cWHOLE = 2;

    private static byte checks = 0;
    
    private static ArrayList list = new ArrayList();
    private static ArrayList rlist = new ArrayList();
    private String repl = "";
    private JTextPane textPane;
    
    private class CMBModel implements ComboBoxModel {
        private int in = -1;
        private ArrayList clist;
        
        public CMBModel(ArrayList clist) {
            this.clist = clist;
        }
        public void setSelectedItem(Object anItem) {
            in = clist.indexOf(anItem);
        }
        public Object getSelectedItem() {
            if (in != -1) return clist.get(in);
            else return null;
        }
        public int getSize() { return clist.size(); }
        public Object getElementAt(int index) {
            return clist.get(index);
        }
        public void addListDataListener(ListDataListener l) {}
        public void removeListDataListener(ListDataListener l) {}
    }
    
    /** Creates new form FindDialog */
    public FindDialog(StudioFrame parent, boolean modal, JTextPane pane) {
        super(parent, modal);
        this.textPane = pane;
        initComponents();
        
        switch(radioDirection) {
            case 0: endRadio.setSelected(true); break;
            case 1: startRadio.setSelected(true); break;
            case 2: allRadio.setSelected(true); break;
        }
        if ((checks & cCASE) != 0) caseCheck.setSelected(true);
        if ((checks & cWHOLE) != 0) wholeCheck.setSelected(true);
        
        searchCombo.setModel(new CMBModel(list));
        replaceCombo.setModel(new CMBModel(rlist));        
        this.setLocationRelativeTo(parent);
    }
    
    private String saveGUI() {
        if (endRadio.isSelected()) radioDirection = 0;
        else if (startRadio.isSelected()) radioDirection = 1;
        else radioDirection = 2;
        if (caseCheck.isSelected()) checks |= cCASE;
        else checks &= (~cCASE);
        if (wholeCheck.isSelected()) checks |= cWHOLE;
        else checks &= (~cWHOLE);
        String str = (String)searchCombo.getEditor().getItem();
        if (!list.contains(str)) {
            list.add(str);
            searchCombo.setModel(new CMBModel(list));
        }
        this.repl = (String)replaceCombo.getEditor().getItem();
        if (!rlist.contains(this.repl)) {
            rlist.add(this.repl);
            replaceCombo.setModel(new CMBModel(rlist));
        }
        return str;
    }

    public boolean findForward() throws NullPointerException {
        if (matcher == null)
            throw new NullPointerException("matcher can't be null, use dialog");
        int startM = -1;
        int endM = -1;
        boolean match = false;
        
        String txt = textPane.getText().replaceAll("\n\r", "\n")
                .replaceAll("\r\n", "\n");
        matcher.reset(txt);
        int endPos = textPane.getDocument().getEndPosition().getOffset()-1;
        int curPos = textPane.getCaretPosition();
        matcher.useTransparentBounds(false);
        if (radioDirection == 0) {
            matcher.region(curPos, endPos);
            match = matcher.find();
            if (match) {
                startM = matcher.start();
                endM = matcher.end();
            }
        }
        else if (radioDirection == 1) {
            matcher.region(0, curPos);
            endM = 0;
            match = false;
            while (matcher.find(endM)) {
                if (matcher.end() >= curPos) break;
                match = true;
                startM = matcher.start();
                endM = matcher.end();
            }
        }
        else if (radioDirection == 2) {
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = true;
            if (!matcher.find(curPos)) match = matcher.find(0);
            if (match) {
                startM = matcher.start();
                endM = matcher.end();
            }
        }
        if (match) textPane.select(startM, endM);
        return match;
    }

    // maybe optimization needed
    public boolean replaceForward(boolean all) throws NullPointerException {
        if (matcher == null)
            throw new NullPointerException("matcher can't be null, use dialog");
        boolean match = false;
        
        String txt = textPane.getText().replaceAll("\n\r", "\n")
                .replaceAll("\r\n", "\n");
        matcher.reset(txt);
        int endPos = textPane.getDocument().getEndPosition().getOffset()-1;
        int curPos = textPane.getCaretPosition();
        
        StringBuffer sb = new StringBuffer();
        matcher.useTransparentBounds(false);
        if (radioDirection == 0) {
            // To end of document
            matcher.region(curPos, endPos);
            match = matcher.find();
            if (match) {
                matcher.appendReplacement(sb, repl);
                while (all && matcher.find())
                    matcher.appendReplacement(sb, repl);
                matcher.appendTail(sb);
                textPane.setText(sb.toString());
            }
        } else if (radioDirection == 1) {
            // To start
            matcher.region(0, curPos);
            match = false;
            int endM = 0, startM = 0;
            if (!all) {
                while (matcher.find(endM)) {
                    if (matcher.end() >= curPos) break;
                    startM = matcher.start()-1;
                    endM = matcher.end();
                    match = true;
                }
                if (startM < 0) startM = 0;
                matcher.region(startM, curPos); // here is only one match
                match = matcher.find(startM);
                if (match) {
                    matcher.appendReplacement(sb, repl);
                    matcher.appendTail(sb);
                    textPane.setText(sb.toString());
                }
            } else {
                while (matcher.find())
                    matcher.appendReplacement(sb, repl);
                matcher.appendTail(sb);
                textPane.setText(sb.toString());
            }
        }
        else if (radioDirection == 2) {
            // all document
            matcher.region(0, endPos);
            matcher.useTransparentBounds(true);
            match = true;
            
            if (!all) {
                if (!matcher.find(curPos)) match = matcher.find(0);
                if (match)
                    matcher.appendReplacement(sb, repl);
                matcher.appendTail(sb);
                textPane.setText(sb.toString());
            } else 
                textPane.setText(matcher.replaceAll(repl));
        }
        return match;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.ButtonGroup buttonGroup1 = new javax.swing.ButtonGroup();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        searchCombo = new javax.swing.JComboBox();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        caseCheck = new javax.swing.JCheckBox();
        wholeCheck = new javax.swing.JCheckBox();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        endRadio = new javax.swing.JRadioButton();
        startRadio = new javax.swing.JRadioButton();
        allRadio = new javax.swing.JRadioButton();
        javax.swing.JButton searchButton = new javax.swing.JButton();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        replaceCombo = new javax.swing.JComboBox();
        javax.swing.JButton replaceButton = new javax.swing.JButton();
        javax.swing.JButton replaceAllButton = new javax.swing.JButton();

        setTitle("Find/replace text");
        setAlwaysOnTop(true);
        setResizable(false);

        jLabel1.setText("Search for:");

        searchCombo.setEditable(true);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Options"));

        caseCheck.setText("Case sensitive");

        wholeCheck.setText("Whole words");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(caseCheck)
                    .addComponent(wholeCheck))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(caseCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wholeCheck)
                .addContainerGap(37, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Direction"));

        buttonGroup1.add(endRadio);
        endRadio.setSelected(true);
        endRadio.setText("To end of document");

        buttonGroup1.add(startRadio);
        startRadio.setText("To start of document");

        buttonGroup1.add(allRadio);
        allRadio.setText("All document");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(endRadio)
                    .addComponent(startRadio)
                    .addComponent(allRadio))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(endRadio)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(startRadio)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(allRadio)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        searchButton.setText("Search");
        searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Replace with:");

        replaceCombo.setEditable(true);

        replaceButton.setText("Replace");
        replaceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceButtonActionPerformed(evt);
            }
        });

        replaceAllButton.setText("Replace all");
        replaceAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceAllButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(searchCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(replaceAllButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(replaceButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(replaceCombo, 0, 293, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(searchCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(replaceCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchButton)
                    .addComponent(replaceButton)
                    .addComponent(replaceAllButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
        String str = saveGUI();
        int flags = 0;
        if ((checks & cCASE) == 0) flags |= Pattern.CASE_INSENSITIVE;
        
        try {
            if ((checks & cWHOLE) != 0) str = "\\b(" + str + ")\\b";
            Pattern p = Pattern.compile(str, flags);
            String txt = textPane.getText().replaceAll("\n\r", "\n")
                    .replaceAll("\r\n", "\n");
            matcher = p.matcher(txt);
            
            if (findForward()) {
                this.setVisible(false);
                textPane.grabFocus();
            } else {
                Main.showMessage("Expression was not found");
            }
        } catch (PatternSyntaxException e) {
            Main.showErrorMessage("Pattern syntax error");
            searchCombo.grabFocus();
            return;
        }
    }//GEN-LAST:event_searchButtonActionPerformed

    private void replaceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceButtonActionPerformed
        String str = saveGUI();
        
        int flags = 0;
        if ((checks & cCASE) == 0) flags |= Pattern.CASE_INSENSITIVE;
        try {
            if ((checks & cWHOLE) != 0) str = "\\b(" + str + ")\\b";
            Pattern p = Pattern.compile(str, flags);
            String txt = textPane.getText().replaceAll("\n\r", "\n")
                    .replaceAll("\r\n", "\n");
            matcher = p.matcher(txt);
            if (replaceForward(false)) {
                this.setVisible(false);
                textPane.grabFocus();
            } else Main.showMessage("Expression was not found");
        } catch (PatternSyntaxException e) {
            Main.showErrorMessage("Pattern syntax error");
            searchCombo.grabFocus();
            return;
        }
}//GEN-LAST:event_replaceButtonActionPerformed

    private void replaceAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceAllButtonActionPerformed
        String str = saveGUI();
        int flags = 0;
        if ((checks & cCASE) == 0) flags |= Pattern.CASE_INSENSITIVE;        
        try {
            if ((checks & cWHOLE) != 0) str = "\\b(" + str + ")\\b";
            Pattern p = Pattern.compile(str, flags);
            String txt = textPane.getText().replaceAll("\n\r", "\n")
                    .replaceAll("\r\n", "\n");
            matcher = p.matcher(txt);
            if (replaceForward(true)) {
                this.setVisible(false);
                textPane.grabFocus();
            } else Main.showMessage("Expression was not found");
        } catch (PatternSyntaxException e) {
            Main.showErrorMessage("Pattern syntax error");
            searchCombo.grabFocus();
            return;
        }
}//GEN-LAST:event_replaceAllButtonActionPerformed
  
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JRadioButton allRadio;
    javax.swing.JCheckBox caseCheck;
    javax.swing.JRadioButton endRadio;
    javax.swing.JComboBox replaceCombo;
    javax.swing.JComboBox searchCombo;
    javax.swing.JRadioButton startRadio;
    javax.swing.JCheckBox wholeCheck;
    // End of variables declaration//GEN-END:variables
    
}
