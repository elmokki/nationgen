package nationGen.restrictions;



import java.util.List;
import java.util.ArrayList;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.entities.Race;
import nationGen.nation.Nation;

public class NoPrimaryRaceRestriction extends TwoListRestriction<Race>  {
	public List<String> possibleRaceNames = new ArrayList<String>();
	
	
	private NationGen ng;
	private NationGenAssets assets;
	
	public NoPrimaryRaceRestriction(NationGen ng, NationGenAssets assets)
	{
		super(ng, "Nation needs to not have one of the races in the right box as primary race", "No primary race");
		this.ng = ng;
		this.assets = assets;
		
		for(Race r : assets.races)
		{
            if (!r.tags.contains("secondary")) 
            {
                rmodel.addElement(r);
            }   
		}
	}
	

	@Override
	public NationRestriction getRestriction() {
		NoPrimaryRaceRestriction res = new NoPrimaryRaceRestriction(ng, assets);
		for(int i =0; i < chosen.getModel().getSize(); i++)
			res.possibleRaceNames.add(chosen.getModel().getElementAt(i).name);
		return res;
	}


	
	@Override
	public boolean doesThisPass(Nation n) {
		if(possibleRaceNames.size() == 0)
		{
			System.out.println("No Primary Race nation restriction has no races set!");
			return true;
		}
		return !possibleRaceNames.contains(n.races.get(0).name);
	}

    @Override
    public RestrictionType getType()
    {
        return RestrictionType.NoPrimaryRace;
    }

	
}
