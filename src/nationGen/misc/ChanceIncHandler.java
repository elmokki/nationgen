package nationGen.misc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;








import com.elmokki.Generic;









import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.nation.Nation;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.Unit;

public class ChanceIncHandler {
	
	private Nation n;
	private List<String> themeincs = new ArrayList<String>();
	
	public ChanceIncHandler(Nation n)
	{
		this.n = n;
		
		for(Theme t : n.themes)
		{
			this.themeincs.addAll(t.themeincs);
		}

		
	}
	
	public <T extends Filter> LinkedHashMap<T, Double> handleChanceIncs(List<T> filters)
	{
		List<Unit> units = new ArrayList<Unit>();
		return handleChanceIncs(units, filters);
	}
	
	
	public <T extends Filter> LinkedHashMap<T, Double> handleChanceIncs(Unit u, List<T> filters)
	{
		List<Unit> units = new ArrayList<Unit>();
		units.add(u);
		return handleChanceIncs(units, filters);
	}
	
	public <T extends Filter> List<T> removeRelated(T thing, List<T> list)
	{
		
		List<T> shit = new ArrayList<T>();
		
		list.remove(thing);
		for(String type : thing.types)
			for(T t : list)
				if(t.types.contains(type))
				{
					shit.add(t);
					continue;
				}
		
		list.removeAll(shit);
		
		return list;
	}
	
	/**
	 * The main method for handling chanceincs.
	 * @param u
	 * @param filters
	 * @return
	 */
	public <T extends Filter> LinkedHashMap<T, Double> handleChanceIncs(List<Unit> u, List<T> filters)
	{

		LinkedHashMap<T, Double> set = new LinkedHashMap<T, Double>();
		for(T t : filters)
		{
			set.put(t, t.basechance);
		}
		
		handleThemeIncs(set);
		handleNationIncs(set, n);

		for(Unit un : u)
			handleUnitIncs(set, un);
		
		
		List<T> redundantFilters = new ArrayList<T>();
		
		for(T f : set.keySet())
		{
			if(set.get(f) <= 0)
				redundantFilters.add(f);
		}
		
		for(Filter f : redundantFilters)
		{
			if(set.keySet().contains(f))
				set.remove(f);
		}

		
		

		return set;
	}
	
	public static boolean suitableFor(Unit u, Filter f, Nation n)
	{
		for(String role : u.pose.roles)
			if(f.tags.contains("nopose " + role))
				return false;
		
		if(n != null)
		{
			Race r = n.races.get(0);
			if(f.tags.contains("norace primary") && u.race == r)
				return false;
			if(f.tags.contains("norace secondary") && u.race != r)
				return false;
		}
		return true;
	}
	
	public static boolean canAdd(Unit u, Filter f)
	{
		if(!suitableFor(u, f, null))
			return false;
		
		
		List<String> primaries = new ArrayList<String>();
		if(Generic.containsTag(f.tags, "primarycommand"))
		{
			for(String tag : f.tags)
			{
				List<String> args = Generic.parseArgs(tag);
				if(args.get(0).equals("primarycommand"))
				{
					primaries.add(args.get(1));
				}
			}
		}
		
		boolean ok = false;
		boolean primarycommandfail = false;
		for(Command c : u.getCommands())
		{

			boolean tempok = true;
			if(primaries.contains(c.command))
			{
				primarycommandfail = true;
				ok = false;
				break;
			}

			
			for(Command fc : f.commands)
			{

				if(c.command.equals(fc.command) && c.args.size() == 0 && fc.args.size() == 0)
				{
					tempok = false;
					break;
				}
			}
			
			if(tempok)
				ok = true;
		}
		

		
		if(f.commands.size() == 0 && Generic.containsTag(f.tags, "lowenctreshold") && !primarycommandfail)
		{
			int treshold = Integer.parseInt(Generic.getTagValue(f.tags, "lowenctreshold"));
			
			int enc = 0;
			if(u.getSlot("armor") != null)
				enc += u.nationGen.armordb.GetInteger(u.getSlot("armor").id, "enc");
			if(u.getSlot("offhand") != null && u.getSlot("offhand").armor)
				enc += u.nationGen.armordb.GetInteger(u.getSlot("offhand").id, "enc");
			if(u.getSlot("helmet") != null)
				enc += u.nationGen.armordb.GetInteger(u.getSlot("helmet").id, "enc");
			
			
			ok = (enc <= treshold);

			

		}
		
		if(!ok)
			return false;
		
		return canAdd(u.appliedFilters, f);
	}
	
