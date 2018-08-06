package nationGen.restrictions;


import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Class for generic restrictions that contain picking options from list.
 * At this point has some toggles:
 * - Text box (see MagicAccessRestriction for how to toggle it on (and how to override button presses if needed))
 * 
 * @author Elmokki
 *
 */
public abstract class TwoListRestriction<E> implements NationRestriction, ActionListener  {
	
	protected String text = "";
	protected String name = "Generic two list restriction";
	
	// Extra text field
	protected boolean extraTextField = false;
	protected String textFieldLabel = "Undefined label";
	protected String textfieldDefaultText = "";

	public TwoListRestriction(String text, String name)
	{
		this.name = name;
		this.text = text;
	}
	
	@Override
	public String toString() {
		return name;
	}


	protected DefaultListModel<E> rmodel = new DefaultListModel<>();
	protected DefaultListModel<E> rmodel2 = new DefaultListModel<>();
	protected JButton addButton;
	protected JButton removeButton;
	protected JList<E> chosen;
	protected JList<E> all;
	
	// Extra text field
	protected JTextField textfield = null;
	
	
	@Override
	public void getGUI(JPanel panel) {
				
		// Text and buttons
		
		int toprows = 2;
		if(extraTextField)
			toprows++;
		
		JPanel top = new JPanel(new GridLayout(toprows,1,5,5));
		top.add(new JLabel(text));
		JPanel combo = new JPanel(new GridLayout(1,2,5,5));
		
		
		if(extraTextField)
		{
			JPanel tpanel = new JPanel(new GridLayout(1,2,5,5));
			tpanel.add(new JLabel(textFieldLabel));
			textfield = new JTextField(textfieldDefaultText);
			tpanel.add(textfield);
			top.add(tpanel);
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
	public LayoutManager getLayout() {
		return new BorderLayout();
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
	
}
