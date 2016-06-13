package nationGen.naming;

import java.util.ArrayList;
import java.util.List;


import nationGen.nation.Nation;
import nationGen.units.Unit;



public class Namer {
	
	Nation n;
	public Namer()
	{
	
	}
	
	protected List<Integer> getHighestPaths(Nation n)
	{
		List<Integer> highestPaths = new ArrayList<Integer>();
		
		// Get paths
		double[] paths = new double[9];
		double highest = 0;
		for(Unit u : n.generateComList())
		{
			for(String tag : u.tags)
				if(tag.startsWith("schoolmage"))
				{
					int[] p = u.getMagicPicks();
					for(int i = 0; i < 9; i++)
					{
						if(paths[i] < p[i])
							paths[i] = p[i];
						if(p[i] > highest && i != 8)
							highest = p[i];
					}
				}
		}
		for(int i = 0; i < 8; i++)
			if(paths[i] == highest)
				highestPaths.add(i);
		
		return highestPaths;
	}
	
	protected List<Integer> getSitePaths(Unit u, Nation n)
	{
		double[] specpowers = new double[9];
		
		List<Integer> highestPaths = this.getHighestPaths(n);
		
		// Get unit specific powers
		for(String tag : u.tags)
		{
			if(tag.startsWith("sitepath"))
			{
				int index = Integer.parseInt(tag.split(" ")[1]);
				int power = Integer.parseInt(tag.split(" ")[2]);
				specpowers[index] += 3 * power;
			}
		}
		

		// Mix in national magic
		for(Integer i : highestPaths)
		{
			if(specpowers[i] != 0)
				specpowers[i] = specpowers[i] * 2;
			else
				specpowers[i] += 1;
		}
		

		
		// Get paths actually used for naming
		List<Integer> usedPaths = new ArrayList<Integer>();
		double highest = 0;
		for(double d : specpowers)
			if(d > highest)
				highest = d;
		
		if(highest > 0)
			for(int i = 0; i < 9; i++)
				if(specpowers[i] == highest)
					usedPaths.add(i);
		
		return usedPaths;
	}

	

}
