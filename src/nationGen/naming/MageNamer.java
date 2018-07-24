package nationGen.naming;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.magic.MageGenerator;
import nationGen.misc.ChanceIncHandler;
import nationGen.nation.Nation;
import nationGen.rostergeneration.UnitGen;
import nationGen.units.Unit;

public class MageNamer extends Namer {
	
	protected Random random;
	private NationGen ng;
	public MageNamer(NationGen ng) {
		this.ng = ng;
		

	}


	
	public List<NamePart> names = new ArrayList<NamePart>();
	public List<NamePart> adjectives = new ArrayList<NamePart>();;
	public List<NamePart> nouns = new ArrayList<NamePart>();;
	public List<NamePart> tieredpriest = new ArrayList<NamePart>();;
	public List<NamePart> tieredmage = new ArrayList<NamePart>();;
	public List<NamePart> rankedprefix = new ArrayList<NamePart>();;
	public List<NamePart> extraparts_n = new ArrayList<NamePart>();;
	public List<NamePart> extraparts_a = new ArrayList<NamePart>();;



	protected void loadNameData()
	{
		
		for(String tag : n.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("magenames"))
			{
				names.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("mageadjectives"))
			{
				adjectives.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("magenouns"))
			{
				nouns.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("tieredpriestnames"))
			{
				tieredpriest.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("tieredmagenames"))
			{
				tieredmage.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("magerankedprefixes"))
			{
				rankedprefix.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("extranamenouns"))
			{
				extraparts_n.addAll(n.nationGen.magenames.get(args.get(1)));
			}
			else if(args.get(0).equals("extranameadjectives"))
			{
				extraparts_a.addAll(n.nationGen.magenames.get(args.get(1)));
			}
		}
		
