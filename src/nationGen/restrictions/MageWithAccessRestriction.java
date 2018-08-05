package nationGen.restrictions;


import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class MageWithAccessRestriction extends TwoListRestrictionWithComboBox<String, String>  {
	public List<String> neededPaths = new ArrayList<String>();
	
	
	private NationGen ng;
	public MageWithAccessRestriction(NationGen ng)
	{
		super(ng, "<html>Nation needs have 1 in 4 (100% random for 4 paths or better) access to at least one of the paths listed in the right box on a single mage</html>", "Magic: Mage with access");
		this.ng = ng;
		
		this.extraTextField = true;
		this.textfieldDefaultText = "1";
		this.textFieldLabel = "Minimum magic level for added paths";
		
		this.comboboxlabel = "25% probability randoms allowed";
		String[] ops = {"True", "False"};
		this.comboboxoptions = ops;
		
		for(int i = 0; i < 8; i++)
			rmodel.addElement(Generic.integerToPath(i));
		
			
		
		
	}
	


	@Override
	public NationRestriction getRestriction() {
		MageWithAccessRestriction res = new MageWithAccessRestriction(ng);
		
		for(int i = 0; i < chosen.getModel().getSize(); i++)
			res.neededPaths.add(chosen.getModel().getElementAt(i));
		
		res.comboselection = this.comboselection;
		
		return res;
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// Add button
		if(arg0.getSource().equals(addButton) && all.getSelectedIndex() > -1)
		{
			if(rmodel.getElementAt(all.getSelectedIndex()) != null)
			{
				if(!rmodel2.contains(rmodel.getElementAt(all.getSelectedIndex())))
				{	if(textfield == null)
						rmodel2.addElement(rmodel.getElementAt(all.getSelectedIndex()));
					else if(Generic.isNumeric(textfield.getText()))
						rmodel2.addElement(rmodel.getElementAt(all.getSelectedIndex()) + " " + textfield.getText());

				}
			}
		}
		// remove button
		if(arg0.getSource().equals(removeButton) && chosen.getSelectedIndex() > -1)
		{
			rmodel2.remove(chosen.getSelectedIndex());
		}
	}

	@Override
	public boolean doesThisPass(Nation n) {
		
		List<Unit> tempmages = n.generateComList("mage");
		
	


		if(neededPaths.size() == 0)
		{
			System.out.println("Magic: Mage with Access nation restriction has no paths set!");
			return true;
		}
		
		
		boolean randoms = comboselection == null || comboselection.equals("True");
		for(Unit u : tempmages)
		{
			boolean pass = true;
			int[] paths = u.getMagicPicks(randoms);
			for(String p : neededPaths)
			{
				List<String> args = Generic.parseArgs(p);
				int path = Generic.PathToInteger(args.get(0));
				int level = Integer.parseInt(args.get(1));
				if(paths[path] < level)
					pass = false;
			}
			if(pass)
				return true;
		}
		return false;
	}

    @Override
    public RestrictionType getType()
    {
        return RestrictionType.MageWithAccess;
    }

	
}
