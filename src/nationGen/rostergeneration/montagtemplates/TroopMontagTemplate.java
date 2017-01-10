package nationGen.rostergeneration.montagtemplates;

import nationGen.NationGen;
import nationGen.entities.Pose;
import nationGen.nation.Nation;
import nationGen.rostergeneration.TroopGenerator;
import nationGen.units.Unit;

public class TroopMontagTemplate extends MontagTemplate {
	
	private TroopGenerator tGen = null;
	
	public TroopMontagTemplate(NationGen ngen, Nation n, TroopGenerator tGen)
	{
			this.tGen = tGen; //new TroopGenerator(ngen, n);
	}


	
	public Unit generateUnit(Unit u, Pose p)
	{
	    Unit newunit = null;
	    int tries = 0;
	    while(tries < 100 && newunit == null)
	    {
	    	tries++;
	    	newunit = tGen.unitGen.generateUnit(u.race, p);
	    	handleFilterInheritance(u, newunit);
	    	newunit = tGen.equipUnit(newunit);

	    }
	    
	    return newunit;
	
	}
}
