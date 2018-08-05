package nationGen.restrictions;

import java.util.List;
import java.util.ArrayList;

import nationGen.NationGenAssets;
import nationGen.entities.Race;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class SacredRaceRestriction extends TwoListRestriction<Race> {
	public List<String> possibleRaceNames = new ArrayList<String>();
	
	private NationGenAssets assets;
	public SacredRaceRestriction(NationGenAssets assets)
	{
		super("Nation needs to have at least one sacred unit of a race on the right box", "Sacred: Race");
		this.assets = assets;
		
		for(Race r : assets.races)
		{
			rmodel.addElement(r);
		}
	}

	@Override
	public NationRestriction getRestriction() {
		SacredRaceRestriction res = new SacredRaceRestriction(assets);
		for(int i =0; i < chosen.getModel().getSize(); i++)
		{
			res.possibleRaceNames.add(chosen.getModel().getElementAt(i).name);
		}
		
		return res;
	}
	
	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleRaceNames.size() == 0)
		{
			System.out.println("Sacred race nation restriction has no races set!");
			return true;
		}
		
		for(Unit u : n.generateUnitList("sacred"))
		{
			if(possibleRaceNames.contains(u.race.name))
			{
				return true;
			}
		}	
		return false;
	}

    @Override
    public RestrictionType getType()
    {
        return RestrictionType.SacredRace;
    }	
}
