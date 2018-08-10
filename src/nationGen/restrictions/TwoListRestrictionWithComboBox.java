package nationGen.restrictions;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Extension for TwoListRestriction with a custom-type combo box!
 * @author Elmokki
 *
 */
public abstract class TwoListRestrictionWithComboBox<E, F> extends TwoListRestriction<E> implements ActionListener, ItemListener  {
	

	public TwoListRestrictionWithComboBox(String text, String name)
	{
		super(text, name);
	}
	
	@Override
	public String toString() {
		return name;
	}


	protected JComboBox<F> combobox = null;
	protected String comboboxlabel = "Undefined label";
	protected F[] comboboxoptions = null;
	private int index = 0;
	public F comboselection = null;
	@Override
	public void getGUI(JPanel panel) {
				
		// Text and buttons
		
		int toprows = 3;
		if(extraTextField)
			toprows++;
		
		JPanel top = new JPanel(new GridLayout(toprows,1,5,5));
		top.add(new JLabel(text));
		
		JPanel combo = new JPanel(new GridLayout(1,2,5,5));
		JPanel tpanel2 = new JPanel(new GridLayout(1,2,5,5));
		tpanel2.add(new JLabel(comboboxlabel));
		combobox = new JComboBox<F>(comboboxoptions);
		combobox.addItemListener(this);
	    combobox.setSelectedIndex(index);
		tpanel2.add(combobox);
		top.add(tpanel2);
		
		if(extraTextField)
		{
			JPanel tpanel = new JPanel(new GridLayout(1,2,5,5));
			tpanel.add(new JLabel(textFieldLabel));
			textfield = new JTextField(textfieldDefaultText);
			tpanel.add(textfield);
			top.add(tpanel);
			
	        // Listen for changes in the text.
	        textfield.getDocument().addDocumentListener(new DocumentListener() {
	          public void changedUpdate(DocumentEvent e) {
	              textFieldUpdate();
	          }
	          public void removeUpdate(DocumentEvent e) {
	              textFieldUpdate();
	          }
	          public void insertUpdate(DocumentEvent e) {
	              textFieldUpdate();
	          }
	        });
		}
		
		addButton = new JButton("Add");
		addButton.addActionListener(this);
		removeButton = new JButton("Remove");
		removeButton.addActionListener(this);

		combo.add(addButton);
		combo.add(removeButton);
		top.add(combo);
		
		// Lists
		
		JPanel lists = new JPanel(new GridLayout(1,2,5,5));
		
		all = new JList<E>(rmodel);

		
		lists.add(new JScrollPane(all));
		
		chosen = new JList<E>(rmodel2);
		lists.add(new JScrollPane(chosen));
	
		
		panel.add(top, BorderLayout.PAGE_START);
		panel.add(lists, BorderLayout.CENTER);

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// Add button
		if(arg0.getSource().equals(addButton) && all.getSelectedIndex() > -1)
		{
			if(rmodel.getElementAt(all.getSelectedIndex()) != null)
			{
				if(!rmodel2.contains(rmodel.getElementAt(all.getSelectedIndex())))
					rmodel2.addElement(rmodel.getElementAt(all.getSelectedIndex()));
			}
		}
		// remove button
		if(arg0.getSource().equals(removeButton) && chosen.getSelectedIndex() > -1)
		{
			rmodel2.remove(chosen.getSelectedIndex());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void itemStateChanged(ItemEvent arg0) {
		
		if(arg0.getStateChange() == 1)
		{
			this.comboselection = (F)combobox.getSelectedItem();
			this.index = combobox.getSelectedIndex();
		}
		
	}

	
}