	public static <E extends Filter> boolean canAdd(List<E> filters, Filter f)
	{



		// Forbid the same type
		for(Filter f2 : filters)
		{
			for(String s : f.types)
			{
				if(f2.types.contains(s))
				{
					
					return false;
				}
			}
		}  
		

		
		return true;
		

	}
	
	public static List<Filter> getFiltersWithPower(int min, int max, List<Filter> orig)
	{
		List<Filter> newList = new ArrayList<Filter>();
		for(Filter f : orig)
		{
			if(f.power <= max && f.power >= min)
				newList.add(f);
		}
		return newList;
	}
	
	
	/**
	 * Checks ChanceIncHandler.canAdd() for all filters and removes bad ones.
	 * @param filters
	 * @param units
	 * @return
	 */
	public static List<Filter> getValidUnitFilters(List<Filter> filters, List<Unit> units)
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
	 * Checks ChanceIncHandler.canAdd() for all filters and removes bad ones.
	 * @param filters
	 * @param units
	 * @return
	 */
	public static <E extends Filter> List<E> getValidFilters(List<E> filters, List<E> oldfilters)
	{
		List<E> list = new ArrayList<E>();
		
		for(E f : filters)
		{
			if(ChanceIncHandler.canAdd(oldfilters, f))
				list.add(f);
	
		}
		
		return list;
	}
	
	
	public static <E extends Filter> List<E> retrieveFilters(String lookfor, String defaultset, ResourceStorage<E> source, Pose p, Race r)
	{
		List<E> filters = new ArrayList<E>();
		
		boolean empty = true;
		
		
		if(p != null)
			for(String tag : p.tags)
			{
				if(tag.startsWith(lookfor))
				{
					empty = false;
					filters.addAll(source.get(tag.split(" ")[1]));
				}
			}
	
		
		if(empty && r != null)
		{
			for(String tag : r.tags)
			{
				if(tag.startsWith(lookfor))
				{
					empty = false;
					filters.addAll(source.get(tag.split(" ")[1]));
				}
			}
		}
		
		
		if(empty)
			filters.addAll(source.get(defaultset));

		return filters;

	}
	
	private List<Integer> pathsAtHighest(int[] unitpaths)
	{
		List<Integer> atHighest = new ArrayList<Integer>();
		int highest = 0;
		for(int i = 5; i > 0; i--)
		{
			highest = i;
			int[] paths = unitpaths;
			for(int j = 0; j < 9; j++)
			{
				if(paths[j] == i && !atHighest.contains(j))
				{
					atHighest.add(j);
				}
			}
			if(atHighest.size() > 0)
				break;
			
			atHighest.clear();
		}
		
		List<Integer> atSecondHighest = new ArrayList<Integer>();
		int secondHighest = 0;
		for(int i = highest - 1; i > 0; i--)
		{
			secondHighest = i;
			int[] paths = unitpaths;
			for(int j = 0; j < 9; j++)
			{
				if(paths[j] == i && !atHighest.contains(j))
				{
					atSecondHighest.add(j);
				}
			}
			if(atSecondHighest.size() > 0)
				break;
			
			atSecondHighest.clear();
		}
		
		if(highest - secondHighest == 1 && atHighest.size() == 1 && atHighest.size() + atSecondHighest.size() < 4)
		{
			atHighest.addAll(atSecondHighest);
		}
		
		return atHighest;
	}
	
	
	
	private double applyMod(double value, String modifier)
	{
		if(modifier.startsWith("+"))
		{
			modifier = modifier.substring(1);
			value += Double.parseDouble(modifier);
		}
		else if(modifier.startsWith("*"))
		{
			modifier = modifier.substring(1);
			value *= Double.parseDouble(modifier);
		}
		else if(modifier.startsWith("/"))
		{
			modifier = modifier.substring(1);
			value /= Double.parseDouble(modifier);
		}
		else
		{

			value += Double.parseDouble(modifier);
		}
		return value;
	}
	
	
	
	
	public Double applyModifier(double value, String modifier)
	{
		String[] mod = modifier.split(" or ");
		double[] results = new double[mod.length];
		
		if(mod.length == 0)
			return value;
		
		boolean max = true;
		if(modifier.startsWith("max("))
			modifier = modifier.substring(4, modifier.length() - 2);
		if(modifier.startsWith("min("))
		{
			modifier = modifier.substring(4, modifier.length() - 2);
			max = false;
		}
		
		

		double biggest = applyMod(value, mod[0]);;
		for(int i = 0; i < results.length; i++)
		{
			results[i] = applyMod(value, mod[i]);
			
			
			if(results[i] > biggest && max)
				biggest = results[i];
			else if(results[i] < biggest && !max)
				biggest = results[i];
		}
		
		return biggest;
			
	}
	
