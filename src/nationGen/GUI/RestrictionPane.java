package nationGen.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;







import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nationGen.NationGen;
import nationGen.restrictions.MagicAccessRestriction;
import nationGen.restrictions.MageWithAccessRestriction;
import nationGen.restrictions.MagicDiversityRestriction;
import nationGen.restrictions.NationThemeRestriction;
import nationGen.restrictions.NoPrimaryRaceRestriction;
import nationGen.restrictions.NoUnitOfRaceRestriction;
import nationGen.restrictions.PrimaryRaceRestriction;
import nationGen.restrictions.SacredRaceRestriction;
import nationGen.restrictions.NationRestriction;
import nationGen.restrictions.UnitCommandRestriction;
import nationGen.restrictions.UnitFilterRestriction;
import nationGen.restrictions.UnitOfRaceRestriction;

public class RestrictionPane extends JPanel implements ActionListener, ListSelectionListener {

	private NationRestriction[] all;
	private JButton addButton;
	private JButton removeButton;
	private JList<NationRestriction> possibles;
	private JList<NationRestriction> chosen;
	private JPanel infopane;
	DefaultListModel<NationRestriction> model = new DefaultListModel<>();

	private static final long serialVersionUID = 1L;

	private NationGen ng;
	



	public RestrictionPane(NationGen ng) {
		super(new GridLayout(1,2,5,5));
		this.ng = ng;
		init();
	}
	
	/*
	 * Update this when you add new restrictions
	 */
	private NationRestriction[] getAllRestrictions()
	{
		// Please keep this is alphabetic order
		NationRestriction[] stuff = {
			new MagicAccessRestriction(ng), 		// "Magic: Access"
			new MagicDiversityRestriction(ng),		// "Magic: Diversity"
			new MageWithAccessRestriction(ng),		// "Magic: Mage with access"
			new NationThemeRestriction(ng),
			new NoPrimaryRaceRestriction(ng),
			new NoUnitOfRaceRestriction(ng),
			new PrimaryRaceRestriction(ng),
			new SacredRaceRestriction(ng),			// "Sacred: Race"
			new UnitCommandRestriction(ng),
			new UnitFilterRestriction(ng),
			new UnitOfRaceRestriction(ng)
		};
		return stuff;
	}
	
	private void init()
	{

        all = getAllRestrictions();
		possibles = new JList<>(all);
		possibles.setPreferredSize(new Dimension(100,100));

		chosen = new JList<>(model);
		chosen.setPreferredSize(new Dimension(200,100));
		chosen.addListSelectionListener(this);
		
		BorderLayout leftLayout = new BorderLayout();
		JPanel left = new JPanel(leftLayout);
		left.setPreferredSize(new Dimension(300,350));
		leftLayout.setHgap(5);
		leftLayout.setVgap(5);
		
		GridLayout buttonLayout = new GridLayout(1,2);
		JPanel buttons = new JPanel(buttonLayout);
		buttonLayout.setHgap(5);
		buttonLayout.setVgap(5);
		addButton = new JButton("Add");
		removeButton = new JButton("Remove");
		
		addButton.addActionListener(this);
		removeButton.addActionListener(this);
		
		buttons.add(addButton);
		buttons.add(removeButton);
		
		JPanel leftleft = new JPanel(new GridLayout(1,2,5,5));
		leftleft.add(new JScrollPane(possibles));
		leftleft.add(new JScrollPane(chosen));
		
		left.add(buttons, BorderLayout.PAGE_START);
		left.add(leftleft, BorderLayout.CENTER);
		
		this.add(left);
		
		infopane = new JPanel(new GridLayout(1,1));

		infopane.add(getInfoText());
		this.add(infopane);
		
	}
	
	private JLabel getInfoText()
	{
		return new JLabel("<html>"
				+ "Warning! Restrictions are used to discard generated nations. Thus setting restrictions can increase nation generation time heavily "
				+ "or cause an infinite loop. Use this feature with care.<br><br>"
				+ "Use of restrictions does not break nation seed compatibility, but does break main seed compatibility. This means you can send nation"
				+ "seeds to other people without specifying restrictions, but the same does not hold for the seed in Main tab. Additionally, setting "
				+ "generation from nation seeds (Options tab) disables all restrictions.<br><br>"
				+ "Restrictions generally work so that only one option inside a restriction needs to hold but all restrictions need to hold. This means"
				+ "that if you want to have both death and astral access, you need to set two restrictions, but if you are happy with either, one is enough."
				+ "<br><br>"
				+ "For more information, please refer to documentation."
				+ "</html>");
	}

	public List<NationRestriction> getRestrictions()
	{
		List<NationRestriction> list = new ArrayList<>();
		for(int i = 0; i < model.size(); i++)
		{
			NationRestriction nrg = model.getElementAt(i);
			list.add(nrg.getRestriction());
		}
		return list;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// Add button
		if(arg0.getSource().equals(addButton) && possibles.getSelectedIndex() > -1)
		{
			
			NationRestriction newres = possibles.getSelectedValue().getInstanceOf();
			if(newres != null && !model.contains(newres));
			{
				model.addElement(newres);
			}
			
		}
		// remove button
		if(arg0.getSource().equals(removeButton) && chosen.getSelectedIndex() > -1)
		{
			model.remove(chosen.getSelectedIndex());
			infopane.removeAll();
			this.revalidate();
			this.repaint();
		}
	}


	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		
		@SuppressWarnings("rawtypes")
		int srcindex = ((JList) arg0.getSource()).getSelectedIndex();
		
		if(arg0.getValueIsAdjusting() == false)
		{
			if(model.size() == 0 || srcindex < 0 || srcindex > model.size())
			{
				infopane.removeAll();
			}
			else
			{
				NationRestriction res = model.getElementAt(srcindex);
				infopane.removeAll();
				infopane.setLayout(res.getLayout());
				res.getGUI(infopane);
			}
			this.revalidate();
			this.repaint();
		}
	}
}
