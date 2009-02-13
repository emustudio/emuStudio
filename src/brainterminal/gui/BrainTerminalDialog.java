/**
 * BrainTerminalDialog.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package brainterminal.gui;

import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

@SuppressWarnings("serial")
public class BrainTerminalDialog extends JDialog {
	private String inputBuffer;
	
	public BrainTerminalDialog() {
		super();
		inputBuffer = "";
		initComponents();
	}
	
	/**
	 * Metóda "zmaže" obrazovku
	 */
	public void clearScreen() {
		txtTerminal.setText("");
	}
	
	/**
	 * Metóda vypíše znak na obrazovku
	 * 
	 * @param c znak, ktorý sa má vypísať
	 */
	public void putChar(char c) {
		txtTerminal.append(String.valueOf(c));
	}
	
	/**
	 * Metóda vráti jeden znak zo vstupného
	 * buffra. Ak je prázdny, naplní ho.
	 * 
	 * @return znak z buffra
	 */
	public char getChar() {
		if (inputBuffer.equals("")) {
			inputBuffer += JOptionPane
				.showInputDialog("Zadaj vstupný znak (alebo reťazec):");
		}
		try {
			char c = inputBuffer.charAt(0);
			inputBuffer = inputBuffer.substring(1);
			return c;
		} catch(Exception e) {
			// ak náhodou používateľ zadal prázdny
			// reťazec
			return 0;
		}
	}
	
    private void initComponents() {
        JScrollPane scrollTerminal = new JScrollPane();
        txtTerminal = new JTextArea();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("BrainDuck Terminal");
        setAlwaysOnTop(true);

        txtTerminal.setColumns(20);
        txtTerminal.setEditable(false);
        txtTerminal.setRows(5);
        scrollTerminal.setViewportView(txtTerminal);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(scrollTerminal, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(scrollTerminal, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
        pack();
    }
    private JTextArea txtTerminal;

}
