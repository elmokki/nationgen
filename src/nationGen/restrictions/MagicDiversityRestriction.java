package nationGen.restrictions;


import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class MagicDiversityRestriction extends DoubleTextBoxListRestriction<RestrictionMagicEntry> {
	
	public List<RestrictionMagicEntry> possibleRaceNames = new ArrayList<RestrictionMagicEntry>();
	
	public MagicDiversityRestriction()
	{
		super("<html>Nation needs to have x magic paths at at least level y.</html>", "Magic: Diversity");

		this.comboboxlabel = "25% probability randoms allowed";
		String[] ops = {"True", "False"};
		this.comboboxoptions = ops;
		this.hascombobox = true;
		this.textFieldLabel = "Path amount:";
		this.textfieldDefaultText = "1";
		this.textFieldLabel2 = "Path level:";
		this.textfieldDefaultText2 = "1";
		
	}
	
	@Override
	public NationRestriction getRestriction() {
		MagicDiversityRestriction res = new MagicDiversityRestriction();
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.possibleRaceNames.add(chosen.getModel().getElementAt(i));
		
		res.comboselection = this.comboselection;
		return res;
	}
	
	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleRaceNames.size() == 0)
		{
			System.out.println("Magic diversity nation restriction has no races set!");
			return true;
		}
		
		List<Unit> tempmages = n.generateComList("mage");


		
		MagicPathInts nonrandom_paths = new MagicPathInts();
		for(Unit u : tempmages)
		{
			MagicPathInts picks = u.getMagicPicks(false);
			for(MagicPath path : MagicPath.values())
			{
				if(nonrandom_paths.get(path) < picks.get(path))
					nonrandom_paths.set(path, picks.get(path));
			}
		}
		
		MagicPathInts random_paths = new MagicPathInts();
		for(Unit u : tempmages)
		{
			MagicPathInts picks = u.getMagicPicks(true);
			for(MagicPath path : MagicPath.values())
			{
				if(random_paths.get(path) < picks.get(path))
					random_paths.set(path, picks.get(path));
			}
		}
		
		
		for(RestrictionMagicEntry res : possibleRaceNames)
		{
			
			MagicPathInts crap;
			if(res.randoms)
				crap = random_paths;
			else
				crap = nonrandom_paths;
			
			int diversity = 0;
			for(MagicPath path : MagicPath.values())
				if(crap.get(path) >= res.level)
					diversity++;
			
			if(diversity >= res.paths)
				return true;
		}
		
		return false;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// Add button
		if(arg0.getSource().equals(addButton))
		{
		
			if(textfield.getText().length() > 0 &&  textfield2.getText().length() > 0 && !rmodel.contains(textfield.getText() + " - " + textfield2.getText()))
			{
				boolean randoms = comboselection == null || comboselection.equals("True");

				rmodel.addElement(new RestrictionMagicEntry(Integer.parseInt(textfield.getText()), Integer.parseInt(textfield2.getText()), randoms));
			}
			
		}
		// remove button
		if(arg0.getSource().equals(removeButton) && chosen.getSelectedIndex() > -1)
		{
			rmodel.remove(chosen.getSelectedIndex());
		}
	}
	
    @Override
    public RestrictionType getType()
    {
        return RestrictionType.MagicDiversity;
    }
}
