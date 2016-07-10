package nationGen.restrictions;



import java.util.List;
import java.util.ArrayList;

import nationGen.NationGen;
import nationGen.entities.Race;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class SacredRaceRestriction extends TwoListRestriction<Race> {
	public List<String> possibleRaceNames = new ArrayList<String>();
	
	private NationGen ng;
	public SacredRaceRestriction(NationGen ng)
	{
		super(ng, "Nation needs to have at least one sacred unit of a race on the right box", "Sacred: Race");
		this.ng = ng;
		
		for(Race r : ng.races)
			rmodel.addElement(r);
		
		
	}
	


	@Override
	public NationRestriction getRestriction() {
		SacredRaceRestriction res = new SacredRaceRestriction(ng);
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.possibleRaceNames.add(chosen.getModel().getElementAt(i).name);
		return res;
	}
	
	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleRaceNames.size() == 0)
		{
			System.out.println("Sacred race nation restriction has no races set!");
			return true;
		}
		
		boolean pass = false;
		for(Unit u : n.generateUnitList("sacred"))
			if(possibleRaceNames.contains(u.race.name))
				pass = true;
		
	
		
		return pass;
	}
	
	@Override
	public NationRestriction getInstanceOf() {
		return new SacredRaceRestriction(ng);
	}
	
}
