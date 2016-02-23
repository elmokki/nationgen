package nationGen.rostergeneration.montagtemplates;

import nationGen.NationGen;
import nationGen.entities.Pose;
import nationGen.nation.Nation;
import nationGen.rostergeneration.TroopGenerator;
import nationGen.units.Unit;

public class TroopMontagTemplate extends MontagTemplate {
	
	public Unit generateUnit(Unit u, Pose p, Nation n, NationGen ngen)
	{
		TroopGenerator tGen = new TroopGenerator(ngen, n);
	    Unit newunit = null;
	    int tries = 0;
	    while(tries < 100 && newunit == null)
	    {
	    	tries++;
	    	newunit = tGen.generateUnit(p, u.race);
	    }
	    
	    return newunit;
	
	}
}
