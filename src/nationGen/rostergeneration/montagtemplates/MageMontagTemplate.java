package nationGen.rostergeneration.montagtemplates;

import java.util.ArrayList;
import java.util.List;

import nationGen.NationGen;
import nationGen.entities.Pose;
import nationGen.magic.MageGenerator;
import nationGen.nation.Nation;
import nationGen.rostergeneration.TroopGenerator;
import nationGen.units.Unit;

public class MageMontagTemplate extends MontagTemplate {
	
	private int tier;
	public MageMontagTemplate(int tier)
	{
		this.tier = tier;
	}
	
	public Unit generateUnit(Unit u, Pose p, Nation n, NationGen ngen)
	{
		MageGenerator mGen = new MageGenerator(ngen, n);
	    Unit newunit = null;
	    int tries = 0;
	    while(tries < 100 && newunit == null)
	    {
	    	newunit = mGen.unitGen.generateUnit(u.race, p);
	    	handleFilterInheritance(u, newunit);
	    	mGen.equipBase(newunit, 1);
	    	
	    	
	    }
	    
	    if(tier > 0 && tier < 4)
	    {
	    	newunit.tags.add("schoolmage " + tier);
	    	newunit.tags.add("identifier schoolmage");

	    }

	    List<Unit> mages = new ArrayList<Unit>();
	    mages.add(newunit);
	    
		mGen.resolveAge(mages);
		MageGenerator.determineSpecialLeadership(u, false);

	    
	    return newunit;
	
	}
}
