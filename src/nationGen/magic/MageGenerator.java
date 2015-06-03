package nationGen.magic;



import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.AbilityTemplate;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.MagicFilter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.rostergeneration.CommanderGenerator;
import nationGen.rostergeneration.TroopGenerator;
//import nationGen.rostergeneration.TroopGenerator.Template;
import nationGen.units.Unit;

public class MageGenerator extends TroopGenerator {
	
	boolean varyHat;
	public List<MagicPattern> possiblePatterns = new ArrayList<MagicPattern>();
	
	public MageGenerator(NationGen g, Nation n) {
		super(g, n);
		
		loadPatterns();
		varyHat = n.random.nextBoolean();
	}

	
	/**
	 * Lists common magic paths of given units
	 * @param all given units
	 * @param minlevel minimum level to consider
	 * @return list of integers signifying paths
	 */
	public static List<Integer> findCommonPaths(List<Unit> all, int minlevel)
	{
		List<Integer> list = new ArrayList<Integer>();
		for(int i = 0; i < 9; i++)
		{
			boolean ok = true;
			for(Unit u : all)
			{

				
				int[] p = u.getMagicPicks(false);
				if(p[i] < minlevel)
				{
					ok = false;
				}
				
			}
			
			if(ok)
				list.add(i);
		}

		
		return list;
	}
	
	/**
	 * Lists common magic paths of given units
	 * @param all given units
	 * @return list of integers signifying paths
	 */
	public static List<Integer> findCommonPaths(List<Unit> all)
	{
		return findCommonPaths(all, 1);
	}
	
	/**
	 * Finds the paths that distinguish given mage from mages in a list
	 * @param u The mage to distinguish
	 * @param all The mages to distinguish from
	 * @return List of integers signifying paths
	 */
	public static List<Integer> findDistinguishingPaths(Unit u, List<Unit> all)
	{
		int[] paths = u.getMagicPicks(false);
		
		
		List<Integer> list = new ArrayList<Integer>();
		for(Unit u2 : all)
		{
			if(u2 == u)
				continue;
			
			int[] p2 = u2.getMagicPicks(false);
			for(int i = 0; i < 9; i++)
			{
				if((paths[i] > p2[i]) && !list.contains(i))
					list.add(i);
			}
		}
		
		if(all.size() == 1)
		{
			for(int i = 0; i < 9; i++)
			{
				if((paths[i] > 0))
					list.add(i);
			}
		}
		
		return list;
	}
	
	private List<Integer> getShuffledPrios(List<Unit> units)
	{
		List<Filter> orig = new ArrayList<Filter>();
		List<Integer> newlist = new ArrayList<Integer>();
		
		
		
		double[] old = getAllPicks(units, true);
		for(int i = 0; i < 8; i++)
		{
			Filter f = new Filter(this.nationGen);
			f.name = Generic.integerToPath(i);
			if(old[i] > 0)
			{
				f.basechance = (double)1 / (3 * old[i]);
				if(old[i] > 3)
					f.basechance = 0;
			}
		
			
			orig.add(f);
		}
		
		while(orig.size() > 0)
		{
			Filter i = Filter.getRandom(nation.random, orig);
			orig.remove(i);
			newlist.add(Generic.PathToInteger(i.name));
		}
		
		return newlist;
		
	}
	
	private List<Integer> getPrios(Unit u)
	{
		List<Filter> orig = new ArrayList<Filter>();
		List<Integer> newlist = new ArrayList<Integer>();
		
		
		
		
		for(int i = 0; i < 8; i++)
		{
			Filter f = new Filter(this.nationGen);
			f.name = Generic.integerToPath(i);
			
			// RACE/POSE EXTRA MAGIC PRIORITIES!
			List<String> source = null;
			if(Generic.getTagValue(u.pose.tags, "magicpriority") != null)
				source = u.pose.tags;
			else if(Generic.getTagValue(u.race.tags, "magicpriority") != null)
				source = u.race.tags;
			
			if(source != null)
			{
				for(String tag : source)
				{
					List<String> args = Generic.parseArgs(tag);
					if(args.get(0).equals("magicpriority") && args.size() > 2)
					{
						if(Generic.PathToInteger(args.get(1)) == i)
						{
							f.basechance = Double.parseDouble(args.get(2));
						}
					}
				}
			}
			
	
			
			orig.add(f);
		}
		
		while(orig.size() > 0)
		{
			Filter i = Filter.getRandom(nation.random, orig);
			orig.remove(i);
			newlist.add(Generic.PathToInteger(i.name));
		}
		
		return newlist;

	}
	
	private void applyStats(Unit u)
	{
		u.commands.add(new Command("#att", "-1"));
		u.commands.add(new Command("#def", "-1"));
		
		int mr = 2 + (int)Math.round(u.getMagicAmount(10));

		
		
		int unitmr = 0;	
		for(Command c : u.getCommands())
		{
			if(c.command.equals("#mr"))
			{

				unitmr += Integer.parseInt(c.args.get(0));
			}
		}
		
		
		unitmr -= 10;
		while(unitmr > 0)
		{
			
			if(unitmr == 1)
				unitmr--;
			else if(unitmr == 2)
			{
				mr--;
				unitmr--;
			}
			else if(unitmr == 3)
			{
				unitmr -= 2;
				mr--;
			}
			else
			{
				unitmr--;
				mr--;
			}
				
		}
		
		if(mr > 0)
		{
			u.commands.add(new Command("#mr", "+" + mr));
		}
	
	}


	
	public List<Unit> generate()
	{
		

		List<List<Unit>> mages = generateBases();
		
		int primaries = mages.get(2).size();
		int secondaries = mages.get(1).size();
		int tertiaries = mages.get(0).size();
		
		// Decide if primaries should be cap only
		boolean caponlyprimaries = false;
		

		if(secondaries > primaries)
			caponlyprimaries = true;	
		if(primaries > 0 && secondaries > 0 && nation.random.nextDouble() < 0.70)
			caponlyprimaries = true;
		if(primaries + secondaries + tertiaries >= 5)
			caponlyprimaries = true;


		

		

		
		List<MagicPattern> available = getPatternsForTier(3, primaries);
		
		List<MagicPattern> primarypatterns = new ArrayList<MagicPattern>();
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation);
		MagicPattern primaryPattern = Entity.getRandom(nation.random, chandler.handleChanceIncs(available));


		// Primaries
		List<Integer> prio = this.getPrios(mages.get(2).get(0));
		for(int i = 0; i < primaries; i++)
		{						
			primarypatterns.add(primaryPattern);
				
		}
		

		// Secondaries

		// varySecs = true -> mages get different paths but same kind of pattern
		boolean varySecs = true;
		List<MagicPattern> secs = getPatternsWithPicks(this.getPatternsForTier(2, primaries), 0, primaryPattern.getPicks(0.25) - 1);
		List<MagicPattern> secondarypatterns = new ArrayList<MagicPattern>();

		
		if(primaries == secondaries && nation.random.nextDouble() < 0.35)
			secs = getPatternsWithSpread(secs, 0, Math.max(0, primaryPattern.getPathsAtleastAt(1)));
		
		// A setting here is probably a good idea as well.
		if(secondaries > 1)
		{
			int lowlim = 2;
			while(secs.size() == 0 || lowlim == 2 && lowlim < 12)
			{
				secs = getPatternsWithSpread(secs, 0, lowlim);
				lowlim++;
			}
		}

		if(secs.size() == 0)
			System.out.println("WARNING: No secondary patterns when primary has " + primaryPattern.getPicks(50) + " picks. Debug info: " + primaries + "/" + secondaries);
		



