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
import nationGen.entities.Theme;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.rostergeneration.CommanderGenerator;
import nationGen.rostergeneration.TroopGenerator;
import nationGen.rostergeneration.montagtemplates.MageMontagTemplate;
//import nationGen.rostergeneration.TroopGenerator.Template;
import nationGen.units.Unit;

public class MageGenerator extends TroopGenerator {
	
	public List<MagicPattern> possiblePatterns = new ArrayList<MagicPattern>();
	
	
	public MageGenerator(NationGen g, Nation n) {
		super(g, n, "magegen");
	
		loadPatterns();
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
		
			if(f.basechance > 0)
				orig.add(f);
		}
		while(orig.size() > 0)
		{

			Filter i = Filter.getRandom(this.random, orig);
			orig.remove(i);
			newlist.add(Generic.PathToInteger(i.name));
		}
		
		return newlist;
		
	}
	
	private List<Integer> getPrios(Unit u)
	{
		List<Filter> orig = new ArrayList<Filter>();
		List<Integer> newlist = new ArrayList<Integer>();
		
		
		
		List<String> source = new ArrayList<String>();
		
		if(Generic.getTagValue(u.pose.tags, "magicpriority") != null)
			source.addAll(Generic.getTags(u.pose.tags, "magicpriority"));
		else if(Generic.getTagValue(u.race.tags, "magicpriority") != null)
			source.addAll(Generic.getTags(u.race.tags, "magicpriority"));
		for(Theme t : nation.nationthemes)
			if(Generic.getTagValue(t.tags, "magicpriority") != null)
				source.addAll(Generic.getTags(t.tags, "magicpriority"));
		
	
		for(int i = 0; i < 8; i++)
		{
			Filter f = new Filter(this.nationGen);
			f.name = Generic.integerToPath(i);
	
			if(source != null)
			{
				for(String tag : source)
				{
					List<String> args = Generic.parseArgs(tag);
					if(args.get(0).equals("magicpriority") && args.size() > 2)
					{
						if(Generic.PathToInteger(args.get(1)) == i)
						{
							f.basechance *= Double.parseDouble(args.get(2));
							
							// All paths need to be possible, so let's have a failsafe
							if(f.basechance == 0)
								f.basechance = 0.00001;
						}
					}
				}
			}
			
	
			
			orig.add(f);
		}
		
		while(orig.size() > 0)
		{
			Filter i = Filter.getRandom(this.random, orig);
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


	
	public List<Unit> generateMages()
	{
		

		List<List<Unit>> mages = generateMageBases();
		
		
		// Tag mages
		for(int i = 0; i < 3; i++)
		{
			this.tagAll(mages.get(i), "schoolmage " + (i+1));
			
			
			for(Unit u : mages.get(i))
			{
				unitGen.addFreeTemplateFilters(u);
				unitGen.addTemplateFilter(u, "magetemplates", "default_magetemplates");
			}

		}

		
		int primaries = mages.get(2).size();
		int secondaries = mages.get(1).size();
		int tertiaries = mages.get(0).size();
		
		// Decide if primaries should be cap only
		boolean caponlyprimaries = false;
		

		if(secondaries > primaries)
			caponlyprimaries = true;	
		if(primaries > 0 && secondaries > 0 && this.random.nextDouble() < 0.70)
			caponlyprimaries = true;
		if(primaries + secondaries + tertiaries >= 5)
			caponlyprimaries = true;


				
		List<MagicPattern> available = getPatternsForTier(3, primaries);
		
		List<MagicPattern> primarypatterns = new ArrayList<MagicPattern>();
		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");
		MagicPattern primaryPattern = Entity.getRandom(this.random, chandler.handleChanceIncs(mages.get(2).get(0), available));


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

		
		if(primaries == secondaries && this.random.nextDouble() < 0.35)
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
		if(secondaries > primaries && this.random.nextDouble() < 0.6 && secs.size() >= secondaries)
		{
			varySecs = false;
			for(int i = 0; i < secondaries; i++)
			{			
				
				MagicPattern p = Entity.getRandom(this.random, chandler.handleChanceIncs(mages.get(1).get(0), secs));
				
				secs.remove(p);
				secondarypatterns.add(p);
			}
		}
		else if(secondaries > 0)
		{
			
			
			MagicPattern p = Entity.getRandom(this.random, chandler.handleChanceIncs(mages.get(1).get(0), secs));
			

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
		
		if(tertiaries > 0)
		{
			MagicPattern p = Entity.getRandom(this.random, chandler.handleChanceIncs(mages.get(0).get(0), terts));
			for(int i = 0; i < tertiaries; i++)
			{						
				tertiarypatterns.add(p);
			}
		}		

		// Apply patterns
		List<List<MagicPattern>> all = new ArrayList<List<MagicPattern>>();
		all.add(tertiarypatterns);
		all.add(secondarypatterns);
		all.add(primarypatterns);
		
		double leaderrandom1 = this.random.nextDouble();
		double leaderrandom2 = this.random.nextDouble();
		double slowrecrand = this.random.nextDouble();
		

		for(int i = 2; i >= 0; i--)
			for(int j = 0; j < mages.get(i).size(); j++)
			{
	
				
				List<Unit> parents = null;
				if(i < 2)
					parents = mages.get(i + 1);
				
				List<Integer> derp;
				if(i == 1 && !varySecs) // Secondary mages, specific case of pattern varying but magic order not
				{
					derp = prio;
				}
				else if(parents != null && mages.get(i).size() == parents.size() && parents.size() > 1) // Secondary and tertiary mages
				{
					derp = varyAt(getVaryPoint(all.get(i).get(j), mages.get(i).size()), prio, j, parents.get(j), parents);
				}
				else // Primary Mages
				{
					derp = varyAt(getVaryPoint(all.get(i).get(j), mages.get(i).size()), prio, j);
				}
				
				MagicFilter f = getMagicFilter(derp, all.get(i).get(j), mages.get(i).size());
				mages.get(i).get(j).appliedFilters.add(f);
				

				// Sets mage name to match pattern. Debug purposes.
				mages.get(i).get(j).name.setType(all.get(i).get(j).toString(derp));
				
			
				
				
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

		for(Unit u : list)
			determineSpecialLeadership(u, false);

		tagAll(list, "identifier schoolmage");
		

		// Handle montags
		for(int i = 1; i <= 3; i++)
			for(Unit u : this.getMagesOfTier(list, i))
				unitGen.handleMontagUnits(u, new MageMontagTemplate(nation, nationGen, i), "montagmages");

		
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
		

		


		
		// Extra mage
		boolean ok = false;
		if(diversity < 4 && ((norand_picks[4] == 0 && norand_picks[7] == 0) && this.random.nextDouble() < 0.8))
		{
			ok = true;
		}
		if(max < 4 && norand_picks[4] == 0 && norand_picks[7] == 0 && diversity < 4) 
		{
			ok = true;
		}
		if(diversity < 5 && norand_picks[4] == 0 && norand_picks[7] == 0 && this.random.nextDouble() < 0.1)
		{
			ok = true;
		}
		if(max < 4 && atmax < 2 && norand_picks[4] == 0 && norand_picks[7] == 0 && this.random.nextDouble() < 0.5)
		{
			ok = true;
		}
		if(norand_picks[4] == 0 && norand_picks[7] == 0 && diversity < 6 && this.random.nextDouble() < 0.05)
		{
			ok = true;
		}
		if(norand_picks[4] == 0 && norand_picks[7] == 0 && atmax == 1 && diversity < 4 && this.random.nextDouble() < 0.65)
		{
			ok = true;
		}
		if(norand_picks[4] < 2 && norand_picks[7] < 2 && atmax == 1 && diversity < 4 && this.random.nextDouble() < 0.35)
		{
			ok = true;
		}
		
		List<Unit> extramages = new ArrayList<Unit>();
		if(ok)
		{
			extramages = this.generateExtraMages(primaries, this.getShuffledPrios(list));
			this.resolveAge(extramages);
			this.tagAll(extramages, "extramage");
			this.applyStats(extramages.get(0));


			
			extramages.get(0).caponly = this.random.nextBoolean();
			if((primaries > 1 || secondaries > 1) && caponlyprimaries)
				extramages.get(0).caponly = true;
			else if(!caponlyprimaries)
				extramages.get(0).caponly = true;
			

			
			if(extramages.get(0).caponly == false && this.random.nextDouble() > 0.5)
				extramages.get(0).commands.add(new Command("#slowrec"));
		
			for(Unit u : extramages)
				unitGen.handleMontagUnits(u, new MageMontagTemplate(nation, nationGen, 4), "montagmages");
		
			list.addAll(0, extramages);
			
			
		}

		// Handle montags
	

		
		// Apply filters
		applyMageFilters(list);
		
		//// Equip units
		
		/// Equip main mages
		
		// Primaries
		List<Unit> primarymages = mages.get(2);
		
		Unit u = primarymages.get(0);

		equipBase(u, 3);
		varyEquipment(u, primarymages, 3);

		// Secondaries
		List<Unit> secondarymages = mages.get(1);
		
		if(secondarymages.size() > 0)
		{
			u = secondarymages.get(0);
			
			if(secondarymages.size() == primarymages.size())
			{
				deriveEquipment(primarymages, secondarymages, 3, 2);
			}
			else
			{
				deriveEquipment(primarymages.get(0), u, 3, 2);
				varyEquipment(u, secondarymages, 2);
			}
		}
		
		// Tertiaries
		List<Unit> tertiarymages = mages.get(0);
		
		if(tertiarymages.size() > 0)
		{
			u = tertiarymages.get(0);
			
			if(secondarymages.size() > 0)
			{
				deriveEquipment(secondarymages.get(0), u, 2, 1);
			}
			else 
			{
				deriveEquipment(primarymages.get(0), u, 3, 1);
			}
		}
		
		
		/// Equip extra mage
		for(Unit un : extramages)
		{
			this.equipBase(un, 2);
		}
		
		determineSpecialLeadership(u, false);
		return list;
	}
	
	
	private void applyPriestFilters(List<Unit> list)
	{
		// priests
		if(list.size() > 0)
		{
			int power = 0;
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
			
			if(this.random.nextDouble() < 0.15 + maxStrength * 0.075)
			{
				power++;
				if(this.random.nextDouble() < 0.1 + maxStrength * 0.05)
				{
					power++;
					if(this.random.nextDouble() < 0.1 + maxStrength * 0.05)
					{
						power++;
						if(this.random.nextDouble() < 0.1 + maxStrength * 0.05)
						{
							power++;
						}
					}
				}
			}
			
			if(power == 0 && this.random.nextDouble() > 0.85)
				power = this.random.nextInt(3) + 2; // 2 to 4 
				
			String[] defaults = {"default_priestfilters"};
			this.applyFilters(list, power, defaults, "priestfilters");

		}
	}
	
	private void applyMageFilters(List<Unit> list2)
	{		
		
		List<Unit> list = new ArrayList<Unit>();
		list.addAll(list2);
		
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
	
		String[] defaults = {"default_magefilters", "default_magefilters_shapeshift", "default_magefilters_strongdefence"};
		//List<Filter> filters = ChanceIncHandler.retrieveFilters("magefilters", defaults, nationGen.filters, list.get(list.size() - 1).pose, list.get(list.size() - 1).race);

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
		
		if(this.random.nextDouble() < mod * 3)
		{
			power++;
			if(this.random.nextDouble() < mod * 2)
			{
				power++;
				if(this.random.nextDouble() < mod)
				{
					power++;
					if(this.random.nextDouble() < mod)
					{
						power++;
						if(this.random.nextDouble() < mod / 2)
						{
							power++;
							if(this.random.nextDouble() < mod / 2)
							{
								power++;
							}
						}
					}
				}
			}
		}
		

		if(power > 0 && this.random.nextDouble() > 0.5)
			power++;
		
		
		Unit extra = null;
		for(Unit u : list)
			if(!u.tags.contains("identifier schoolmage"))
			{
				extra = u;
				break;
			}
		list.remove(extra);
		this.applyFilters(list, power, defaults, "magefilters");

		
		// Filters for extra mage
		if(extra != null)
		{
			list.clear();
			list.add(extra);
			power = this.random.nextInt(3) + 2; // 2 to 4
			if(this.random.nextBoolean())
				power += this.random.nextInt(4); // 0 to 3;
			
			this.applyFilters(list, power, defaults, "magefilters");
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
		f.tags.addAll(p.tags);
		f.tags.add("do_not_show_in_descriptions");
		return f;
	}
	
	private int getVaryPoint(MagicPattern p, int primaries)
	{
		if(primaries <= p.getPathsAtleastAt(1, 100) && (this.random.nextDouble() > 0.8 && p.getPathsAtleastAt(3, 0) < 2))
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
	

	
	private List<Unit> generateExtraMages(int primaries, List<Integer> prio)
	{
		Race race = nation.races.get(0);
		
		double bonussecchance = 1;
		if(Generic.containsTag(race.tags, "secondaryracemagemod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(race.tags, "secondaryracemagemod"));
		if(Generic.containsTag(nation.races.get(1).tags, "primaryracemagemod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(nation.races.get(1).tags, "primaryracemagemod"));
		
		if((this.random.nextDouble() < 0.075 * bonussecchance) && nation.races.get(1).hasRole("mage"))
			race = nation.races.get(1);
		else if(!nation.races.get(0).hasRole("mage"))
			race = nation.races.get(1);

		List<Unit> bases = generateBases("mage", race, 1, 2);
		
		List<MagicPattern> available = getPatternsForTier(0, primaries);
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");
		MagicPattern pattern = Entity.getRandom(this.random, chandler.handleChanceIncs(available));

		
		if(prio == null)
		{
			prio = this.getPrios(bases.get(0));
		}
	

		MagicFilter f = new MagicFilter(nationGen);
		f.prio = prio;
		f.pattern = pattern;
		
		bases.get(0).color = nation.colors[2];
		bases.get(0).appliedFilters.add(f);
		
		this.equipBase(bases.get(0), 2);
		
		return bases;

	}
	
	public List<Unit> generatePriests()
	{		

		List<List<Unit>> all = nation.getMagesInSeparateLists();
		List<Unit> extras = all.get(3);
		
		
		Random r = this.random;
		
		
		double priestUpChance = 0.5;
		if(Generic.containsTag(Generic.getAllNationTags(nation), "higherpriestlevelchance"))
		{
			List<String> possiblevalues = Generic.getTagValues(Generic.getAllNationTags(nation), "higherpriestlevelchance");
			double highest = 0;
			for(String str : possiblevalues)
			{
				if(Double.parseDouble(str) > highest)
					highest = Double.parseDouble(str);
			}
			priestUpChance = highest;
		}
		
		int maxStrength = 1;
		if(r.nextDouble() < priestUpChance)
		{
			maxStrength++;
			if(r.nextDouble() < priestUpChance)
				maxStrength++;
		}
		

		if(Generic.containsTag(Generic.getAllNationTags(nation), "highestpriestlevel"))
		{
			List<String> possiblevalues = Generic.getTagValues(Generic.getAllNationTags(nation), "highestpriestlevel");
			int highest = 0;
			for(String str : possiblevalues)
			{
				if(Integer.parseInt(str) > highest)
					highest = Integer.parseInt(str);
			}
			maxStrength = highest;
		}
		
	
		
		double magePriestChance = 0.3;
		if(Generic.containsTag(Generic.getAllNationTags(nation), "magepriestchance"))
		{
			List<String> possiblevalues = Generic.getTagValues(Generic.getAllNationTags(nation), "magepriestchance");
			double highest = 0;
			for(String str : possiblevalues)
			{
				if(Double.parseDouble(str) > highest)
					highest = Double.parseDouble(str);
			}
			magePriestChance = highest;
		}
				
		boolean magePriests = r.nextDouble() < magePriestChance;
		
		// If we have extra mages, they can be priests 20% of the time if primary mages aren't
		boolean compensationMagePriests = (!magePriests && r.nextDouble() < 0.20);

		
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
		
		int priestextracost = 0;
		if(Generic.getTagValue(Generic.getAllNationTags(nation), "priestextracost") != null)
		{
			priestextracost = Integer.parseInt(Generic.getTagValue(Generic.getAllNationTags(nation), "priestextracost"));
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
						u.commands.add(new Command("#gcost", "+" + 10*currentStrength + currentStrength * priestextracost));
	
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
						u.commands.add(new Command("#gcost", "+" + 10*currentStrength + currentStrength * priestextracost));


					}
					done = true;
					atTier++;
					
					if(r.nextDouble() < 0.2)
					{
						doneWithMages = true;
					}
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
					u = deriveBasesFrom(1, "priest", priests.get(0), priests.get(0).race, currentStrength + 1, currentStrength).get(0);
				}
				else
				{
					int str = currentStrength;
					if(r.nextBoolean())
						str = Math.min(currentStrength + 1, 3);
					
					u = this.generateBases("priest", prace, 1, str).get(0);
				}
				u.commands.add(new Command("#gcost", "+" + 10*currentStrength ));

				u.color = priestcolor;
				u.appliedFilters.add(this.getPriestPattern(currentStrength));
				u.commands.add(new Command("#holy"));
				u.commands.add(new Command("#gcost", "+" + (int)(Math.pow(2, currentStrength)*10) + currentStrength * priestextracost));
				u.tags.add("priest " + currentStrength);

				
				u.commands.add(new Command("#mr", "+" + (2 + currentStrength)));

				

				
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
		
		
		// Apply filters
		applyPriestFilters(priests);

		// Equip priests
		int maxtier = 0;
		Unit parent = null;
		for(Unit u : priests)
		{
			int tier = Integer.parseInt(Generic.getTagValue(u.tags, "priest"));
			if(tier > maxtier)
				maxtier = tier;
		}
		
	
		for(int i = priests.size() - 1; i >= 0; i--)
		{

			Unit u = priests.get(i);
			int tier = Integer.parseInt(Generic.getTagValue(u.tags, "priest"));
			

			if(tier == maxtier)
			{
				equipBase(u, tier);
			}
			else
			{
				int ptier = Integer.parseInt(Generic.getTagValue(parent.tags, "priest"));
				this.deriveEquipment(parent, u, ptier, tier);
			}
			

			
			
			parent = u;
		}

		
		for(Unit u : priests)
		{
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
			{
				if(maxStrength == 1 && r.nextDouble() > 0.5)
					u.commands.add(new Command("#gcost", "+10"));
				else if(currentStrength == 1 && r.nextDouble() > 0.875)
					u.commands.add(new Command("#gcost", "+10"));
				else if(currentStrength == 1 && maxStrength > 1 && r.nextDouble() > 0.875)
				{
					u.commands.add(new Command("#noleader"));
					u.commands.add(new Command("#gcost", "-5"));
				}
				else
				{
					u.commands.add(new Command("#poorleader"));
					if(r.nextDouble() > 0.875)
					{
						u.commands.add(new Command("#command", "+30"));
						u.commands.add(new Command("#gcost", "+5"));
					}
				}
			}
			
			// Determine special leadership
			determineSpecialLeadership(u, true);
		}
		
		
		return priests;
	}
	
	public static void determineSpecialLeadership(Unit u, boolean isPriest)
	{
		boolean isUndead = false;
		
		String basiclevel = u.getLeaderLevel();
		
		List<String> others = new ArrayList<String>();
		for(Command c : u.commands)
			if(c.command.equals("#undead"))
			{
				isUndead = true;
				others.add("undead");
			}
			else if (c.command.equals("#demon"))
				others.add("undead");
			else if(c.command.equals("#magicbeing"))
				others.add("magic");
		
		for(String str : others)
		{
			// Undead priests almost all have elevated undead leadership, so if they have leadership bump it up a level
			if(!u.hasLeaderLevel(str) && str.equals("undead") && isUndead && isPriest)
			{
				if(basiclevel.equals("no"))
					u.commands.add(new Command("#noundeadleader"));
				else if(basiclevel.equals("poor"))
					u.commands.add(new Command("#okayundeadleader"));
				else
				{
					u.commands.add(new Command("#" + basiclevel + str + "leader"));
					u.commands.add(new Command("#undcommand +40"));
				}					
			}
			else if(!u.hasLeaderLevel(str))
				u.commands.add(new Command("#" + basiclevel + str + "leader"));
		}

		
	}
	
	/**
	 * The new method of actually applying filters. Used in HeroGenerator and here.
	 * @param units
	 * @param power
	 * @param filters
	 */
	public void applyFilters(List<Unit> units, int power, String[] defaults, String lookfor)
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

		
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");

		while(power > 0)
		{
			List<Filter> moreFilters = new ArrayList<Filter>();
			
			//if(this.random.nextDouble() > 0.1)
			//	moreFilters = ChanceIncHandler.getFiltersWithPower(power, power, filters);
			

			

			
			int rolls = 0;
			List<Unit> mages = new ArrayList<Unit>();
			
			int tier = 3;
			
			// Heroes and extra mages
			if(getMagesOfTier(units, 1).size() == 0 && getMagesOfTier(units, 2).size() == 0 && getMagesOfTier(units, 3).size() == 0)
			{
				mages.addAll(units);
				tier = 4;
			}
			// Other stuff
			else
			{
				while(mages.size() == 0 && rolls < 50)
				{
				
					tier = 3;
					if(this.random.nextDouble() > 0.65)
						tier = 2;
					else if(this.random.nextDouble() > 0.875)
						tier = 1;
				
					mages = getMagesOfTier(units, tier);
					
					rolls++;
				}
			}
	

			if(mages.size() == 0) // This happens for heroes and extra mages. An unlikely failsafe.
			{
				mages.addAll(units);
				tier = 4;
			}
			

			
			// Figure whether to give different filters for mages of tier or not
			boolean different = allHaveDistinguishingPath(mages);
			boolean common = MageGenerator.findCommonPaths(mages).size() > 0;
			different = different && (this.random.nextBoolean() || !common);
			
			// If montag on all units, they should get different filters
			boolean montags = true;
			for(Unit u : units)
			{
				boolean gotit = false;
				for(Command c : u.getCommands())
					if(c.command.equals("#montag"))
						gotit = true;
				
				if(!gotit)
				{
					montags = false;
					break;
				}
			}
			
			if(montags)
				different = true;
			
			
			// Get filters
			List<Filter> filters = ChanceIncHandler.retrieveFilters(lookfor, defaults, nationGen.filters, mages.get(mages.size() - 1).pose, mages.get(mages.size() - 1).race);

			int at = this.random.nextInt(power + 6) - 5; // -4 to power
			while(moreFilters.size() == 0 && at >= -5)
			{
				moreFilters = getFiltersForTier(ChanceIncHandler.getFiltersWithPower(at, power, filters), tier);
				if(different)
					moreFilters = ChanceIncHandler.getValidUnitFilters(moreFilters, mages);
				else
					moreFilters = ChanceIncHandler.getValidUnitFilters(moreFilters, mages.get(0));

				at--;
			}
			

			if(moreFilters.size() < 2)
			{
				moreFilters.clear();
				moreFilters.addAll(filters);
			}
			
			List<Filter> actualFilters = this.getFiltersForTier(moreFilters, tier);
			
			if(actualFilters.size() == 0)
			{
				System.out.println(moreFilters);
				System.out.println("No filters for tier " + tier + ", original " + moreFilters.size() + " / " + mages.get(0).race);
				return;
			}
			
		
			List<Filter> oldFilters = new ArrayList<Filter>();
			for(Unit u : units)
				oldFilters.addAll(u.appliedFilters);

			// If all mages of tier have distinguishing paths and either luck or no common paths, separate filters are rolled. 
			if(different)
			{
				double maxpower = 0;
				for(Unit u : mages)
				{
					List<Filter> givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findDistinguishingPaths(u, mages), strict);
					
					if(MageGenerator.findDistinguishingPaths(u, mages).size() == 0)
						givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findCommonPaths(mages), false);
					
					givenFilters.removeAll(u.appliedFilters);
	
					
						

					if(chandler.countPossibleFilters(givenFilters, u) == 0)
					{
						givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findDistinguishingPaths(u, mages), false);
						givenFilters.removeAll(u.appliedFilters);
					}
					

					// CanAdd check
					givenFilters = ChanceIncHandler.getValidUnitFilters(givenFilters, mages);
					

					// Use old filters when feasible!
					List<Filter> tempFilters = new ArrayList<Filter>();
					tempFilters.addAll(givenFilters);
					tempFilters.retainAll(oldFilters);
					tempFilters = ChanceIncHandler.getValidUnitFilters(tempFilters, mages);


					if(chandler.countPossibleFilters(tempFilters, u) > 0 && this.random.nextDouble() > 0.25)
						givenFilters = tempFilters;



					
					Filter f = Filter.getRandom(this.random, chandler.handleChanceIncs(u, givenFilters));

					if(f != null)
					{
						
						if(!handleMontagFilters(u, power, defaults, lookfor))
							u.appliedFilters.add(f);
			
						
						handleGiveToAll(f, units, tier);
	
						
						if(f.power > maxpower)
							maxpower = f.power;
					}
					else
						System.out.println("Nation " + nation.seed + " had a null filter for a mage. Power was " + power + " and tier " + tier + ".");
					
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
				if(chandler.countPossibleFilters(givenFilters, mages.get(0)) == 0)
				{
					givenFilters = new ArrayList<Filter>();
					givenFilters.addAll(actualFilters);
					for(Unit u : mages)
					{
						givenFilters.removeAll(u.appliedFilters);
					}
				}
				

				// CanAdd check
				givenFilters = ChanceIncHandler.getValidUnitFilters(givenFilters, mages);

				
				// Use old filters when feasible!
				List<Filter> tempFilters = new ArrayList<Filter>();
				tempFilters.addAll(givenFilters);
				tempFilters.retainAll(oldFilters);
				tempFilters = ChanceIncHandler.getValidUnitFilters(tempFilters, mages);


				if(chandler.countPossibleFilters(tempFilters, mages.get(0)) > 0 && this.random.nextDouble() > 0.25)
					givenFilters = tempFilters;
					

				
				Filter f = Filter.getRandom(this.random, chandler.handleChanceIncs(mages, givenFilters));
				
				for(Unit u : mages)
				{
					if(!handleMontagFilters(u, power, defaults, lookfor))
						u.appliedFilters.add(f);
				}
				
				handleGiveToAll(f, units, tier);
				
				power -= f.power;
			}
			
	
		}
	
	}
	
	
	private boolean handleMontagFilters(Unit u, int power, String[] defaults, String lookfor)
	{
		if(unitGen.getMontagUnits(u).size() == 0)
		{
			return false;
		}
		else
		{
			power = Math.max(1, power);
			this.applyFilters(unitGen.getMontagUnits(u), power, defaults, lookfor);
			
			return true;
		}

	}
	
	private void handleGiveToAll(Filter f, List<Unit> units, int tier)
	{
		if(f.tags.contains("givetoall") && tier < 4 && tier > 0)
			for(int i = 1; i <= 3; i++)
			{
				List<Unit> mages = getMagesOfTier(units, i);
				mages.addAll(nation.generateUnitList("montagmage"));
				for(Unit u : mages)
				{
					if(!u.appliedFilters.contains(f) && !f.tags.contains("notfortier " + i))
					{
						u.appliedFilters.add(f);
					}
				}
			}
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
			if(Generic.getTagValue(u.tags, "priest") != null)
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
				List<Integer> nottiers = new ArrayList<Integer>();
				for(String str : Generic.getTagValues(f.tags, "notfortier"))
					nottiers.add(Integer.parseInt(str));
		
				if(nottiers.contains(tier))
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
		
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");
		while(power > 0)
		{
			List<Filter> moreFilters = ChanceIncHandler.getFiltersWithPower(power, power, filters);
			

			if(chandler.handleChanceIncs(units, moreFilters).size() == 0 || this.random.nextDouble() > 0.7)
				moreFilters = ChanceIncHandler.getFiltersWithPower(power - 1, power, filters);
			if(chandler.handleChanceIncs(units, moreFilters).size() == 0 || power == 1)
				moreFilters = ChanceIncHandler.getFiltersWithPower(-100, power, filters);
			if(chandler.handleChanceIncs(units, moreFilters).size() == 0)
				break;
			

			Filter f = Filter.getRandom(this.random, chandler.handleChanceIncs(units, moreFilters));
			
			
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
	


	public List<List<Unit>> generateMageBases()
	{

		boolean primaryforeign = false;
		
		// PRIMARIES
		Race race = nation.races.get(0);
		
		double bonussecchance = 1;
		if(Generic.containsTag(race.tags, "secondaryracemagemod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(race.tags, "secondaryracemagemod"));
		if(Generic.containsTag(nation.races.get(1).tags, "primaryracemagemod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(nation.races.get(1).tags, "primaryracemagemod"));
		
		

		if(this.random.nextDouble() < 0.075 * bonussecchance && nation.races.get(1).hasRole("mage"))
		{
			if(this.hasPoseForTier("mage", nation.races.get(1), 3))
				race = nation.races.get(1);
		}
		else if(!nation.races.get(0).hasRole("mage")) // This hopefully never happens since it can cause issues unless handled very delicately.
			race = nation.races.get(1);
		
		if(race == nation.races.get(1))
			primaryforeign = true;
		
		int primaryamount = 1;
		
		double onechance = 0.75;
		double twochance = 0.2;
		double threechance = 0.05;
		
		if(Generic.containsTag(race.tags, "oneprimarychance"))
			onechance = Double.parseDouble(Generic.getTagValue(race.tags, "oneprimarychance"));
		if(Generic.containsTag(race.tags, "twoprimarychance"))
			twochance = Double.parseDouble(Generic.getTagValue(race.tags, "twoprimarychance"));
		if(Generic.containsTag(race.tags, "threeprimarychance"))
			threechance = Double.parseDouble(Generic.getTagValue(race.tags, "threeprimarychance"));
		
		double rand = this.random.nextDouble() * (onechance + twochance + threechance);

		if(rand < onechance)
			primaryamount = 1;
		else if(rand < onechance + twochance)
			primaryamount = 2;
		else
			primaryamount = 3;

		
		List<Unit> primaries = generateBases("mage", race, primaryamount, 3);
		


		
		
		
		// SECONDARIES
		boolean changed_at_secondary = false;
		if(this.random.nextDouble() < 0.1 * bonussecchance && race != nation.races.get(0)){/*do nothing*/}
		else
			race = nation.races.get(0);
		
		if((race == nation.races.get(0) && this.random.nextDouble() < 0.1 && hasPoseForTier("mage", nation.races.get(1), 2)) || !hasPoseForTier("mage", nation.races.get(0), 2))
		{
			changed_at_secondary = true;
			race = nation.races.get(1);
		}

		
		double randroll = this.random.nextDouble();
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
		if(secondaryamount > 0)
		{
			secondaries = deriveBasesFrom(secondaryamount, "mage", primaries.get(0), race, 3, 2);
		}
		
	
		// TERTIARIES
		List<Unit> tertiaries = new ArrayList<Unit>();
		
		// Tertiaries might be of secondary race if primaries aren't.
		bonussecchance = 1;
		if(Generic.containsTag(race.tags, "secondaryracetertiarymagemod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(race.tags, "secondaryracemagemod"));
		if(Generic.containsTag(nation.races.get(1).tags, "primaryracetertiarymagemod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(nation.races.get(1).tags, "primaryracemagemod"));
		
		if((0.05 * bonussecchance > this.random.nextDouble() && hasPoseForTier("mage", nation.races.get(1), 1)) || !hasPoseForTier("mage", nation.races.get(0), 1) || changed_at_secondary)
			race = nation.races.get(1);
		else
			race = nation.races.get(0);
		
		

		int tertiaryamount = 1;
		
		if((primaryamount + secondaryamount > 3 && this.random.nextDouble() < 0.25) || (primaryamount + secondaryamount > 4 && this.random.nextDouble() < 0.5) || primaryamount + secondaryamount > 5)
			tertiaryamount = 0;
	
		

		if(tertiaryamount > 0 && secondaryamount > 0)
		{
			tertiaries = deriveBasesFrom(tertiaryamount, "mage", secondaries.get(0), race, 2, 1);
		}
		else if(tertiaryamount > 0)
		{
			tertiaries = deriveBasesFrom(tertiaryamount, "mage", primaries.get(0), race, 3, 1);
		}
		


		
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
		if(current.tags.contains("notfortier " + futuretier))
			past = true;
		
		// We have a specific pose and current doesn't contain the required pose
		if(future && !current.tags.contains("tier " + futuretier))
			past = true;
		
		// Random chance
		if(future && this.random.nextDouble() < 0.075)
			past = true;
		
		if(oldrace != newrace)
			past = true;
		

			
		return past;
	}
	

	private List<Unit> deriveBasesFrom(int amount, String posename, Unit parent, Race race, int fromTier, int toTier)
	{
		
		
		List<Unit> units = new ArrayList<Unit>();
		
		Pose newpose = null;
		if(shouldChangePose(posename, race, parent.race, parent.pose, fromTier, toTier))
		{
			List<Pose> poses = this.getPossiblePoses(posename, race, toTier);
			if(poses.size() > 0)
				newpose = Entity.getRandom(this.random, chandler.handleChanceIncs(race, "mage", poses));
			
			

		}
		else
		{
			newpose = parent.pose;
		}
		
		
		for(int i = 0; i < amount; i++)
		{
			Unit nu = unitGen.generateUnit(race, newpose);
			units.add(nu);
		}
		
		return units;
		
	}
	
	public void deriveEquipment(Unit parent, Unit follower, int fromTier, int toTier)
	{
		List<Unit> parents = new ArrayList<Unit>();
		parents.add(parent);
		List<Unit> followers = new ArrayList<Unit>();
		followers.add(follower);
		
		deriveEquipment(parents, followers, fromTier, toTier);
	}

	
	private void polishUnit(Unit unit)
	{
		
		if(unit.getSlot("weapon") != null)
		{
			boolean troop = false;
			if(Generic.containsTag(Generic.getAllUnitTags(unit), "troopweapon"))
				troop = true;
			else if(Generic.containsTag(unit.getSlot("weapon").tags, "troopweapon"))
				troop = true;
	
			if(troop)
			{
				if(nationGen.weapondb.GetInteger(unit.getSlot("weapon").id, "lgt") >= 3)
				{
					if(unit.getSlot("offhand") != null && !unit.getSlot("offhand").armor)
						unit.setSlot("offhand", null);
				}
				else if(nationGen.weapondb.GetInteger(unit.getSlot("weapon").id, "2h") == 1)
				{
					unit.setSlot("offhand", null);
				}
			}	
		
		}
	}
	
	public void deriveEquipment(List<Unit> parents, List<Unit> followers, int fromTier, int toTier)
	{
		
		int max = parents.size();
		if(followers.size() < parents.size())
			max = followers.size();
		
		// Handle varyslot
		String oldslot = Generic.getTagValue(parents.get(0).tags, "varyslot");
		String newslot = null;
		
		if(followers.size() > 1)
		{
			List<String> possibleslots = this.getPossibleVarySlots(followers.get(0));
			if(possibleslots.contains(oldslot))
				newslot = oldslot;
			else
				newslot = possibleslots.get(random.nextInt(possibleslots.size()));
			
			tagAll(followers, "varyslot " + newslot);
		}
		
		ItemSet used = new ItemSet();
		for(int k = 0; k < max; k++)
		{
			Unit u = parents.get(k); // Old unit
			Unit nu = followers.get(k); // New unit
			
			for(String slot : u.slotmap.keySet())
			{
				if(slot.equals("overlay"))
					continue;
				
				if(u.pose.getItems(slot) == null || nu.pose.getItems(slot) == null || nu.getSlot(slot) != null)
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
					
					// Failure may result in using old armor in tier 2 to 1 
					if((newitem == null || !newitem.tags.contains("eliteversion " + u.getSlot(slot).name)) && slot.equals("armor") && toTier < 2 && fromTier == 2 && u.pose == nu.pose)
						newitem = u.getSlot(slot);
					

					
					if(newitem == null)
					{
						if(nu.pose == u.pose && (u.getSlot(slot).tags.contains("tier " + toTier) || !u.getSlot(slot).tags.contains("tier " + fromTier)))
						{
							newitem = u.getSlot(slot);
						}
					}
					if(newitem == null)
					{				
						
	
						if(all.possibleItems() > 1)
						{
							all.remove(u.getSlot(slot));

						}
						
						ItemSet temp = new ItemSet();
						temp.addAll(all);
						temp.removeAll(exclusions);
						if(chandler.countPossibleFilters(temp, nu) > 0)
						{
							all = temp;
						}
		
						newitem = Entity.getRandom(this.random, chandler.handleChanceIncs(nu, all));
					
					}
	
			
					if(newslot != null && slot.equalsIgnoreCase(newslot))
						exclusions.add(newitem);
					
					if(newitem != null)
					{
						nu.setSlot(slot, newitem);
						used.add(newitem);
					}
					
				}

			}
			
			



			// This should fill in missing slots like mount
			unitGen.armorUnit(nu, null, exclusions, "tier " + toTier, true);
			unitGen.armUnit(nu, null, exclusions, "tier " + toTier, true);
			

			
			if(toTier < 2)
			{
				if(Generic.containsTag(Generic.getAllUnitTags(nu), "mage_nolowtierhat"))
				{
					nu.setSlot("helmet", null);

				}
				if(!Generic.containsTag(Generic.getAllUnitTags(nu), "mage_lowtiercloak"))
				{
					nu.setSlot("cloakb", null);
					nu.setSlot("cloakf", null);
				}
			}
			
			
			
			this.polishUnit(nu);
			
		}
		
	
	}
	
	private Item getDerivedItem(Item item, Unit u, ItemSet used)
	{
		if(used == null)
			used = new ItemSet();
	

		ItemSet items = u.pose.getItems(item.slot).filterSameSprite(exclusions);
		items.removeAll(exclusions);
		
		if(chandler.countPossibleFilters(items, u) == 0)
		{
			items = u.pose.getItems(item.slot).filterSameSprite(exclusions);
		}
		
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
		if(chandler.handleChanceIncs(u, test).size() > 0)
		{
			possibles = test;
		}
		
		Item i = chandler.getRandom(possibles, u)
;
		

		return i;
	}
	
	
	public List<Unit> generateBases(String posename, Race race, int amount, int tier)
	{
		
		List<Unit> units = new ArrayList<Unit>();
		
	
		Pose p = Entity.getRandom(this.random, chandler.handleChanceIncs(race, posename, getPossiblePoses(posename, race, tier)));
		if(p == null)
			p = Entity.getRandom(this.random, chandler.handleChanceIncs(race, posename, getPossiblePoses(posename, race, tier - 1)));
	
		if(p == null)
			amount = 0;

		
		for(int i = 0; i < amount; i++)
		{
			Unit u = unitGen.generateUnit(race, p);
			units.add(u);
		}
		
		return units;

	}
	
	

	public void equipBase(Unit u, int tier)
	{
		
		unitGen.armorUnit(u, used, exclusions, "tier " + tier, false);
		unitGen.armUnit(u, used, exclusions, "tier " + tier, false);
		if(tier == 3)
		{
			u.setSlot("cloakb", unitGen.getSuitableItem("cloakb", u, exclusions, used, null));
		}
		

		if(!exclusions.contains(u.getSlot("weapon")))
			exclusions.add(u.getSlot("weapon"));
		
	
		this.polishUnit(u);
	}
	
	
	public List<Unit> generateaNew(String posename, Race race, int amount, int tier)
	{
		
		
		List<Unit> list = this.generateBases(posename, race, amount, tier);
		
		Unit u = list.get(0);
		
		this.equipBase(u, tier);
		
		if(tier == 3)
		{
			u.setSlot("cloakb", unitGen.getSuitableItem("cloakb", u, exclusions, used, null));
		}
		
	
		this.varyEquipment(u, list, tier);
		return list;
	}
	
	
	private boolean hasPoseForTier(String posename, Race race, int tier)
	{
		boolean isPrimaryRace = (race == nation.races.get(0));
		for(Pose p : race.getPoses(posename))
		{
			if(p.tags.contains("tier " + tier) && (isPrimaryRace || !p.tags.contains("primaryraceonly")))
				return true;
		}
		

		for(Pose p : race.getPoses(posename))
		{
			if(!p.tags.contains("notfortier " + tier) && (isPrimaryRace || !p.tags.contains("primaryraceonly")))
				return true;
		}
		
		return false;
		
	}
	
	private List<Pose> getPossiblePoses(String posename, Race race, int tier)
	{
		boolean isPrimaryRace = (race == nation.races.get(0));
		
		List<Pose> possibles = new ArrayList<Pose>();
		for(Pose p : race.getPoses(posename))
		{
			if(p.tags.contains("tier " + tier) && (isPrimaryRace || !p.tags.contains("primaryraceonly")))
			{
				possibles.add(p);
			}
		}
		
		if(possibles.size() == 0)
		{
			for(Pose p : race.getPoses(posename))
			{
				if(!p.tags.contains("notfortier " + tier) && (isPrimaryRace || !p.tags.contains("primaryraceonly")))
				{
					possibles.add(p);
				}
			}
		}
		
		
		if(possibles.size() == 0)
		{
			System.out.println("CRITICAL ERROR: No possible pose for " + race.name + " " + posename + " for tier " + tier + ". Nation seed: " + nation.seed +  " and main race " + nation.races.get(0));
		}
		return possibles;
			
	}
	
	
	private List<String> getPossibleVarySlots(Unit u)
	{
		//  Get slot to vary
		List<String> possibleslots = new ArrayList<String>();
		for(String tag : u.pose.tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("varyslot"))
				possibleslots.add(args.get(1));
		}
		
		if(possibleslots.size() == 0)
		{
			if(!u.pose.tags.contains("dontvaryhat") && u.pose.getItems("helmet") != null && chandler.countPossibleFilters(u.pose.getItems("helmet"), u) > 0)
			{
				possibleslots.add("helmet");
			}
			if(!u.pose.tags.contains("dontvaryweapon") && u.pose.getItems("weapon") != null && chandler.countPossibleFilters(u.pose.getItems("weapon"), u) > 0)
			{
				possibleslots.add("weapon");
			}
		}
		
		return possibleslots;
	}

	
	private void varyEquipment(Unit u, List<Unit> all, int tier)
	{
		int amount = all.size();
		
		// Copy gear
		for(Unit other : all)
		{
			if(other == u)
				continue;
			
			for(String slot : u.slotmap.keySet())
			{
				other.setSlot(slot, u.getSlot(slot));
			}
		}
	
		
		List<String> possibleslots = this.getPossibleVarySlots(u);
		
		if(possibleslots.size() == 0)
			return;
		
		String slot = possibleslots.get(this.random.nextInt(possibleslots.size()));
		
		// Vary item
		ItemSet stuff = u.pose.getItems(slot).filterTag("tier " + tier, true);
		stuff.removeAll(exclusions);
		stuff.remove(u.getSlot(slot));
		
		if(chandler.countPossibleFilters(stuff, u) == 0)
		{
			stuff = u.pose.getItems(slot).filterTag("tier " + tier, true);
			stuff.removeAll(exclusions);
			stuff.remove(u.getSlot(slot));
		}




		
		exclusions.add(u.getSlot(slot));
		
		for(int i = 1; i < amount; i++)
		{
			Unit nu = all.get(i);
			Item newitem = unitGen.getSuitableItem(slot, nu, exclusions, used, "tier " + tier);
			nu.setSlot(slot, newitem);
			exclusions.add(nu.getSlot(slot));
			
			
		}
		
		tagAll(all, "varyslot " + slot);
		for(Unit un : all)
			this.polishUnit(un);

	}
	
	


	
	public void resolveAge(List<Unit> units)
	{
		Random r = this.random;
		double random = r.nextDouble();
		for(Unit u : units)
		{
			
			double picks = u.getMagicAmount(20);
			double limit = 0.1 + (picks - 2) * 0.2; 
			if(picks < 4)
				limit = 0;
			
			if(random < limit && u.getSlot("mount") == null)
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
		m.tags.add("do_not_show_in_descriptions");
		
		return m;
	
	}

}
