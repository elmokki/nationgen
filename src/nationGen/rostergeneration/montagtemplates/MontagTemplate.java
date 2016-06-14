package nationGen.rostergeneration.montagtemplates;

import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class MontagTemplate {
	

	
	public Unit generateUnit(Unit u, Pose p)
	{
		return null;
	}
	
	protected void handleFilterInheritance(Unit from, Unit to)
	{
		for(Filter f : from.appliedFilters)
		{
			if(!f.tags.contains("not_montag_inheritable") && !to.appliedFilters.contains(f))
			{
				to.appliedFilters.add(f);
			}
		}
	}
}
