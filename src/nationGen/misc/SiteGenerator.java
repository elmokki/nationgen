package nationGen.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.nation.Nation;
import nationGen.units.Unit;



public class SiteGenerator {

	public static void generateSites(Nation n, NationGenAssets assets)
	{
		Random random = new Random(n.random.nextInt());
		
		int stuff = 0;
		
		HashMap<Integer, Integer> totalgems = getTotalGems(n);
		int gems = 0;
		for(int i = 0; i < 8; i++)
		{
			gems += totalgems.get(i);
			if(totalgems.get(i) > 0)
				stuff++;
		}

		
		List<Unit> origmages = n.generateComList("mage");
		List<Unit> mages = new ArrayList<Unit>();
		for(Unit u : origmages)
		{
			if(u.caponly)	
			{
					mages.add(u);
					stuff++;
				}
		}

		
		List<Unit> otherTroops = new ArrayList<Unit>();
		List<Unit> otherComs = new ArrayList<Unit>();
		for(Unit u : n.generateComList())
			if(u.caponly && !mages.contains(u))
			{
				otherComs.add(u);
				stuff++;
			}
		for(Unit u : n.generateTroopList())
			if(u.caponly)
			{
				otherTroops.add(u);
				stuff++;
			}
		

		/**
		for(ShapeChangeUnit su : n.specialmonsters)
			if(su.thisForm.tags.contains("caponly"))
			{
				Unit u = new Unit(su.id);
				otherTroops.add(u);
			}
		*/
		
		
		Filter f = null;
		ChanceIncHandler chandler = new ChanceIncHandler(n);
		List<Filter> filters = ChanceIncHandler.retrieveFilters("sitefeatures", "default_sitefeatures", assets.miscdef, null, n.races.get(0));
		f = chandler.getRandom(filters);
		if(!f.name.equals("nothing"))
			stuff++;
		
		int sites = (int)Math.ceil((double)stuff / 5);
		if(sites < 4 && (sites * 5 - stuff) / 20 > random.nextDouble())
			sites++;
		

		while(stuff > 0)
		{
			//int minstuff = StuffPerSite(sites, stuff);
			int added = 0;
			Site s = new Site();
			int max = 4 + random.nextInt(3);
			
			while(added < max && added < stuff)
			{
			

				// 0 = troop
				// 1 = com
				// 2 = mage
				// 3 = filter
				// 4 = gem
				
				
				int type = random.nextInt(4);
				

				if(type == 0 && otherComs.size() > 0)
				{
					Unit u = otherComs.get(random.nextInt(otherComs.size()));
					s.coms.add(u);
					otherComs.remove(u);
					added++;
			
					List<Unit> rels = getRelatives(otherTroops, u);
					while(added < max && rels.size() > 0)
					{
						Unit u2 = rels.get(random.nextInt(rels.size()));
						rels.remove(u2);
						otherTroops.remove(u2);
						added++;
						s.troops.add(u2);
					}
					
				}
				else if(type == 1 && otherTroops.size() > 0)
				{
					Unit u = otherTroops.get(random.nextInt(otherTroops.size()));
					s.troops.add(u);
					otherTroops.remove(u);
					added++;
			
					List<Unit> rels = getRelatives(otherComs, u);
					while(added < max && rels.size() > 0)
					{
						Unit u2 = rels.get(random.nextInt(rels.size()));
						rels.remove(u2);
						otherComs.remove(u2);
						added++;
						s.coms.add(u2);
					}
					
				}
				else if(type == 2 && mages.size() > 0)
				{
					Unit u = mages.get(random.nextInt(mages.size()));
					s.coms.add(u);
					mages.remove(u);
					added++;
			
					List<Unit> rels = getRelatives(mages, u);
					while(added < max && rels.size() > 0)
					{
						Unit u2 = rels.get(random.nextInt(rels.size()));
						rels.remove(u2);
						mages.remove(u2);
						added++;
						s.coms.add(u2);

					}
				}
				else if(type == 3 && f != null && !f.name.equals("nothing"))
				{
					s.appliedfilters.add(f);
					added++;
					f = null;
				}
			
				if(added < max && gems > 0)
				{
					double[] paths = s.getSitePathDistribution();
					paths[8] = -100;
					

					boolean done = false;
					while(paths[Generic.GetHighestPosition(paths)] > 0 && !done)
					{
						int pos = Generic.GetHighestPosition(paths);
						if(totalgems.get(pos) > 0)
						{
							added++;
							s.gemMap.put(pos, totalgems.get(pos));
							
							gems -= totalgems.get(pos);
							totalgems.put(pos, 0);
							done = true;
						}
					
						paths[pos] = -1;
						
					}
					
					
					if(!done)
					{
						List<Integer> possibles = new ArrayList<Integer>();
						for(Integer i : totalgems.keySet())
						{
							if(totalgems.get(i) > 0)
								possibles.add(i);
						}
						
						int pos = possibles.get(random.nextInt(possibles.size()));
						added++;
						s.gemMap.put(pos, totalgems.get(pos));
						gems -= totalgems.get(pos);
						totalgems.put(pos, 0);

					}
				}
				
	
			}
			stuff -= added;	
			n.sites.add(s);
		}
		
	}
	
	
	
	
	
