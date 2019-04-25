package nationGen.naming;


import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.misc.Args;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;



public class Namer {
	
	Nation n;
	public Namer()
	{
	
	}
	
	protected List<MagicPath> getHighestPaths(Nation n)
	{
		List<MagicPath> highestPaths = new ArrayList<>();
		
		// Get paths
		MagicPathInts paths = new MagicPathInts();
		for(Unit u : n.generateComList())
		{
			if(u.tags.containsName("schoolmage"))
			{
				MagicPathInts picks = u.getMagicPicks(true);
				for(MagicPath path : MagicPath.values())
				{
					if(paths.get(path) < picks.get(path))
						paths.set(path, picks.get(path));
				}
			}
		}
		paths.set(MagicPath.HOLY, 0);
		int highest = paths.getHighestAmount();
		
		for(MagicPath path : MagicPath.NON_HOLY)
			if(paths.get(path) == highest)
				highestPaths.add(path);
		
		return highestPaths;
	}
	
	protected List<MagicPath> getSitePaths(Unit u, Nation n)
	{
		MagicPathInts specpowers = new MagicPathInts();
		
		List<MagicPath> highestPaths = this.getHighestPaths(n);
		
		// Get unit specific powers
		for(Args args : u.tags.getAllArgs("sitepath"))
		{
			MagicPath path = MagicPath.fromInt(args.get(0).getInt());
			int power = args.get(1).getInt();
			specpowers.add(path, 3 * power);
		}
		

		// Mix in national magic
		for(MagicPath path : highestPaths)
		{
			if(specpowers.get(path) != 0)
				specpowers.scale(path, 2);
			else
				specpowers.add(path, 1);
		}
		

		
		// Get paths actually used for naming
		List<MagicPath> usedPaths = new ArrayList<>();
		int highest = specpowers.getHighestAmount();
		
		if(highest > 0)
			for(MagicPath path : MagicPath.values())
				if(specpowers.get(path) == highest)
					usedPaths.add(path);
		
		return usedPaths;
	}

	

}