		// More secondaries than primaries only if there's 1 secondary (10.1.2014.)
		if(secondaries > primaries && nation.random.nextDouble() < 0.6 && secs.size() >= secondaries)
		{
			varySecs = false;
			for(int i = 0; i < secondaries; i++)
			{			
				
				MagicPattern p = Entity.getRandom(nation.random, chandler.handleChanceIncs(secs));
				secs.remove(p);
				secondarypatterns.add(p);
			}
		}
		else
		{
			MagicPattern p = Entity.getRandom(nation.random, chandler.handleChanceIncs(secs));
			for(int i = 0; i < secondaries; i++)
			{						
				secondarypatterns.add(p);
			}
		}
		

		// Tertiary
		
		
		int maxPicks = primaryPattern.getPicks(0.25) - 1;
		for(MagicPattern p : secondarypatterns)
			if(p.getPicks(0.25) - 1 < maxPicks)
			{
				maxPicks = p.getPicks(0.25) - 1;

			}
		int maxSpread = 8;
		if(tertiaries == secondaries)
			for(MagicPattern p : secondarypatterns)
				if(p.getPathsAtleastAt(1) < maxSpread)
					maxSpread = p.getPathsAtleastAt(1);
		
		
		
		List<MagicPattern> terts = getPatternsWithPicks(this.getPatternsForTier(1, primaries), 0, maxPicks);
		List<MagicPattern> tertiarypatterns = new ArrayList<MagicPattern>();
		
		MagicPattern p = Entity.getRandom(nation.random, chandler.handleChanceIncs(terts));
		for(int i = 0; i < tertiaries; i++)
		{						
			tertiarypatterns.add(p);
		}
		

		List<List<MagicPattern>> all = new ArrayList<List<MagicPattern>>();
		all.add(tertiarypatterns);
		all.add(secondarypatterns);
		all.add(primarypatterns);
		
		double leaderrandom1 = nation.random.nextDouble();
		double leaderrandom2 = nation.random.nextDouble();
		double slowrecrand = nation.random.nextDouble();
		

		for(int i = 2; i >= 0; i--)
			for(int j = 0; j < mages.get(i).size(); j++)
			{
	
				List<Unit> parents = null;
				if(i < 2)
					parents = mages.get(i + 1);
				
				List<Integer> derp;
				if(i == 1 && !varySecs)
				{
					derp = prio;
				}
				else if(parents != null && mages.get(i).size() == parents.size() && parents.size() > 1)
				{
					derp = varyAt(getVaryPoint(all.get(i).get(j), mages.get(i).size()), prio, j, parents.get(j), parents);
				}
				else
				{
					derp = varyAt(getVaryPoint(all.get(i).get(j), mages.get(i).size()), prio, j);
				}
				
				MagicFilter f = getMagicFilter(derp, all.get(i).get(j), mages.get(i).size());
				mages.get(i).get(j).appliedFilters.add(f);
				

				
				mages.get(i).get(j).name.setType(all.get(i).get(j).toString(derp));
				
				mages.get(i).get(j).commands.add(new Command("#gcost", "+" + this.priceMage(mages.get(i).get(j))));
				
				
				
				if(i == 2 && leaderrandom1 > 0.9)
					mages.get(i).get(j).commands.add(new Command("#goodleader"));
				else if(i == 2 && leaderrandom1 > 0.6){/*do nothing*/}
				else if(i == 1 && leaderrandom1 > 0.9 && leaderrandom2 > 0.5){/*do nothing*/}
				else
					mages.get(i).get(j).commands.add(new Command("#poorleader"));

					
					
				// SETTING HERE FOR MAGE INSANITY

				
				
				double slowrecmod = mages.get(i).get(j).getMagicAmount(0.25) / 8 + 0.25;
			
				int highpicks = all.get(i).get(j).getPathsAtleastAt(3, 0.25);
				if(highpicks > 2)
					slowrecmod *= 2;
				
				if(mages.get(i).get(j).getMagicAmount(0.25) <= 4)
					slowrecmod = 0;
	
				
				applyStats(mages.get(i).get(j));
				
				
				if(caponlyprimaries && i == 2)
				{
					mages.get(i).get(j).caponly = true;
				}
				else if(i == 2)
					slowrecmod *= 1.5;

				if(slowrecrand < slowrecmod && i == 2)
				{
					mages.get(i).get(j).commands.add(new Command("#slowrec"));
				}


			}


		// Put to one unit list
		List<Unit> list = new ArrayList<Unit>();
		for(List<Unit> l : mages)
		{
			this.resolveAge(l);
			list.addAll(l);
		}

		tagAll(list, "identifier schoolmage");
		

		// Diagnostics
		double[] picks = getAllPicks(list, true);
		double[] norand_picks = getAllPicks(list, false);
		
		int diversity = 0;
		int max = 0;
		int atmax = 0;
		for(int i = 0; i < 9; i++)
		{
			if(picks[i] > 1 || (norand_picks[i] == 1 && (i == 7 && i == 4)))
				diversity++;
			
			if(picks[i] > max)
			{
				max = (int)picks[i];
				atmax = 1;
			}
			else if(picks[i] == max)
			{
				atmax++;
			}
		}
		

		
		// Filters for main mages

		
		List<Filter> filters = ChanceIncHandler.retrieveFilters("magefilters", "default_magefilters", nationGen.filters, list.get(list.size() - 1).pose, list.get(list.size() - 1).race);
		double mod = 0.25;
		if(diversity < 3)
			mod += 0.1;
		if(diversity < 4)
			mod += 0.1;
		
		if(max > 4)
			mod -= 0.1;
		if(max < 4)
			mod += 0.1;
		if(atmax < 2 && max < 4)
			mod += 0.1;
		if(atmax < 3 && max < 4)
			mod += 0.1;
		
			
		if(picks[4] > 0 && picks[3] > 0)
			mod *= 0.75;
		if(picks[4] > 2)
			mod *= 0.75;
		else if(norand_picks[4] > 0)
			mod *= 0.8;
		if(picks[7] > 2)
			mod *= 0.75;
		

		
		int power = 0;
		if(nation.random.nextDouble() < mod * 2)
		{
			power++;
			if(nation.random.nextDouble() < mod)
			{
				power++;
				if(nation.random.nextDouble() < mod / 2)
				{
					power++;
					if(nation.random.nextDouble() < mod / 4)
					{
						power++;
					}
				}
			}
		}
		//this.applyFilters(list, power, filters);
		

		
		// Extra mage
		boolean ok = false;
		if(diversity < 4 && ((norand_picks[4] == 0 && norand_picks[7] == 0) && nation.random.nextDouble() < 0.8))
		{
			ok = true;
		}
		if(max < 4 && norand_picks[4] == 0 && norand_picks[7] == 0 && diversity < 4) 
		{
			ok = true;
		}
		if(diversity < 5 && norand_picks[4] == 0 && norand_picks[7] == 0 && nation.random.nextDouble() < 0.1)
		{
			ok = true;
		}
		if(max < 4 && atmax < 2 && norand_picks[4] == 0 && norand_picks[7] == 0 && nation.random.nextDouble() < 0.5)
		{
			ok = true;
		}
		if(norand_picks[4] == 0 && norand_picks[7] == 0 && diversity < 6 && nation.random.nextDouble() < 0.05)
		{
			ok = true;
		}
		if(norand_picks[4] == 0 && norand_picks[7] == 0 && atmax == 1 && diversity < 4 && nation.random.nextDouble() < 0.65)
		{
			ok = true;
		}
		if(norand_picks[4] < 2 && norand_picks[7] < 2 && atmax == 1 && diversity < 4 && nation.random.nextDouble() < 0.35)
		{
			ok = true;
		}
		