		if(names.size() == 0)
			names = n.nationGen.magenames.get("defaultnames");
		if(adjectives.size() == 0)
			adjectives = n.nationGen.magenames.get("defaultadjectives");
		if(nouns.size() == 0)
			nouns = n.nationGen.magenames.get("defaultnouns");
		if(tieredpriest.size() == 0)
			tieredpriest = n.nationGen.magenames.get("defaulttieredpriestnames");
		if(tieredmage.size() == 0)
			tieredmage = n.nationGen.magenames.get("defaulttieredmagenames");
		if(rankedprefix.size() == 0)
			rankedprefix = n.nationGen.magenames.get("defaultprefixranks");
		if(extraparts_n.size() == 0)
			extraparts_n = n.nationGen.magenames.get("defaultextranamenouns");
		if(extraparts_a.size() == 0)
			extraparts_a = n.nationGen.magenames.get("defaultextranameadjectives");
	}
	
	
	
	public void execute(Nation n, NationGenAssets assets)
	{
		this.n = n;
		random = new Random(n.random.nextInt());
		loadNameData();
		
		
		List<List<Unit>> all = n.getMagesInSeparateLists();
		execute(all, 3);
		
		UnitGen ug = new UnitGen(ng, n, assets);
		for(List<Unit> l : all)
			for(Unit u : l)
			{
				for(Unit mu : ug.getMontagUnits(u))
				{
					mu.name = u.name.getCopy();
				}
			}
		
	}
	
	
	
	protected void execute(List<List<Unit>> all, int startTier)
	{
	
		List<Unit> primaries = all.get(0);
		List<Unit> secondaries = all.get(1);
		List<Unit> tertiaries = all.get(2);
				
		List<Unit> extras = null;
		if(all.size() > 3)
			extras = all.get(3);
		
		// prefixrank = <rank> <fixed name>
		// other = <rank based name>
		boolean prefixrank = random.nextBoolean();
		

		// whether extra special fun stuff should be suffix or prefix
		// ie apprentice storm druid or apprentice druid of astral graves
		// ie golden storm master or master of golden storm
		boolean suffix = random.nextBoolean();
		
		
		generateNewNames(primaries, startTier, prefixrank, suffix);

		if(secondaries.size() > 0)
			deriveNames(primaries, secondaries, startTier - 1, prefixrank, suffix);
		
		if(secondaries.size() > 0 && secondaries.size() <= primaries.size() && tertiaries.size() > 0)
			deriveNames(secondaries, tertiaries, startTier - 2, prefixrank, suffix);
		else if(tertiaries.size() > 0)
			deriveNames(primaries, tertiaries, startTier - 2, prefixrank, suffix);
		
		
		boolean didstuff = false;
		if(secondaries.size() > 0)
		{
			if(secondaries.get(0).name.suffix == null && secondaries.get(0).name.suffixprefix == null && secondaries.get(0).name.prefix == null && secondaries.get(0).name.prefixprefix == null)
				for(Unit u : secondaries)
					u.name.setPrefix(n.name + n.nationalitysuffix);
			
			didstuff = true;
		}
		if(tertiaries.size() > 0)
		{
			if((tertiaries.get(0).name.rankprefix == null || didstuff) && tertiaries.get(0).name.suffix == null && tertiaries.get(0).name.suffixprefix == null && tertiaries.get(0).name.prefix == null && tertiaries.get(0).name.prefixprefix == null)
				for(Unit u : tertiaries)
					u.name.setPrefix(n.name + n.nationalitysuffix);
		}
		
	
		if(extras != null && extras.size() > 0)
			generateNewNames(extras, 2, false, random.nextBoolean());

		
		


	}
	
	protected List<Integer> getPathInformation(NamePart ff)
	{
		List<Integer> paths = new ArrayList<Integer>();
		for(String str : ff.chanceincs)
		{
			List<String> args = Generic.parseArgs(str);
			if(args.get(0).equals("personalmagic"))
			{
				for(int i = 0; i < 9; i++)
					if(args.contains(Generic.integerToPath(i)) && !paths.contains(i))
						paths.add(i);
						
			}
		}
		return paths;
	}
	
	
	public void deriveNames(List<Unit> from, List<Unit> to, int rank, boolean prefixrank, boolean suffix)
	{

		
		List<NamePart> source = tieredmage;
		if(prefixrank)
			source = rankedprefix;
		
		ChanceIncHandler cha = new ChanceIncHandler(n);
		NamePart ff = null;
		LinkedHashMap<NamePart, Double> filters = null;
		boolean noname = false;
		
		do
		{	
			filters = cha.handleChanceIncs(to.get(0), this.filterByRank(source, rank));
			if(filters.size() == 0)
			{
				filters = cha.handleChanceIncs(to.get(0), source);
			}
			
			ff = NamePart.getRandom(random, filters);

			
		} while((ff.name.equals(from.get(0).name.type) && !prefixrank)|| (ff.name.equals(from.get(0).name.rankprefix) && prefixrank));
		

		
		boolean swapnoun = random.nextBoolean();
		double modify = random.nextDouble();
		
		List<String> used = new ArrayList<String>();
		for(Unit u2 : from)
			used.add(u2.name.type.toString());
		for(int i = to.size() - 1; i >= 0; i--)
		{
			Unit u = to.get(i);

			if(to.size() == from.size())
			{
				Unit u2 = from.get(i);

				u.name = u2.name.getCopy();
				
				
			}
			else if(to.size() < from.size())
			{
				Name same = getSameParts(from);
				if(same.suffix == null && same.suffixprefix != null)
				{
					same.prefix = same.suffixprefix;
					same.suffixprefix = null;	
				}
				else if(same.suffix != null && same.suffixprefix == null && from.get(0).name.suffixprefix != null)
				{
					same.pluralsuffix = true;
				}
								

				u.name = same;
				
			}
			else // to.size() > from.size()
			{
				
				source = new ArrayList<NamePart>();
				//source.addAll(filterByRank(tieredmage, rank));
				source.addAll(names);
				
				Name same = getSameParts(from);
				if(same.suffix == null && same.suffixprefix != null)
				{
					same.prefix = same.suffixprefix;
					same.suffixprefix = null;	
				} 
				else if(modify < 0.5) // extra part, experimental
				{
					if(same.suffixprefix == null && same.suffix != null)
					{
						same.prefix = same.suffix;
						same.suffix = null;
					}
					else if(same.suffixprefix != null && same.suffix != null)
					{
						if(!swapnoun)
							same.prefix = same.suffixprefix;
						else
							same.prefix = same.suffix;

						same.suffixprefix = null;
						same.suffix = null;
					}
				}
			
				
				List<NamePart> available = new ArrayList<NamePart>();
				available.addAll(source);
				available = this.filterByPaths(available, MageGenerator.findDistinguishingPaths(u, to), true);
				if(available.size() == 0)
					available.addAll(source);
				
		
				NamePart ff2 = null;
				do
				{	
					filters = cha.handleChanceIncs(u, this.filterByRank(available, rank));
					if(filters.size() == 0)
					{
						filters = cha.handleChanceIncs(u, available);
					}
					ff2 = NamePart.getRandom(random, filters);
				} while(used.contains(ff2.getCopy()));
				
				used.add(ff2.toString());
				
				u.name = same;
				u.name.rankprefix = null;
				u.name.type = ff2.getCopy();
			}
			
			
			if(u.name.type == null || u.name.type.equals(""))
			{
				List<NamePart> available = new ArrayList<NamePart>();
				
				if(!prefixrank)
					available.addAll(tieredmage);
				else
					available.addAll(filterByPaths(names, MageGenerator.findDistinguishingPaths(u, to), false));
	
					
				NamePart ff2 = null;
				do
				{	
					filters = cha.handleChanceIncs(u, this.filterByRank(available, rank));
					if(filters.size() == 0)
					{
						filters = cha.handleChanceIncs(u, available);
					}
					
					ff2 = NamePart.getRandom(random, filters);
				} while(used.contains(ff2.toString()));
				used.add(ff2.toString());
				u.name.type = ff2.getCopy();
				noname = true;
			}

			if(to.size() <= from.size())
			{
				if(prefixrank && !noname)
					u.name.rankprefix = ff.getCopy();
				else if(!prefixrank)
					u.name.type = ff.getCopy();
			}
						
		}
		
		
	}
	
	private Name getSameParts(List<Unit> units)
	{
		Name name = units.get(0).name.getCopy();
		for(Unit u : units)
		{
			if(u.name.rankprefix != null && !u.name.rankprefix.equals(name.rankprefix))
				name.rankprefix = null;
			if(u.name.prefix != null && !u.name.prefix.equals(name.prefix))
				name.prefix = null;
			if(u.name.prefixprefix != null && !u.name.prefixprefix.equals(name.prefixprefix))
				name.prefixprefix = null;
			if(u.name.suffixprefix != null && !u.name.suffixprefix.equals(name.suffixprefix))
				name.suffixprefix = null;
			if(u.name.suffix != null && !u.name.suffix.equals(name.suffix))
				name.suffix = null;
			if(u.name.suffix != null && !u.name.type.equals(name.type))
				name.type = null;
		}
		
		return name;
	}
	
	public void generateNewNames(List<Unit> primaries, int rank, boolean prefixrank, boolean suffix)
	{
		ChanceIncHandler cha = new ChanceIncHandler(n);
		
		//boolean twopartsuffixdefinite = false;
		

		// Prefix or suffix crap
		boolean noun = random.nextBoolean();


			
		// Should it be a suffix if it's a noun?
		boolean shouldbesuffix = random.nextBoolean();
		
		// Get common paths
		List<Integer> commons = new ArrayList<Integer>();
		

		for(int j = 5; j > 0; j--)
		{
			for(int i = 0; i < 9; i++)
			{
				boolean has = true;
				for(Unit u : primaries)
				{
					if(u.getMagicPicks()[i] < j || commons.contains(i))
						has = false;	
				}
				if(has)
					commons.add(i);
			}
			
			if(commons.size() >= 2)
				break;
		}
		
		
		// Vary name instead of some other parT?
		boolean varyname = false;
		if(prefixrank && primaries.size() > 1 && commons.size() > 0)
			varyname = random.nextDouble() > 0.85;
			
		// Base name "mage" "necromancer" etc
		List<NamePart> source = null;
		if(prefixrank && varyname)
		{
			if(noun)
				source = nouns;				
			else
				source = adjectives;
		}
		else if(prefixrank)
			source = names;
		else
			source = filterByRank(tieredmage, rank);
	
		boolean strict = random.nextBoolean();
		

		LinkedHashMap<NamePart, Double> filters = cha.handleChanceIncs(primaries.get(0), filterByPaths(source, commons, strict));


		if(filters.size() == 0)
		{
			filters = cha.handleChanceIncs(primaries.get(0), source);

		}
		NamePart ff = NamePart.getRandom(random, filters);

		for(Unit u : primaries)
		{
			if(!varyname)
				u.name.type = ff.getCopy();
			else
			{
				
				if(ff.tags.contains("nosuffix"))
					shouldbesuffix = false;
				else if(ff.tags.contains("noprefix"))
					shouldbesuffix = true;
				
				
				if(!noun || !shouldbesuffix) // Is noun or should be prefix -> Nouns for prefix and all kinds of adjectives
				{
					if(ff != null)
						u.name.prefixprefix = ff.getCopy();
				}
				else
				{
					if(ff != null)
						u.name.suffix = ff.getCopy();
				}
			}
				
		}
		
	
		// If rank is a prefix, set it
		if(prefixrank)
		{

			filters = cha.handleChanceIncs(primaries.get(0), filterByRank(rankedprefix, rank));
			if(filters.size() == 0)
				filters = cha.handleChanceIncs(primaries.get(0), rankedprefix);
			
			NamePart prefixf = NamePart.getRandom(random, filters);
			for(Unit u : primaries)
				u.name.rankprefix = prefixf.getCopy();
			

		}
		

		// Break now for single path mages (= priests usually) if the basic name contains information already
		if(primaries.size() == 1 && commons.size() == 1 && (getPathInformation(ff).contains(commons.get(0)) || commons.contains(8)))
		{
			return;
		}
		
		// Get an extra name part. Not for prefixrank mages to avoid super long names.
		NamePart extra = null;
		if((commons.size() > 1) && !prefixrank)
		{
			List<NamePart> source2 = new ArrayList<NamePart>();
			if(noun)
			{
				source2.addAll(adjectives);
				source2.addAll(extraparts_a);
			}
			else
			{
				source2.addAll(nouns);
				source2.addAll(extraparts_n);
			}
						
			filters = cha.handleChanceIncs(primaries.get(0), filterByPaths(source2, commons, false));
			if(filters.size() == 0)
				filters = cha.handleChanceIncs(primaries.get(0), source2);
			extra = NamePart.getRandom(random, filters);
		}
		
		// Set rest of the name
		for(Unit u : primaries)
		{
			List<Integer> d = MageGenerator.findDistinguishingPaths(u, primaries);
		
			if(d.size() == 0)
				d = commons;
	
			if(varyname)
			{
				filters = cha.handleChanceIncs(u, filterByPaths(names, d, true));

				if(filters.size() == 0)
					filters = cha.handleChanceIncs(primaries.get(0), names);
				
			}
			else if(noun)
			{
				filters = cha.handleChanceIncs(u, filterByPaths(nouns, d, false));
				if(filters.size() == 0)
					filters = cha.handleChanceIncs(primaries.get(0), nouns);
			}
			else
			{
				filters = cha.handleChanceIncs(u, filterByPaths(adjectives, d, false));
				if(filters.size() == 0)
					filters = cha.handleChanceIncs(primaries.get(0), adjectives);
			}

			NamePart suffixpart = NamePart.getRandom(random, filters);
			
			if(suffixpart.tags.contains("nosuffix"))
				shouldbesuffix = false;
			else if(suffixpart.tags.contains("noprefix"))
				shouldbesuffix = true;
			
			if(varyname)
			{
				if(suffixpart != null)
					u.name.type = suffixpart.getCopy();
			}
			else if(!noun || !shouldbesuffix) // Is noun or should be prefix -> Nouns for prefix and all kinds of adjectives
			{
				if(suffixpart != null)
					u.name.prefixprefix = suffixpart.getCopy();
			}
			else
			{
				if(suffixpart != null)
					u.name.suffix = suffixpart.getCopy();
			}


			
			if(!prefixrank && extra != null && shouldbesuffix)
			{
				if(noun)
				{
					u.name.suffixprefix = extra.getCopy();
				}
				else
				{
					suffixpart = extra;
					u.name.suffix = extra.getCopy();
					u.name.suffixprefix = u.name.prefixprefix;
					u.name.prefixprefix = null;
				}

			}

			if(u.name.suffix != null && suffixpart != null && suffixpart.name.equals(u.name.suffix.name))
			{
				boolean def = suffixpart.tags.contains("definitesuffix");
				boolean plur = suffixpart.tags.contains("pluralsuffix");
				
				if(def)
					u.name.definitesuffix = true;
				if(plur)
					u.name.pluralsuffix = true;
			
			}
			if(u.name.suffix != null && extra != null && extra.name.equals(u.name.suffix.name))
			{

				boolean def = extra.tags.contains("definitesuffix");
				boolean plur = extra.tags.contains("pluralsuffix");
				
				if(def)
					u.name.definitesuffix = true;
				if(plur)
					u.name.pluralsuffix = true;
			}
			if(u.name.suffix != null && ff != null && ff.name.equals(u.name.suffix.name))
			{

				boolean def = ff.tags.contains("definitesuffix");
				boolean plur = ff.tags.contains("pluralsuffix");
				
				if(def)
					u.name.definitesuffix = true;
				if(plur)
					u.name.pluralsuffix = true;
			
			}


		}
		
		

	}
	

	
	protected List<NamePart> filterByRank(List<NamePart> orig, int rank)
	{
		List<NamePart> list = new ArrayList<NamePart>();
		for(NamePart f : orig)
		{
			if(f.rank == rank)
				list.add(f);
		}
		return list;
	}
	
	protected List<NamePart> filterByPaths(List<NamePart> orig, List<Integer> paths, boolean strict)
	{
		List<NamePart> list = new ArrayList<NamePart>();
		for(NamePart f : orig)
		{
			boolean ok = strict;
			boolean hadChanceInc = false;
			for(String c : f.chanceincs)
			{
				
				List<String> args = Generic.parseArgs(c);
				if(args.get(0).equals("personalmagic"))
				{
					hadChanceInc = true;
					for(int i = 0; i < 9; i++)
					{
						String path = Generic.integerToPath(i);
						if(args.contains(path))
						{
							if(!paths.contains(i) && strict)
							{
								ok = false;
							}
							else if(paths.contains(i) && !strict)
							{
								ok = true;
							}
						}
					}
				}

			}
			if(!hadChanceInc && (paths.size() == 0 || !strict))
				ok = true;
			else if(f.tags.contains("alwaysok"))
				ok = true;
			else if(strict && !hadChanceInc)
				ok = false;
			
			if(ok)
			{
				list.add(f);
			
			}
			
		}
		
		return list;
	}
	
	
	
}