	/**
	 * This is a separate method for chanceincs that target the filter/item/whatever itself - ie stuff that is relevant 
	 * for themes only since in general targetting the thing itself is pointless.
	 * 
	 * All theme chanceincs SHOULD NOT be implemented here. If the chanceinc does not target the filter itself,
	 * for example not something like "higher chance if item contains theme 'advanced'", it is usable in other
	 * places as well.
	 * 
	 * @param filters
	 * @param n
	 */
	private <T extends Filter> void handleThemeIncs(LinkedHashMap<T, Double> filters)
	{
		for(T f : filters.keySet())
		{
			List<String> chanceincs = new ArrayList<String>();
			chanceincs.addAll(themeincs);
			
			for(String str : chanceincs)
			{
				
				List<String> args = Generic.parseArgs(str);
	
				// Theme
				if(args.get(0).equals("theme") && args.size() >= 3)
				{
					boolean not = args.contains("not");
					if(f.tags.contains(args.get(args.size() - 2)) != not)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
					}
				}
				

			}
			
			
		}
	}
	
	private <T extends Filter> void handleNationIncs(LinkedHashMap<T, Double> filters,  Nation n)
	{

		List<Unit> tempmages = n.generateComList("mage");
		
		int[] paths = new int[9];
		for(Unit u : tempmages)
		{						
			for(String tag : u.tags)
				if(tag.startsWith("schoolmage"))
				{
					int[] picks = u.getMagicPicks();
					for(int j = 0; j < 9; j++)
					{
						if(paths[j] < picks[j])
							paths[j] = picks[j];
					}
				}
		}
		
		List<Integer> atHighest = this.pathsAtHighest(paths);

		// Avg resources and gold
		int unitCount = 0;
		double totalgold = 0;
		double totalres = 0;
		
		for(Unit u : n.generateTroopList())
		{
			unitCount++;
			if(u.pose.roles.contains("mounted") || u.pose.roles.contains("chariot"))
				totalgold += u.getGoldCost()  * 0.66;
			else
				totalgold += u.getGoldCost();
			
			totalres += u.getResCost(true);
		}
		
		double avgres = totalres / unitCount;
		double avggold = totalgold / unitCount;
		
		
		
		
		// Do chanceincs!
		for(T f : filters.keySet())
		{
			
			List<String> chanceincs = new ArrayList<String>();
			chanceincs.addAll(f.chanceincs);
			chanceincs.addAll(themeincs);
			
			for(String str : chanceincs)
			{
				
				List<String> args = Generic.parseArgs(str);
	
				// Magic paths
				boolean canIncrease = true;
				if(args.get(0).equals("magic") && args.size() >= 3)
				{
					for(int i = 1; i < args.size() - 1; i++)
					{
						int path = Generic.PathToInteger(args.get(i));
						if(!atHighest.contains(path))
							canIncrease = false;
					}
					
					if(canIncrease)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
					}
				}
				
				// Existing shapeshifts
				canIncrease = false;
				if(args.get(0).equals("shape") && args.size() >= 3)
				{
					String shape = args.get(1);
					for(ShapeChangeUnit u : n.secondshapes)
					{
						if(u.thisForm.name.equals(shape))
							canIncrease = true;			
					}
					
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}

				// Era
				canIncrease = false;
				if(args.get(0).equals("era") && args.size() >= 3)
				{
					int era = Integer.parseInt(args.get(1));
					if(era == n.era)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				// Primary race
				canIncrease = false;
				if(args.get(0).equals("hasprimaryrace") && args.size() >= 2)
				{
					if(n.races.size() > 0)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				// Nation commands
				canIncrease = false;
				if(args.get(0).equals("nationcommand") && args.size() >= 3)
				{
					String command = args.get(1);
					
					int dir = 0;
					if(args.contains("below"))
						dir = -1;
					if(args.contains("above"))
						dir = 1;
					
					
						
					List<Command> coms = n.getCommands();
					coms.addAll(n.races.get(0).nationcommands);
					for(Command c : coms)
					{
						if(c.command.equals(command))
						{
					
							
							String arg = c.args.get(0);

							
							if(dir == -1 && Integer.parseInt(arg) < Integer.parseInt(args.get(args.size() - 2)))
							{
								canIncrease = true;
							}
							else if(dir == 1 && Integer.parseInt(arg) > Integer.parseInt(args.get(args.size() - 2)))
							{
								canIncrease = true;
							}
							else if(dir == 0)
							{
								canIncrease = true;
							}
								
						}
					}
					
			

					
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				
				// Unit commands
				canIncrease = false;
				if(args.get(0).equals("command") && args.size() >= 3)
				{
					String command = args.get(1);
					List<Unit> units = n.generateTroopList();
					units.addAll(n.generateComList("mage"));
					units.addAll(n.generateComList("priest"));
					
					for(Unit u : units)
					{
						for(Command c : u.getCommands())
						{
							if(c.command.equals(command))
							{
								if(args.size() > 3)
								{
									int level = 0;
									boolean above = true;
									if(Generic.isNumeric(args.get(2)))
									{
										level = Integer.parseInt(args.get(2));
									}
									else if(args.size() > 4)
									{
										if(args.contains("below"))
											above = false;
										
										level = Integer.parseInt(args.get(3));
									}
									else
										canIncrease = true;
									
									
									if(!canIncrease)
									{
										if((Integer.parseInt(c.args.get(0)) >= level) == above)
										{
											canIncrease = true;
										}

										
									}
									
								}
								else
									canIncrease = true;
							}
							
						}
					}
					if(canIncrease)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
					}
				}
				
				// Percentage of command
				canIncrease = false;
				if(args.get(0).equals("percentageofcommand") && args.size() >= 3)
				{
					double all = 0;
					double found = 0;
					
					String command = args.get(1);
					List<Unit> units = n.generateTroopList();
					units.addAll(n.generateComList("mage"));
					units.addAll(n.generateComList("priest"));
					
					for(Unit u : units)
					{
						all++;
						for(Command c : u.getCommands())
						{
							if(c.command.equals(command))
							{
								found++;
							}
							
						}
					}
					
					double share = found/all;
					boolean below = args.contains("below");
					double required = Double.parseDouble(args.get(args.size() - 2));
					if((share < required) == below)
						canIncrease = true;
					
					if(canIncrease)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
					}
				}
				
				// Percentage of race
				canIncrease = false;
				if(args.get(0).equals("percentageofrace") && args.size() >= 3)
				{

					Race r = null;
					for(Race r2 : n.races)
						if(args.get(1).toLowerCase().equals(r2.name.toLowerCase()))
								r = r2;
					
					if(r != null)
					{
						double share = n.percentageOfRace(r);
						boolean below = args.contains("below");
						double required = Double.parseDouble(args.get(args.size() - 2));
						if((share < required) == below)
							canIncrease = true;
					}
					
					if(canIncrease)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
					}
				}
				
				// Race
				canIncrease = false;
				if(args.get(0).equals("primaryrace") && args.size() >= 3)
				{
					if(n.races.get(0).name.toLowerCase().equals(args.get(1).toLowerCase()))
					{
						canIncrease = true;
					}
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				
				// racetag
				canIncrease = false;
				if(args.get(0).equals("racetag") && args.size() >= 3)
				{
					if(n.races.get(0).tags.contains(args.get(1)))
						canIncrease = true;
					
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				
				// nationtag
				canIncrease = false;
				if(args.get(0).equals("nationtag") && args.size() >= 3)
				{
					
					for(Filter f2 : n.appliedfilters)
						if(f2.tags.contains(args.get(1)))
							canIncrease = true;
					
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				
				// Average gold and res
				canIncrease = false;
				if(args.get(0).equals("avggold") && args.size() >= 3)
				{
					double gold = Double.parseDouble(args.get(1));
					if(avggold >= gold)
						canIncrease = true;
					
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				
				canIncrease = false;
				if(args.get(0).equals("avgres") && args.size() >= 3)
				{
					double res = Double.parseDouble(args.get(1));
					if(avgres >= res)
						canIncrease = true;
					
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				
			}

		}
		

		
		
		// Do national affinities
		for(String tag : n.races.get(0).tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("filteraffinity") && args.size() > 3)
			{
				double multi = Double.parseDouble(args.get(args.size() - 1));
				String value = args.get(args.size() - 2);
				boolean type = false;
				
				if(args.contains("type"))
					type = true;
				else if(args.contains("name")){/*do nothing*/}
				else
					System.out.println("Invalid #filteraffinity (no type/name): " + tag);
					
				for(T f : filters.keySet())
				{
					if(type && f.types.contains(value))
						filters.put(f, f.basechance * multi);
					else if(!type && f.name.equals(value))
						filters.put(f, f.basechance * multi);
				}
			}

		}
		

	}

	
	protected boolean alreadyHasType(List<? extends Filter> filters, List<String> types)
	{
		if(types.size() == 0)
			return false;
		
		for(Filter f : filters)
			for(String type : types)
				if(f.types.contains(type) && !f.tags.contains("freetype"))
					return true;
		
		return false;
	}
	
	protected boolean alreadyHasType(List<? extends Filter> filters, String type)
	{
		if(type.equals(""))
			return false;
		
		for(Filter f : filters)
			if(f.types.contains(type) && !f.tags.contains("freetype"))
				return true;
		
		return false;
	}
	
	
	protected <T extends Filter> void handleUnitIncs(LinkedHashMap<T, Double> filters,  Unit u)
	{

		if(u == null)
			return;

		for(T f : filters.keySet())
		{	
			
			List<String> chanceincs = new ArrayList<String>();
			chanceincs.addAll(f.chanceincs);
			chanceincs.addAll(themeincs);
			
			for(String str : chanceincs)
			{
				// Poses
				List<String> args = Generic.parseArgs(str);
				if(args.get(0).equals("pose") && args.size() > 2)
				{
					if(u.pose.roles.contains(args.get(1)))
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				else if(args.get(0).equals("personalcommand") && args.size() > 2)
				{

					boolean contains = false;
					for(Command c : u.commands)
						if(c.command.equals(args.get(1)))
							contains = true;
					
					if(contains)
					{
						
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				if(args.get(0).equals("personalshape") && args.size() >= 3)
				{
					boolean contains = false;
					String shape = args.get(1);
					for(ShapeChangeUnit u2 : n.secondshapes)
					{
						if(u2.thisForm.name.equals(shape) && u2.otherForm.equals(u))
							contains = true;			
					}
					
					if(contains)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				if(args.get(0).equals("filter") && args.size() >= 3)
				{
					boolean contains = false;
					for(Filter f2 : u.appliedFilters)
					{
						if(f2.name.equals(args.get(1)))
							contains = true;
					}
					if(contains)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				else if(args.get(0).equals("unittag") && args.size() > 2)
				{

					boolean contains = Generic.containsTag(u.tags, args.get(1));					
					if(contains)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				else if(args.get(0).equals("racetag") && args.size() > 2)
				{

					boolean contains = Generic.containsTag(u.race.tags, args.get(1));					
					if(contains)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				else if(args.get(0).equals("posetag") && args.size() > 2)
				{

					boolean contains = Generic.containsTag(u.pose.tags, args.get(1));					
					if(contains)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				else if(args.get(0).equals("tag") && args.size() > 2)
				{

					boolean contains = Generic.containsTag(u.race.tags, args.get(1)) || Generic.containsTag(u.tags, args.get(1)) || Generic.containsTag(u.pose.tags, args.get(1));					
					if(contains)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				else if(args.get(0).equals("taggedmagic") && args.size() > 1)
				{
			
					List<String> tags = new ArrayList<String>();
					
					tags.addAll(u.tags);
					for(Filter fu : u.appliedFilters)
						tags.addAll(fu.tags);
					for(String slot : u.slotmap.keySet())
					{
						if(u.slotmap.get(slot) != null)
							tags.addAll(u.slotmap.get(slot).tags);
					}
					
					boolean contains = true;
					for(int i = 1; i < args.size() - 1; i++)
					{
						String path = args.get(i);
						if(!tags.contains("path " + path))
							contains = false;
					}
					
					
					if(contains)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
					
				}
				// "comparemagic earth above air 1"
				// "comparemagic earth equal air 1"
				// "comparemagic earth below air 1"
				else if(args.get(0).equals("comparemagic") && args.size() > 4)
				{
					int path1 = Generic.PathToInteger(args.get(1));
					int path2 = Generic.PathToInteger(args.get(3));
					
		
					int[] picks = u.getMagicPicks();
					
					boolean check = false;
					
					if(args.get(2).equals("below")  && args.size() <= 5)
						check = (picks[path1] < picks[path2]);
					else if(args.get(2).equals("above") && args.size() <= 5)
						check = (picks[path1] > picks[path2]);
					else if(args.get(2).equals("equal"))
						check = (picks[path1] == picks[path2]);
					else if(args.size() > 5 && args.get(2).equals("below"))
					{
						int amount = Integer.parseInt(args.get(4));
						check = (picks[path2] - picks[path1] <= amount) && picks[path2] > picks[path1];
					}
					else if(args.size() > 5 && args.get(2).equals("above"))
					{
						int amount = Integer.parseInt(args.get(4));
						check = (picks[path1] - picks[path2] <= amount) && picks[path2] < picks[path1];
					}	
					
					else
						System.out.println("Error with " + str + ". No keyword below/above/equal was found.");

					
					if(check)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
						
					
				}
				else if(args.get(0).equals("personalmagic") && args.size() > 1)
				{

					boolean contains = true;
					int level = 1;
					
					int step = 0;
					int path = -1;
					for(int i = 1; i < args.size() - 1; i++)
					{
						if(step == 0)
						{
							path = Generic.PathToInteger(args.get(i));
							
							// If there's no level specified
							if(i != args.size() - 2 && !Generic.isNumeric(args.get(i + 1)) && !args.get(i+1).equals("below"))
							{
								if(u.getMagicPicks()[path] < 1)
								{
									contains = false;
									break;
								}	
							}
							else
								step++;
							
							
							
						}
						else if(step == 1)
						{
							
							boolean not = false;
							if(args.get(i).equals("below"))
							{
								if(i != args.size() - 2)
								{
									level = Integer.parseInt(args.get(i+1));
								}
								else
								{
									System.out.println("Error in chanceinc " + str + ". Need a path level after keyword 'below'");
									level = 0;
								}
								not = true;
							}
							else
								level = Integer.parseInt(args.get(i));		

							

							if((u.getMagicPicks()[path] < level) != not)
							{
								contains = false;
								break;
							}
							step--;
						}
			
		
					}
					
					if(contains)
					{
						
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				else if(args.get(0).equals("magicbelow") && args.size() > 1)
				{

					boolean contains = true;
					int level = 1;
					
					int step = 0;
					int path = -1;
					for(int i = 1; i < args.size() - 1; i++)
					{
						if(step == 0)
						{
							path = Generic.PathToInteger(args.get(i));
							step++;
						}
						else if(step == 1)
						{
							level = Integer.parseInt(args.get(i));		
							if(u.getMagicPicks()[path] > level)
							{
								contains = false;
								break;
							}
							step--;
						}
			
		
					}
					
					if(contains)
					{
						
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				
				else if(args.get(0).equals("origname") && args.size() > 1)
				{

					boolean contains = false;
					if(u.name.type.equals(args.get(1)))
						contains = true;
					
					if(contains)
					{
						
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
						continue;
					}
				}
				else if(args.get(0).equals("race") && args.size() >= 3)
				{
					boolean canIncrease = false;
					if(u.race.name.toLowerCase().equals(args.get(1).toLowerCase()))
					{
						canIncrease = true;
					}
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				else if(args.get(0).equals("prot") && args.size() >= 3)
				{
					boolean canIncrease = false;
					
					boolean below = false;
					if(args.contains("below"))
						below = true;
					
					if(!below && u.getTotalProt(true) >= Integer.parseInt(args.get(args.size() - 2)))
					{
						canIncrease = true;
					}
					else if(below && u.getTotalProt(true) < Integer.parseInt(args.get(args.size() - 2)))
					{
						canIncrease = true;
					}
					if(canIncrease)
					{
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
					}
				}
				else if(args.get(0).equals("enc") && args.size() >= 3)
				{
					boolean canIncrease = false;
					
					boolean below = false;
					if(args.contains("below"))
						below = true;
					
					int enc = 3;
					for(Command c : u.getCommands())
					{
						if(c.command.equals("#enc"))
							enc = Integer.parseInt(c.args.get(0));
					}
					
					if(!below && enc >= Integer.parseInt(args.get(args.size() - 2)))
					{
						canIncrease = true;
					}
					else if(below && enc < Integer.parseInt(args.get(args.size() - 2)))
					{
						canIncrease = true;
					}
					if(canIncrease)
						filters.put(f, applyModifier(f.basechance, args.get(args.size() - 1)));
				}
				
				
			}
			

		}
		

		
	}
	
}