		if(ok)
		{
			List<Unit> stuff = this.generateExtraMages(primaries, this.getShuffledPrios(list));
			this.resolveAge(stuff);
			this.tagAll(stuff, "extramage");
			this.priceMage(stuff.get(0));
			this.applyStats(stuff.get(0));


			
			stuff.get(0).caponly = nation.random.nextBoolean();
			if((primaries > 1 || secondaries > 1) && caponlyprimaries)
				stuff.get(0).caponly = true;
			else if(!caponlyprimaries)
				stuff.get(0).caponly = true;
			

			
			if(stuff.get(0).caponly == false && nation.random.nextDouble() > 0.5)
				stuff.get(0).commands.add(new Command("#slowrec"));
		
			// filters
			/*
			power = nation.random.nextInt(3) + 2;

			this.applyFilters(stuff, power, filters);
			*/
			list.addAll(0, stuff);
			
			
		}


		
		return list;
	}
	
	
	
	
	public void applyFilters()
	{		
		List<Unit> list = nation.generateComList("mage");
		
		// Diagnostics
		double[] picks = getAllPicks(list, true);
		double[] norand_picks = getAllPicks(list, false);
		
		int diversity = 0;
		int max = 0;
		int atmax = 0;
		for(int i = 0; i < 9; i++)
		{
			if(picks[i] > 1 || (norand_picks[i] == 1 && (i == 7 && i == 4)))
				diversity++;
			
			if(picks[i] > max)
			{
				max = (int)picks[i];
				atmax = 1;
			}
			else if(picks[i] == max)
			{
				atmax++;
			}
		}
		
		
		
		// Filters for main mages
	
		
		List<Filter> filters = ChanceIncHandler.retrieveFilters("magefilters", "default_magefilters", nationGen.filters, list.get(list.size() - 1).pose, list.get(list.size() - 1).race);
		double mod = 0.25;
		if(diversity < 3)
			mod += 0.2;
		if(diversity < 4)
			mod += 0.15;
		
		if(max > 4)
			mod -= 0.25;
		if(max < 4)
			mod += 0.1;
		if(atmax < 2 && max < 4)
			mod += 0.2;
		if(atmax < 3 && max < 4)
			mod += 0.1;
		
			
		if(picks[4] > 0 && picks[3] > 0)
			mod *= 0.75;
		if(picks[4] > 2)
			mod *= 0.75;
		else if(norand_picks[4] > 0)
			mod *= 0.8;
		if(picks[7] > 2)
			mod *= 0.75;
		

		int power = 0;
		
		if(nation.random.nextDouble() < mod * 3)
		{
			power++;
			if(nation.random.nextDouble() < mod * 2)
			{
				power++;
				if(nation.random.nextDouble() < mod)
				{
					power++;
					if(nation.random.nextDouble() < mod)
					{
						power++;
						if(nation.random.nextDouble() < mod / 2)
						{
							power++;
							if(nation.random.nextDouble() < mod / 2)
							{
								power++;
							}
						}
					}
				}
			}
		}
		

		if(power > 0 && nation.random.nextDouble() > 0.5)
			power++;
		
		
		Unit extra = null;
		for(Unit u : list)
			if(!u.tags.contains("identifier schoolmage"))
			{
				extra = u;
				break;
			}
		list.remove(extra);
		
		this.applyFilters(list, power, filters);
		
		if(extra != null)
		{
			list.clear();
			list.add(extra);
			power = nation.random.nextInt(3) + 2; // 2 to 4
			if(nation.random.nextBoolean())
				power += nation.random.nextInt(4); // 0 to 3;
			
			this.applyFilters(list, power, filters);
		}
		
		// priests
		list = nation.generateComList("priest");
		if(list.size() > 0)
		{
			filters = ChanceIncHandler.retrieveFilters("priestfilters", "default_priestfilters", nationGen.filters, list.get(0).pose, list.get(0).race);
			power = 0;
			int maxStrength = 1;
			for(Unit u : list)
				for(int i = 3; i > 0; i--)
				{
					if(u.tags.contains("priest " + i) && i > maxStrength)
					{
						maxStrength = i;
						break;
					}
				}
			
			if(nation.random.nextDouble() < 0.15 + maxStrength * 0.075)
			{
				power++;
				if(nation.random.nextDouble() < 0.1 + maxStrength * 0.05)
				{
					power++;
					if(nation.random.nextDouble() < 0.1 + maxStrength * 0.05)
					{
						power++;
						if(nation.random.nextDouble() < 0.1 + maxStrength * 0.05)
						{
							power++;
						}
					}
				}
			}
			
			if(power == 0 && nation.random.nextDouble() > 0.85)
				power = nation.random.nextInt(3) + 2; // 2 to 3 
				

			this.applyFilters(list, power, filters);

		}

		
	}
	public static double[] getAllPicks(List<Unit> units, boolean randoms)
	{
		double[] picks = {0, 0, 0, 0, 0, 0, 0, 0, 0};
		
		for(Unit u : units)
		{
			int[] p = u.getMagicPicks(randoms);
		

			
			for(int i = 0; i < 9; i++)
			{
				if(picks[i] < p[i])
					picks[i] = p[i];
			}
		}
		

		return picks;
	}
	
	public MagicFilter getMagicFilter(List<Integer> prio, MagicPattern p, int primaries)
	{
		MagicFilter f = new MagicFilter(nationGen);
		f.prio = prio;
		f.pattern = p;
		f.name = "MAGICPICKS";
		return f;
	}
	
	private int getVaryPoint(MagicPattern p, int primaries)
	{
		if(primaries <= p.getPathsAtleastAt(1, 100) && (nation.random.nextDouble() > 0.8 && p.getPathsAtleastAt(3, 0) < 2))
		{
			for(int i = 3; i > 0; i--)
			{
				if(p.getPathsAt(i) > 0)
					return (p.getPathsAtleastAt(i, 100) - 1);	
			}
		}
		
			
		
		return (int)Math.max(0, (p.getPathsAtleastAt(1, 100) - 1));
	}
	
	private List<Integer> varyAt(int point, List<Integer> prio, int times)
	{
		List<Integer> nprio = new ArrayList<Integer>();
		nprio.addAll(prio);
		int old = nprio.get(point);
		nprio.set(point, nprio.get(point + times));
		nprio.set(point + times, old);
		return nprio;
	}
	
	private List<Integer> varyAt(int point, List<Integer> prio, int times, Unit parent, List<Unit> parents)
	{
		List<Integer> nprio = new ArrayList<Integer>();
		nprio.addAll(prio);

		
		List<Integer> dpaths = findDistinguishingPaths(parent, parents);
		
		if(dpaths.size() == 0)
			return varyAt(point, prio, times);
		
		int index = nprio.indexOf(dpaths.get(0));
		int old = nprio.get(point);
		nprio.set(point, dpaths.get(0));
		nprio.set(index, old);
		
		return nprio;
	}
	
	
	public List<MagicPattern> getPatternsForTier(int tier, int primaries)
	{
		List<MagicPattern> available = getPatternsOfPrimaries(getPatternsOfLevel(possiblePatterns, tier), primaries);
		if(available.size() == 0)
			System.out.println("WARNING: No level " + tier + " magic patterns for " + primaries + " primary mage setup.");
		
		
		
		return available;
	}
	
	public static List<MagicPattern> getPatternsOfLevel(List<MagicPattern> orig, int level)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		
		for(MagicPattern p : orig)
		{
			if(p.levels.size() == 0)
				list.add(p);
			else if(p.levels.contains(level))
				list.add(p);
		}
		
		return list;
	}
	
	
	public static List<MagicPattern> getPatternsOfPrimaries(List<MagicPattern> orig, int level)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		
		for(MagicPattern p : orig)
		{
			if(p.primaries.size() == 0)
				list.add(p);
			else if(p.primaries.contains(level))
				list.add(p);
		}
		
		return list;
	}
	
	
	public static List<MagicPattern> getPatternsWithSpread(List<MagicPattern> orig, int min, int max)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		for(MagicPattern p : orig)
		{
			if(p.getPathsAtleastAt(1, 1) >= min && p.getPathsAtleastAt(1, 1) <= max)
				list.add(p);
		}
		return list;
	}
	
	public static List<MagicPattern> getPatternsWithPicks(List<MagicPattern> orig, int min, int max)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		for(MagicPattern p : orig)
		{
			if(p.getPicks(0.25) >= min && p.getPicks(0.25) <= max )
				list.add(p);
		}
		return list;
	}
	
	public static List<MagicPattern> getHighestLevel(List<MagicPattern> orig, int min, int max)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		for(MagicPattern p : orig)
		{
			if(p.getHighestReachable(0.25) >= min && p.getHighestReachable(0.25) <= max)
				list.add(p);
		}
		return list;
	}
	
	public static List<MagicPattern> getPathsAtHighest(List<MagicPattern> orig, int min, int max)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		for(MagicPattern p : orig)
		{
			if(p.getPicksAtHighest() <= max && p.getPicksAtHighest() >= min)
				list.add(p);
		}
		return list;
	}
	
	
	private void loadPatterns()
	{
		// Patterns
		List<String> patternsets = new ArrayList<String>();
		for(String tag : nation.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("magicpatterns") && args.size() > 1)
				patternsets.add(args.get(1));
		}
		
		if(patternsets.size() == 0)
		{
			patternsets.add("defaultprimary");
			patternsets.add("defaultsecondary");
			patternsets.add("defaulttertiary");
		}
		
		for(String set : patternsets)
		{
			List<MagicPattern> fetch = nationGen.patterns.get(set);;
			if(fetch == null)
				System.out.println("WARNING: Magic path pattern set " + set + " could not be found.");
			else
				possiblePatterns.addAll(fetch);
		}
	}
	
	private List<Unit> generateExtraMages(int primaries)
	{
		return generateExtraMages(primaries, null);
	}
	
	private List<Unit> generateExtraMages(int primaries, List<Integer> prio)
	{
		Race race = nation.races.get(0);
		
		double bonussecchance = 1;
		if(Generic.containsTag(race.tags, "secondaryracemagemod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(race.tags, "secondaryracemagemod"));
		if(Generic.containsTag(nation.races.get(1).tags, "primaryracemagemod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(nation.races.get(1).tags, "primaryracemagemod"));
		
		if((nation.random.nextDouble() < 0.075 * bonussecchance) && nation.races.get(1).hasRole("mage"))
			race = nation.races.get(1);
		else if(!nation.races.get(0).hasRole("mage"))
			race = nation.races.get(1);

		List<Unit> bases = generateNew("mage", race, 1, 2, race == nation.races.get(0));
		
		List<MagicPattern> available = getPatternsForTier(0, primaries);
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation);
		MagicPattern pattern = Entity.getRandom(nation.random, chandler.handleChanceIncs(available));

		
		if(prio == null)
		{
			prio = this.getPrios(bases.get(0));
		}
	

		MagicFilter f = new MagicFilter(nationGen);
		f.prio = prio;
		f.pattern = pattern;
		
		bases.get(0).color = nation.colors[2];
		bases.get(0).appliedFilters.add(f);
		this.priceMage(bases.get(0));
		
		return bases;

	}
	
	public List<Unit> generatePriests()
	{		

		List<List<Unit>> all = nation.getMagesInSeparateLists();
		List<Unit> extras = all.get(3);
		
		
		Random r = nation.random;
		
		int maxStrength = 1;
		if(r.nextDouble() > 0.50)
		{
			maxStrength++;
			if(r.nextDouble() > 0.5)
				maxStrength++;
		}
		
		boolean magePriests = r.nextBoolean();
		boolean compensationMagePriests = r.nextBoolean();
		
		int sacredMageTiers = 0;
		if(r.nextDouble() < 0.5)
		{
			sacredMageTiers++;
			if(r.nextDouble() < 0.5)
			{
				sacredMageTiers++;
				if(r.nextDouble() < 0.5)
					sacredMageTiers++;

			}

		}
		
		if(magePriests && maxStrength < 3 && r.nextDouble() < 0.5)
			sacredMageTiers++;
			
	
		if(r.nextBoolean())
			sacredMageTiers = 0;
		
		
		if(extras.size() == 0)
			compensationMagePriests = false;
		
		
		
		// Adjust color
		Color priestcolor = nation.colors[1];
		if(!magePriests || (priestcolor.getRed() + priestcolor.getBlue() + priestcolor.getGreen() < 660))
		{
			priestcolor = priestcolor.brighter();
		}
		else
		{
			priestcolor = priestcolor.darker();
		}
		
		List<Unit> priests = new ArrayList<Unit>();
		
		int currentStrength = maxStrength;
		int atTier = 0;

		boolean doneWithMages = false;
		
		int priestsFrom = 0;
		if(magePriests && maxStrength > 1 && r.nextDouble() < 0.25)
		{
			priestsFrom = r.nextInt(maxStrength + 1);
		}
		
		while(currentStrength > 0)
		{
			boolean done = false;
			
			if(atTier >= 0 && all.get(atTier).size() == 0 && atTier < 3)
				atTier++;
			
			if(magePriests && !doneWithMages)
			{
				if(compensationMagePriests && currentStrength == maxStrength)
				{
					for(Unit u : extras)
					{
						u.commands.add(new Command("#holy"));
						u.appliedFilters.add(this.getPriestPattern(currentStrength));
						u.tags.add("magepriest");
						u.commands.add(new Command("#gcost", "+" + 10*currentStrength));

					}
					done = true;
				}
				else if(atTier >= 0 && all.get(atTier).size() > 0 && !compensationMagePriests)
				{
					for(Unit u : all.get(atTier))
					{
						u.appliedFilters.add(this.getPriestPattern(currentStrength));
						u.commands.add(new Command("#holy"));
						u.tags.add("magepriest");
						u.commands.add(new Command("#gcost", "+" + 10*currentStrength));

					}
					done = true;
					atTier++;
					
					if(r.nextDouble() < 0.2)
						doneWithMages = true;
				}
			}
			
			Race prace = nation.races.get(0);
			if(!nation.races.get(0).hasRole("priest"))
				prace = nation.races.get(1);
			
			if((currentStrength > 1 && !done) || currentStrength == 1 || currentStrength <= priestsFrom)
			{
				Unit u = null;
				if(priests.size() > 0)
				{
					List<Unit> derp = new ArrayList<Unit>();
					derp.add(priests.get(0));
					u = this.deriveFrom("priest", derp, priests.get(0).race, currentStrength + 1, currentStrength, true).get(0);
				}
				else
				{
					u = this.generateNew("priest", prace, 1, currentStrength + 1, prace == nation.races.get(0)).get(0);
				}
				
				u.color = priestcolor;
				u.appliedFilters.add(this.getPriestPattern(currentStrength));
				u.commands.add(new Command("#holy"));
				u.commands.add(new Command("#gcost", "+" + (int)(Math.pow(2, currentStrength)*10)));
				u.tags.add("priest " + currentStrength);

				
				u.commands.add(new Command("#mr", "+" + (2 + currentStrength)));

				
				// STR or cap only for h3
				if(currentStrength == 3 && r.nextDouble() > 0.5)
					u.commands.add(new Command("#slowrec"));
				else if(currentStrength == 3)
				{
					u.caponly = true;
					if(r.nextDouble() > 0.9)
						u.commands.add(new Command("#slowrec"));
				}
				
				if(currentStrength == 3 && r.nextDouble() > 0.75){/*do nothing*/}
				else if(currentStrength == 2 && r.nextDouble() > 0.85){/*do nothing*/}
				else
					u.commands.add(new Command("#poorleader"));
				
				List<String> body = new ArrayList<String>();	// Set a temporary null description for priests
				(new CommanderGenerator(this.nationGen, this.nation)).generateDescription(u, false, false, false);
				//body.add();					
		
				//body.add("\"No description\"");
				//u.commands.add(new Command("#descr", body));

				priests.add(0, u);

			}	
			currentStrength--;
		}
		
	
		if(sacredMageTiers > 0)
		{
			for(int i = 0; i < 3; i++)
			{
				for(Unit u : all.get(i))
					u.commands.add(new Command("#holy"));
			
				sacredMageTiers--;
				if(sacredMageTiers == 0)
					break;
			}
		}
		
		
		
		// Filters
		List<Filter> filters = ChanceIncHandler.retrieveFilters("priestfilters", "default_priestfilters", nationGen.filters, priests.get(0).pose, priests.get(0).race);

		int power = 0;
		if(nation.random.nextDouble() < 0.05 + maxStrength * 0.025)
		{
			power++;
			if(nation.random.nextDouble() < 0.1 + maxStrength * 0.025)
			{
				power++;
				if(nation.random.nextDouble() < 0.1 + maxStrength * 0.025)
				{
					power++;
					if(nation.random.nextDouble() < 0.1 + maxStrength * 0.025)
					{
						power++;
					}
				}
			}
		}
		

		//this.applyFilters(priests, power, filters);
		

		return priests;
	}
	
	/**
	 * The new method
	 * @param units
	 * @param power
	 * @param filters
	 */
	public void applyFilters(List<Unit> units, int power, List<Filter> filters)
	{
		if(units.size() == 0)
			return;
		

		// Filter selection is strict (see method getPossibleFiltersByPaths) for mages, but not for priests.
		// IF PRIESTS START TO CONSISTENTLY GET MAGIC WITH SOME NON-FILTER IMPLEMENTATION THIS NEEDS TO BECOME TAG BASED
		boolean strict = false;
		int[] picks = units.get(0).getMagicPicks(false);
		
		for(int i = 0; i < 8; i++)
		{
			if(picks[i] > 0)
				strict = true;
		}

		
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation);

		while(power > 0)
		{
			List<Filter> moreFilters = new ArrayList<Filter>();
			
			//if(nation.random.nextDouble() > 0.1)
			//	moreFilters = ChanceIncHandler.getFiltersWithPower(power, power, filters);
			

			
			int at = nation.random.nextInt(power + 5) - 5; // -5 to power - 2
			int cyc = 0;
			while(moreFilters.size() == 0 && at >= -5)
			{
				moreFilters = ChanceIncHandler.getFiltersWithPower(at, power, filters);
				at--;
				cyc++;
			}
			

			if(moreFilters.size() < -2)
			{
				moreFilters.clear();
				moreFilters.addAll(filters);
			}
			

			
			int tier = 3;
			if(nation.random.nextDouble() > 0.65)
				tier = 2;
			else if(nation.random.nextDouble() > 0.875)
				tier = 1;
			
			List<Unit> mages = getMagesOfTier(units, tier);
			
			
			if(mages.size() == 0)
			{

				if(units.get(0).tags.contains("extramage"))
				{
					mages.addAll(units);
					tier = nation.random.nextInt(2) + 2; // 2 to 3;
				}
				
				if(mages.size() == 0) // Unhandled tier. Heroes have that.
				{ 
					mages = units;
				}
			}
			
			List<Filter> actualFilters = this.getFiltersForTier(moreFilters, tier);

			
			if(actualFilters.size() == 0)
			{
				System.out.println("No filters for tier " + tier + ", original " + moreFilters.size() + " / " + mages.get(0).race);
				return;
			}
			
			
			boolean different = allHaveDistinguishingPath(mages);
			boolean common = MageGenerator.findCommonPaths(mages).size() > 0;
			
			List<Filter> oldFilters = new ArrayList<Filter>();
			for(Unit u : units)
				oldFilters.addAll(u.appliedFilters);

			// If all mages of tier have distinguishing paths and either luck or no common paths, separate filters are rolled. 
			if(different && (nation.random.nextBoolean() || !common))
			{
				int maxpower = 0;
				for(Unit u : mages)
				{
					List<Filter> givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findDistinguishingPaths(u, mages), strict);
					givenFilters.removeAll(u.appliedFilters);
	

					if(givenFilters.size() == 0)
					{
						givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findDistinguishingPaths(u, mages), false);
						givenFilters.removeAll(u.appliedFilters);
					}
					
					
					// CanAdd check
					givenFilters = getValidFilters(givenFilters, mages);
					
					
					// Use old filters when feasible!
					List<Filter> tempFilters = new ArrayList<Filter>();
					tempFilters.addAll(givenFilters);
					tempFilters.retainAll(oldFilters);
					tempFilters = getValidFilters(tempFilters, mages);

					if(tempFilters.size() > 0 && nation.random.nextDouble() > 0.25 && chandler.handleChanceIncs(u, tempFilters).size() > 0)
						givenFilters = tempFilters;
						
	
					
					
					Filter f = Filter.getRandom(nation.random, chandler.handleChanceIncs(u, givenFilters));
		
					
					u.appliedFilters.add(f);
					
					
					
					if(f.power > maxpower)
						maxpower = f.power;
					
				}
				power -= maxpower;
			}
			// Otherwise all mages get the same filter if possible
			else 
			{
				List<Filter> givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findCommonPaths(mages), false);
				
				// Remove given filters to avoid duplicates
				for(Unit u : mages)
				{
					givenFilters.removeAll(u.appliedFilters);
				}
				
				// If no filters, failsafe into giving whatever
				if(givenFilters.size() == 0)
				{
					givenFilters = new ArrayList<Filter>();
					givenFilters.addAll(actualFilters);
					for(Unit u : mages)
					{
						givenFilters.removeAll(u.appliedFilters);
					}
				}
				
				// CanAdd check
				givenFilters = getValidFilters(givenFilters, mages);
		
				
				// Use old filters when feasible!
				List<Filter> tempFilters = new ArrayList<Filter>();
				tempFilters.addAll(givenFilters);
				tempFilters.retainAll(oldFilters);
				tempFilters = getValidFilters(tempFilters, mages);


				
				if(tempFilters.size() > 0 && nation.random.nextDouble() > 0.25)
					givenFilters = tempFilters;
					
	
	
				
				Filter f = Filter.getRandom(nation.random, chandler.handleChanceIncs(mages, givenFilters));


				
				for(Unit u : mages)
					u.appliedFilters.add(f);
				
				power -= f.power;
			}
			
			
		}
	}
	
	
	/**
	 * Checks ChanceIncHandler.canAdd() for all filters and removes bad ones.
	 * @param filters
	 * @param units
	 * @return
	 */
	private List<Filter> getValidFilters(List<Filter> filters, List<Unit> units)
	{
		List<Filter> list = new ArrayList<Filter>();
		
		for(Filter f : filters)
		{
			boolean ok = true;
			for(Unit u : units)
			{
				if(!ChanceIncHandler.canAdd(u, f))
					ok = false;
			}
			
			if(ok)
				list.add(f);
		}
		
		return list;
	}
	
	
	/**
	 * Returns filters that align with given path (either #personalmagic or possibly also if just chanceincs)
	 * @param filters Filters
	 * @param path Paths
	 * @param strict If true, require positive chanceinc in addition to #personalmagic
	 * @return
	 */
	private List<Filter> getPossibleFiltersByPaths(List<Filter> filters, List<Integer> paths, boolean strict)
	{
		
		List<Filter> list = new ArrayList<Filter>();
		for(Filter f : filters)
		{
			boolean ok = true;
			for(String tag : f.tags)
			{
				List<String> args = Generic.parseArgs(tag);
				if(args.get(0).equals("personalmagic") && args.size() > 1)
				{
					int needpath = Generic.PathToInteger(args.get(1));
					if(!paths.contains(needpath))
					{
						ok = false;
					}
				}
				
			
			}
			
			if(strict)
			{
				for(String c : f.chanceincs)
				{
					List<String> args = Generic.parseArgs(c);
					if((args.get(0).equals("personalmagic") || args.get(0).equals("magic")) && args.size() > 1)
					{
						List<Integer> possibles = new ArrayList<Integer>();
						for(int i = 1; i < args.size() - 1; i++)
						{
							int needpath = Generic.PathToInteger(args.get(i));
							if(needpath > -1)
								possibles.add(needpath);
						}
						
						if(!paths.containsAll(possibles))
						{
							ok = false;
						}
					}
				}
			}
			
			
			if(ok)
				list.add(f);
		}
		
		return list;
	}
	
	/** 
	 * Checks whether all mages given have a distinguishing path
	 * @param mages
	 * @return
	 */
	private boolean allHaveDistinguishingPath(List<Unit> mages)
	{
		boolean d = true;
		for(Unit u : mages)
		{
			if(MageGenerator.findDistinguishingPaths(u, mages).size() == 0)
				d = false;
		}
		return d;
	}
	
	/**
	 * Returns a list of mages of given tier out of the given list of mages
	 * @param mages List of mages
	 * @param tier Tier
	 * @return
	 */
	private List<Unit> getMagesOfTier(List<Unit> mages, int tier)
	{
		List<Unit> list = new ArrayList<Unit>();
		for(Unit u : mages)
		{
			int gettier = -1;
			if(Generic.getTagValue(u.tags, "schoolmage") != null)
				gettier = Integer.parseInt(Generic.getTagValue(u.tags, "schoolmage"));
			if(Generic.getTagValue(u.tags, "priest") != null && tier == 0)
				gettier = Integer.parseInt(Generic.getTagValue(u.tags, "priest"));
			
			
			if(gettier == tier)
				list.add(u);
			
		}
		
		return list;
	}
	
	
	
	
	/**
	 * Returns a list of filters without #notfortier <tier>
	 * @param filters List of original filters
	 * @param tier Tier
	 * @return
	 */
	private List<Filter> getFiltersForTier(List<Filter> filters, int tier)
	{
		List<Filter> list = new ArrayList<Filter>();
		for(Filter f : filters)
		{
			
			boolean ok = true;
			if(Generic.getTagValue(f.tags, "notfortier") != null)
			{
				int nottier = Integer.parseInt(Generic.getTagValue(f.tags, "notfortier"));
				if(tier == nottier)
				{
					ok = false;
				}
			}
			
			if(ok)
				list.add(f);
			
		}
		
		return list;
	}
	
	
	/**
	 * The old method
	 * @param units
	 * @param power
	 * @param filters
	 */
	public void applyFilters_old(List<Unit> units, int power, List<Filter> filters)
	{
		if(units.size() == 0)
			return;
		
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation);
		while(power > 0)
		{
			List<Filter> moreFilters = ChanceIncHandler.getFiltersWithPower(power, power, filters);
			

			if(chandler.handleChanceIncs(units, moreFilters).size() == 0 || nation.random.nextDouble() > 0.7)
				moreFilters = ChanceIncHandler.getFiltersWithPower(power - 1, power, filters);
			if(chandler.handleChanceIncs(units, moreFilters).size() == 0 || power == 1)
				moreFilters = ChanceIncHandler.getFiltersWithPower(-100, power, filters);
			if(chandler.handleChanceIncs(units, moreFilters).size() == 0)
				break;
			

			Filter f = Filter.getRandom(nation.random, chandler.handleChanceIncs(units, moreFilters));
			
			
			// validate
			
			int added = 0;
			for(Unit u : units)
			{
				boolean ok = true;
				
				// Validate suitable paths
				int[] paths = u.getMagicPicks();
				for(String tag : f.tags)
				{
					List<String> args = Generic.parseArgs(tag);
					if(args.get(0).equals("personalmagic") && args.size() > 1)
					{
						int path = Generic.PathToInteger(args.get(1));
						if(paths[path] == 0)
							ok = false;
					}
				}
				
				// Validate tier things
				int tier = 0;
				if(Generic.getTagValue(u.tags, "schoolmage") != null)
					tier = Integer.parseInt(Generic.getTagValue(u.tags, "schoolmage"));
				if(Generic.getTagValue(u.tags, "priest") != null && tier == 0)
					tier = Integer.parseInt(Generic.getTagValue(u.tags, "priest"));
	
				if(Generic.getTagValue(f.tags, "notfortier") != null)
				{
					int nottier = Integer.parseInt(Generic.getTagValue(f.tags, "notfortier"));
					if(tier == nottier)
						ok = false;
				}
					
				if(ok && ChanceIncHandler.canAdd(u, f))
				{
					added++;
					u.appliedFilters.add(f);
				}
				
			
				
			}
			
			filters.remove(f);
			if(added > 0)
			{
				power -= f.power;
			}
	
			
			
			
			
		}
		
		
	}
	
	
	public List<List<Unit>> generateBases()
	{

		boolean primaryforeign = false;
		
		// PRIMARIES
		Race race = nation.races.get(0);
		
		double bonussecchance = 1;
		if(Generic.containsTag(race.tags, "secondaryracemagemod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(race.tags, "secondaryracemagemod"));
		if(Generic.containsTag(nation.races.get(1).tags, "primaryracemagemod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(nation.races.get(1).tags, "primaryracemagemod"));
		
		

		// 27.09.14 sloppy code to check to make sure secondary nation has a secondary-eligible pose
		if(nation.random.nextDouble() < 0.075 * bonussecchance && nation.races.get(1).hasRole("mage"))
		{
			for(Pose p : nation.races.get(1).poses)
			{
				if(Generic.containsTag(p.roles, "mage"))
					for(AbilityTemplate t : p.templates)
					{
						if(!Generic.containsTag(t.tags, "primarynationonly"))
						{
							race = nation.races.get(1);
							break;
						}
					}
					if(race == nation.races.get(1))
						break;
			}
		}
		else if(!nation.races.get(0).hasRole("mage"))
			race = nation.races.get(1);
		
		if(race == nation.races.get(1))
			primaryforeign = true;
		
		int primaryamount = 1;
		
		double onechance = 0.625;
		double twochance = 0.225;
		double threechance = 0.15;
		
		if(Generic.containsTag(race.tags, "oneprimarychance"))
			onechance = Double.parseDouble(Generic.getTagValue(race.tags, "oneprimarychance"));
		if(Generic.containsTag(race.tags, "twoprimarychance"))
			twochance = Double.parseDouble(Generic.getTagValue(race.tags, "twoprimarychance"));
		if(Generic.containsTag(race.tags, "threeprimarychance"))
			threechance = Double.parseDouble(Generic.getTagValue(race.tags, "threeprimarychance"));
		
		double rand = nation.random.nextDouble() * (onechance + twochance + threechance);

		if(rand < onechance)
			primaryamount = 1;
		else if(rand < onechance + twochance)
			primaryamount = 2;
		else
			primaryamount = 3;

		
		List<Unit> primaries = generateNew("mage", race, primaryamount, 3, race == nation.races.get(0));
		

		
		// SECONDARIES
		if(nation.random.nextDouble() < 0.1 * bonussecchance && race != nation.races.get(0)){/*do nothing*/}
		else
			race = nation.races.get(0);

		
		
		double randroll = nation.random.nextDouble();
		int secondaryamount = 1;
		// 3 primaries - 35% chance for 3 secondaries.
		if(primaryamount == 3 && randroll < 0.35)
			secondaryamount = 3;
		// 3 primaries - 15% chance for 0 secondaries.
		else if(primaryamount == 3 && randroll < 0.50)
			secondaryamount = 0;
		
		// 2 primaries - 65% chance for 2 secondaries. Otherwise 1 or 0.
		if(primaryamount == 2 && randroll < 0.65)
			secondaryamount = 2;
		// 2 primaries - 15% chance for 0 secondaries. 
		else if(primaryamount == 2 && randroll < 0.75)
			secondaryamount = 0;

		// 1 primaries - 25% chance for 3 secondaries.
		if(primaryamount == 1 && randroll < 0.1)
			secondaryamount = 3;
		// 1 primaries - 25% chance for 2 secondaries.
		else if(primaryamount == 1 && randroll < 0.3)
			secondaryamount = 2;
		
		List<Unit> secondaries = new ArrayList<Unit>();
		if(secondaryamount <= primaryamount && secondaryamount > 0)
		{
			secondaries = deriveFrom("mage", primaries, race, 3, 2, true);
			// Remove last generated unit until we have the correct count
			for(int i = 0; i < primaryamount - secondaryamount; i++)
				secondaries.remove(secondaries.size() - 1);
		}
		else if(secondaryamount > 0)
		{
			secondaries = deriveFrom("mage", primaries, race, 3, 2, true);
			secondaries = this.varyUnit(secondaries.get(0), secondaryamount, 2, varyHat);
		}
		

		// TERTIARIES
		List<Unit> tertiaries = new ArrayList<Unit>();
		
		// Tertiaries might be of secondary race if primaries aren't.
		bonussecchance = 1;
		if(Generic.containsTag(race.tags, "secondaryracetertiarymagemod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(race.tags, "secondaryracemagemod"));
		if(Generic.containsTag(nation.races.get(1).tags, "primaryracetertiarymagemod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(nation.races.get(1).tags, "primaryracemagemod"));
		
		if(0.05 * bonussecchance > nation.random.nextDouble() && !primaryforeign)
			race = nation.races.get(1);
		else
			race = nation.races.get(0);
		
		

		int tertiaryamount = 1;
		
		if((primaryamount + secondaryamount > 3 && nation.random.nextDouble() < 0.25) || (primaryamount + secondaryamount > 4 && nation.random.nextDouble() < 0.5) || primaryamount + secondaryamount > 5)
			tertiaryamount = 0;
	
		
		if(tertiaryamount <= secondaryamount && tertiaryamount > 0)
		{
			tertiaries = deriveFrom("mage", secondaries, race, 2, 1, true);
	
			// Remove last generated unit until we have the correct count
			for(int i = 0; i < secondaryamount - tertiaryamount; i++)
				tertiaries.remove(tertiaries.size() - 1);
		}
		else if(secondaryamount > 0  && tertiaryamount > 0)
		{
			tertiaries = deriveFrom("mage", secondaries, race, 2, 1, true);
			tertiaries = this.varyUnit(tertiaries.get(0), tertiaryamount, 1, varyHat);
		}
		else if(tertiaryamount > 0)
		{
			tertiaries = deriveFrom("mage", primaries, race, 3, 1, true);
			for(int i = 0; i < primaryamount - tertiaryamount; i++)
				tertiaries.remove(tertiaries.size() - 1);
		}
		
		tagAll(primaries, "schoolmage 3");
		tagAll(secondaries, "schoolmage 2");
		tagAll(tertiaries, "schoolmage 1");

		
		List<List<Unit>> list = new ArrayList<List<Unit>>();
		list.add(tertiaries);
		list.add(secondaries);
		list.add(primaries);
		return list;
	}
	
	
	private void tagAll(List<Unit> units, String tag)
	{
		if(units == null)
			return;
	
		for(Unit u : units)
			u.tags.add(tag);
	}
	
	private boolean shouldChangePose(String posename, Race newrace, Race oldrace, Pose current, int currenttier, int futuretier)
	{
		boolean future = false;
		boolean past = false;
		

		
		// Change pose if we have specific next pose
		for(Pose p : newrace.getPoses(posename))
			if(p.tags.contains("tier " + futuretier) && p != current)
				future = true;
		
		// Current doesn't contain pose for next tier
		if(current.tags.contains("tier " + currenttier) && !current.tags.contains("tier " + futuretier))
			past = true;
		
		// Forbidden from the future tier
		if(current.tags.contains("notier " + futuretier))
			past = true;
		
		// We have a specific pose and current doesn't contain the required pose
		if(future && !current.tags.contains("tier " + futuretier))
			past = true;
		
		// Random chance
		if(future && nation.random.nextDouble() < 0.075)
			past = true;
		
		if(oldrace != newrace)
			past = true;
		

			
		return past;
	}
	

	
	private List<Unit> deriveFrom(String posename, List<Unit> parents, Race race, int fromTier, int toTier, boolean isPrimaryRace)
	{
		
		List<Unit> newlist = new ArrayList<Unit>();
		
		Pose newpose = null;
		if(shouldChangePose(posename, race, parents.get(0).race, parents.get(0).pose, fromTier, toTier))
		{
			List<Pose> poses = this.getPossiblePoses(posename, race, toTier, isPrimaryRace);
			if(poses.size() > 0)
				newpose = Entity.getRandom(nation.random, poses);
		}
		else
			newpose = parents.get(0).pose;
		
		ItemSet used = new ItemSet();
		for(Unit u : parents)
		{
			Unit nu = this.unitGen.generateUnit(nation.races.get(0), newpose); //this.copyUnit(u);
			for(String slot : u.slotmap.keySet())
			{
				if(u.pose.getItems(slot) == null || nu.pose.getItems(slot) == null)
					continue;
				
				// Just targeted tier		
				ItemSet all = nu.pose.getItems(slot).filterTag("tier " + toTier, true);
				if(all.possibleItems() == 0) // Allows tiers below toTier
					for(int i = toTier + 1; i <= fromTier; i++)
						all = all.filterTag("tier " + i, false);
				if(all.possibleItems() == 0 && toTier != 1) // Just everything
					all = nu.pose.getItems(slot).getCopy();
				
				
				if(u.getSlot(slot) != null)
				{
					Item newitem = getDerivedItem(u.getSlot(slot), nu, used);
					
					if((newitem == null || !newitem.tags.contains("eliteversion " + u.getSlot(slot).name)) && slot.equals("armor") && toTier < 2 && fromTier == 2 && u.pose == nu.pose)
						newitem = u.getSlot(slot);
					
					
					
					if(newitem == null)
					{				
						ItemSet derp = new ItemSet();
						derp.addAll(all);
						derp.retainAll(used);
						if(derp.possibleItems() > 0)
							all = derp;
						
						if(all.possibleItems() > 1)
						{
							all.remove(u.getSlot(slot));

						}
						newitem = Entity.getRandom(nation.random, all);
					}
	
					
					if(newitem != null)
					{
						nu.setSlot(slot, newitem);
						used.add(newitem);

					}
					
				}

			}
			
			
			if(toTier < 2)
			{
				varyHat = false;
				nu.setSlot("helmet", null);
				nu.setSlot("cloakb", null);
				nu.setSlot("cloakf", null);
			}
			
			newlist.add(nu);
			if(varyHat)
				exclusions.add(nu.getSlot("helmet"));
			else
				exclusions.add(nu.getSlot("weapon"));
			
			
			// 2015.03.26 Sloppy code derived from unitGen to add mounts
			if(nu.pose.getItems("mount") != null && nu.getSlot("mount") == null)
			{
				//Item mount = nation.usedItems.filterSlot("mount").filterForPose(p).getRandom(nation.random);

				/*if(mount == null)
				{
					mount = p.getItems("mount").getRandom(nation.random);
					System.out.println("..\nCouldn't find old mount; using new\n..");
				}*/
				
				//template.setSlot("mount", mount);
				
				// Scouts usually ride the typical racial mount
				String pref = null;
				if(Generic.getTagValue(nu.race.tags, "preferredmount") != null && nation.random.nextDouble() > 0.80)
				{
					pref = Generic.getTagValue(nu.race.tags, "preferredmount");
				}
		
				nu.setSlot("mount", unitGen.getSuitableItem("mount", nu, nu.pose.getItems("mount"), exclusions, nu.pose.getItems("mount"), "animal " + pref, false, false));
								
				if(nu.getSlot("mount") == null)
				{
					nu.setSlot("mount", nu.pose.getItems("mount").getRandom(nation.random));
					// System.out.println("..\nCouldn't find old mount; using new\n..");
				}
			}
	
		}
		
		return newlist;
	}
	
	private Item getDerivedItem(Item item, Unit u, ItemSet used)
	{
		if(used == null)
			used = new ItemSet();
		
		ItemSet items = u.pose.getItems(item.slot).filterSameSprite(exclusions);
		
		ItemSet possibles = new ItemSet();
		for(Item i : items)
		{
			if(i.tags.contains("eliteversion " + item.name))
			{
				possibles.add(i);
			}
		}
		
		possibles.remove(item);
		
		ItemSet test = new ItemSet();
		test.addAll(possibles);
		test.retainAll(used);
		if(test.possibleItems() > 0)
		{
			possibles = test;
		}
		
		return possibles.getRandom(nation.random);
	}
	
	// Generates a set of specific tier mages from scratch
	public List<Unit> generateNew(String posename, Race race, int amount, int tier, boolean isPrimaryRace)
	{
		
		Unit u = unitGen.generateUnit(race, Entity.getRandom(nation.random, getPossiblePoses(posename, race, tier, isPrimaryRace)));
		


		unitGen.equipUnit(u, used, exclusions, "tier " + tier, false);
		unitGen.armUnit(u, used, exclusions, "tier " + tier, false);
		if(tier == 3)
		{
			u.setSlot("cloakb", unitGen.getSuitableItem("cloakb", u, exclusions, used, u.pose.getItems("cloakb"), null, true, true));
		}
		

		
		List<Unit> list = varyUnit(u, amount, tier, varyHat);
	
		
		if(!exclusions.contains(u.getSlot("weapon")))
			exclusions.add(u.getSlot("weapon"));
		
		return list;
	}
	
	
	private List<Pose> getPossiblePoses(String posename, Race race, int tier, boolean isPrimaryRace)
	{
		List<Pose> possibles = new ArrayList<Pose>();
		for(Pose p : race.getPoses(posename))
			if(p.tags.contains("tier " + tier) && (isPrimaryRace || !p.tags.contains("primaryraceonly")))
				possibles.add(p);
	
		if(possibles.size() == 0)
		{
			for(Pose p : race.getPoses(posename))
			{
				if(!p.tags.contains("notier " + tier) && (isPrimaryRace || !p.tags.contains("primaryraceonly")))
					possibles.add(p);
			}
		}
		
		if(possibles.size() == 0)
			System.out.println("CRITICAL ERROR: No possible pose for " + race.name + " mages for tier " + tier + ".");
		
		return possibles;
			
	}
	
	
	private List<Unit> varyUnit(Unit u, int amount, int tier, boolean varyHat)
	{
		
		
		if(u.pose.tags.contains("dontvaryhat"))
		{
			varyHat = false;
		}
		if(u.pose.tags.contains("dontvaryweapon"))
			varyHat = true;

		String slot = "weapon";
		if(varyHat)
			slot = "helmet";
		
		
		List<String> possibleslots = new ArrayList<String>();
		for(String tag : u.pose.tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("varyslot"))
				possibleslots.add(args.get(1));
		}
		if(possibleslots.size() > 0)
			slot = possibleslots.get(nation.random.nextInt(possibleslots.size()));
		
		ItemSet stuff = u.pose.getItems(slot).filterTag("tier " + tier, true);
		stuff.removeAll(exclusions);
		stuff.remove(u.getSlot(slot));
		

		if(stuff.possibleItems() == 0)
		{
			if(slot.equals("weapon") && u.pose.getItems("helmet") != null)
				slot = "helmet";
			else if(u.pose.getItems("weapon") != null)
				slot = "weapon";
			
			stuff = u.pose.getItems(slot).filterTag("tier " + tier, true);
			stuff.removeAll(exclusions);
			stuff.remove(u.getSlot(slot));
		}



		
		List<Unit> list = new ArrayList<Unit>();
		
		list.add(u);
		
		exclusions.add(u.getSlot(slot));
		
		ItemSet all = u.pose.getItems(slot);
		for(int i = 1; i < amount; i++)
		{
			Unit nu = copyUnit(u);
			Item newitem = unitGen.getSuitableItem(slot, nu, exclusions, used, all, "tier " + tier);
			nu.setSlot(slot, newitem);
			
			exclusions.add(nu.getSlot(slot));
			

			
			
			list.add(nu);

		}
		

		

		return list;
	}
	
	
	public int priceMage(Unit u)
	{
		/*
		double max = u.getMagicAtHighest(0.25);
		double picks = u.getMagicAmount(1);
		double randoms = u.getRandoms(0.25);
		double holy = u.getHolyPicks();
		*/


		double premium = 0;
		List<MagicFilter> fs = u.getMagicFilters();
		for(MagicFilter f : fs)
		{
			for(String t : f.tags)
			{
				List<String> args = Generic.parseArgs(t);
				if(args.get(0).equals("pricepremium") && args.size() > 1)
				{
					premium += Double.parseDouble(args.get(1));
				}
			}
		}
		/*
		double baseprice = 28.98577 * picks + 24.00298 * randoms + 20.1666 * holy + 20.50836 * max + premium;
		int price = 5 * (int)Math.round(baseprice / 5);
		*/
		return (int)premium;
	}


	
	protected void resolveAge(List<Unit> units)
	{
		Random r = nation.random;
		double random = r.nextDouble();
		for(Unit u : units)
		{
			
			double picks = u.getMagicAmount(20);
			double limit = 0.1 + (picks - 2) * 0.2; 
			if(picks < 4)
				limit = 0;
			
			if(random < limit)
			{
				u.commands.add(new Command("#mapmove", "-1"));
			}
			else if(picks > 6)
			{
				u.commands.add(new Command("#older", "-15"));
			}
			else if(picks > 5)
			{
				u.commands.add(new Command("#older", "-10"));
			}
			else if(picks > 4)
			{
				u.commands.add(new Command("#older", "-5"));
			}

		}
		

	}
	
	
	private MagicFilter getPriestPattern(int level)
	{
		MagicPattern p = new MagicPattern(nationGen);
		p.picks.put(1, level);
	
		List<Integer> prio = new ArrayList<Integer>();
		prio.add(8);
		
		MagicFilter m = new MagicFilter(nationGen);
		m.pattern = p;
		m.prio = prio;
		m.name = "PRIESTPICKS";
		
		return m;
	
	}

}
