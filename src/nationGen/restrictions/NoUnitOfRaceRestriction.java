package nationGen.restrictions;



import java.util.List;
import java.util.ArrayList;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Race;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class NoUnitOfRaceRestriction extends TwoListRestrictionWithComboBox<Race, String> {
	public List<String> possibleRaceNames = new ArrayList<String>();
	
	private NationGen ng;
	private NationGenAssets assets;
	
	private String[] ownoptions = {"All", "Troops", "Commanders", "Sacred troops"};
	public NoUnitOfRaceRestriction(NationGen ng, NationGenAssets assets)
	{
		super(ng, "Nation needs to not have any units of a race on the right box", "No unit of race");
		this.ng = ng;
		this.assets = assets;
		
		this.comboboxlabel = "Units to match:";
		for(Race r : assets.races)
			rmodel.addElement(r);
		
		this.comboboxoptions = ownoptions;
		
		
	}
	


	@Override
	public NationRestriction getRestriction() {
		NoUnitOfRaceRestriction res = new NoUnitOfRaceRestriction(ng, assets);
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.possibleRaceNames.add(chosen.getModel().getElementAt(i).name);
		
		res.comboselection = this.comboselection;
		return res;
	}
	
	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleRaceNames.size() == 0)
		{
			System.out.println("No units of race nation restriction has no races set!");
			return true;
		}
		
		if(comboselection == null)
			comboselection = "All";
		
		boolean pass = false;
		if(comboselection.equals("Troops") || comboselection.equals("All"))
			for(Unit u : n.generateTroopList())
				if(possibleRaceNames.contains(u.race.name))
					pass = true;
		
		if(!pass && (comboselection.equals("Commanders") || comboselection.equals("All")))
			for(Unit u : n.generateComList())
				if(possibleRaceNames.contains(u.race.name))
					pass = true;
		
		if(!pass && comboselection.equals("Sacred troops"))
			for(Unit u : n.generateUnitList("sacred"))
				if(possibleRaceNames.contains(u.race.name))
					pass = true;
		

		return !pass;
	}
	
	@Override
	public NationRestriction getInstanceOf() {
		return new NoUnitOfRaceRestriction(ng, assets);
	}
	
}
