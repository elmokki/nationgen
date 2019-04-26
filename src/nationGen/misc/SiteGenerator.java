package nationGen.misc;


import nationGen.NationGenAssets;
import nationGen.entities.Filter;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathDoubles;
import nationGen.magic.MagicPathInts;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class SiteGenerator {

	public static void generateSites(Nation n, NationGenAssets assets)
	{
		Random random = new Random(n.random.nextInt());
		
		int stuff = 0;
		
		MagicPathInts totalgems = getTotalGems(n);
		int gems = 0;
		for(MagicPath path : MagicPath.NON_HOLY)
		{
			gems += totalgems.get(path);
			if(totalgems.get(path) > 0)
				stuff++;
		}

		
		List<Unit> origmages = n.generateComList("mage");
		List<Unit> mages = new ArrayList<>();
		for(Unit u : origmages)
		{
			if(u.caponly)	
			{
				mages.add(u);
				stuff++;
			}
		}

		
		List<Unit> otherTroops = new ArrayList<>();
		List<Unit> otherComs = new ArrayList<>();
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
		

		/*
		for(ShapeChangeUnit su : n.specialmonsters)
			if(su.thisForm.tags.contains("caponly"))
			{
				Unit u = new Unit(su.id);
				otherTroops.add(u);
			}
		*/
		
		
		ChanceIncHandler chandler = new ChanceIncHandler(n);
		List<Filter> filters = ChanceIncHandler.retrieveFilters("sitefeatures", "default_sitefeatures", assets.miscdef, null, n.races.get(0));
		Filter f = chandler.getRandom(filters);
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
					MagicPathInts paths = s.getSitePathDistribution();
					paths.set(MagicPath.HOLY, -100);
					

					boolean done = false;
					while(paths.getHighestAmount() > 0 && !done)
					{
						MagicPath path = paths.getHighestPath();
						if(totalgems.get(path) > 0)
						{
							added++;
							s.gemMap.set(path, totalgems.get(path));
							
							gems -= totalgems.get(path);
							totalgems.set(path, 0);
							done = true;
						}
					
						paths.set(path, -1);
						
					}
					
					
					if(!done)
					{
						List<MagicPath> possibles = new ArrayList<>();
						for(MagicPath i : MagicPath.values())
						{
							if(totalgems.get(i) > 0)
								possibles.add(i);
						}
						
						MagicPath path = possibles.get(random.nextInt(possibles.size()));
						added++;
						s.gemMap.set(path, totalgems.get(path));
						gems -= totalgems.get(path);
						totalgems.set(path, 0);

					}
				}
				
	
			}
			stuff -= added;	
			n.sites.add(s);
		}
		
	}
	
	
	
	
	
	private static List<Unit> getRelatives(List<Unit> list, Unit u)
	{
		List<String> identifiers = u.tags.getAllValues("identifier").stream()
				.map(Arg::get)
				.collect(Collectors.toList());
		
		return list.stream()
				.filter(u2 -> identifiers.stream()
						.anyMatch(idt -> u2.tags.contains("identifier", idt)))
				.collect(Collectors.toList());
	}
	

	
	private static MagicPathInts getTotalGems(Nation n)
	{
		MagicPathInts gems = new MagicPathInts();
		
		MagicPathDoubles power = new MagicPathDoubles();
		
		List<Unit> list = n.generateComList();
		for(Unit u : list)
		{
			if(u.tags.getValue("schoolmage").filter(a -> a.getInt() == 3).isPresent())
			{
				
				MagicPathInts unitpower = null;
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
				
				power.addAll(unitpower);
			}
		}
		

			
		// Blood slaves or holy paths won't be counted.
		power.set(MagicPath.BLOOD, 0);
		power.set(MagicPath.HOLY, 0);
		
		int totalgems = 7 - n.era;

		for (int i = 0; i < totalgems; i++)
		{
			MagicPath high = power.getHighestPath();
			gems.add(high, 1);
			power.scale(high, 0.6);
		}


		return gems;
	}
}