	private static List<Unit> getRelatives(List<Unit> list, Unit u)
	{
		List<Unit> relatives = new ArrayList<Unit>();
		List<String> idts = new ArrayList<String>();
		

		
		for(String t : u.tags)
		{
			List<String> args = Generic.parseArgs(t);
			if(args.get(0).equals("identifier") && args.size() > 1)
				idts.add(args.get(1));
		}
		

		
		for(Unit u2 : list)
		{
			for(String idt : idts)
				if(u2.tags.contains("identifier "  + idt))
				{
					relatives.add(u2);
					break;
				}
		}
		return relatives;
		
	}
	

	
	private static HashMap<Integer, Integer> getTotalGems(Nation n)
	{
		HashMap<Integer, Integer> gems = new HashMap<Integer, Integer>();
		for(int i = 0; i < 8; i++)
			gems.put(i, 0);
		
		double[] power = new double[9];
		
		List<Unit> list = n.generateComList();
		for(Unit u : list)
		{
			if(Generic.containsTag(u.tags, "schoolmage") && Generic.getTagValue(u.tags, "schoolmage").equals("3"))
			{
				
				int[] unitpower = null;
				if(u.getMagicFilters().size() > 0)
				{
					int randoms = u.getMagicFilters().get(0).pattern.getRandoms(0.25);
					int nonrandoms = u.getMagicFilters().get(0).pattern.getPicks(1);
					int paths = u.getMagicFilters().get(0).pattern.getPathsAtleastAt(1);
					
					double level = 0.5;
					if(paths < 4)
					{
						level -= (4 - paths) * 0.05;  
					}
					
					double prop = (double)randoms/(double)(nonrandoms + randoms);
					
					if(prop >= level)
					{
						unitpower = u.getMagicPicks(true); 
					}
											
				}
				
				if(unitpower == null)	
					unitpower = u.getMagicPicks(); 
				
				for(int i = 0; i < unitpower.length; i++)
				{

					power[i] += unitpower[i];
				
				}
			}
		}
		

			
        // Blood slaves or holy paths won't be counted.
        power[7] = 0;
        power[8] = 0;


        
        double[] temppower = new double[9];

        for (int i = 0; i < 8; i++)
        {
            temppower[i] = power[i];
        }

        int totalgems = 7 - n.era;

        for (int i = 0; i < totalgems; i++)
        {
            int high = Generic.GetHighestPosition(power);
            gems.put(high, gems.get(high) + 1);
            power[high] = power[high] * 0.6;
        }


        return gems;
	}
}
