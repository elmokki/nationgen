package nationGen.misc;


import nationGen.chances.ChanceInc;
import nationGen.chances.ChanceIncData;
import nationGen.chances.EntityChances;
import nationGen.chances.ThemeInc;
import nationGen.chances.ThemeIncData;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class ChanceIncHandler {
	
	public final Nation n;
	public String identifier = "";
	
	public ChanceIncHandler(Nation n, String identifier)
	{
		this.identifier = identifier;
		this.n = n;
	}
	
	public ChanceIncHandler(Nation n)
	{
		this.n = n;
	}
	
	private List<ThemeInc> getRaceThemeIncs(Race r)
	{
		List<ThemeInc> list = new ArrayList<>();
		
		for(Theme t : r.themefilters)
			list.addAll(t.themeincs);
		
		return list;
	}
	
	public <T extends Filter> List<T> removeRelated(T thing, List<T> list)
	{
		
		List<T> shit = new ArrayList<>();
		
		list.remove(thing);
		for(String type : thing.types)
			for(T t : list)
				if(t.types.contains(type))
					shit.add(t);
		
		list.removeAll(shit);
		
		return list;
	}
	
	
	
	
	public <T extends Filter> EntityChances<T> handleChanceIncs(List<T> filters)
	{
		return handleChanceIncs(null,null, filters, null);
	}
	
	
	public <T extends Filter> EntityChances<T> handleChanceIncs(Unit u, List<T> filters)
	{
		return handleChanceIncs(u, null, filters, null);
	}
	
	public EntityChances<Pose> handleChanceIncs(Race r, List<Pose> filters)
	{
		return handleChanceIncs(null, r, filters, null);
	}
	
	public <T extends Filter> EntityChances<T> handleChanceIncs(Unit u, List<T> filters, List<ThemeInc> extraincs) {
		return handleChanceIncs(u, null, filters, extraincs);
	}
	
	public <T extends Filter> EntityChances<T> handleChanceIncs(Unit u, Race r, List<T> filters, List<ThemeInc> extraincs)
	{
		return handleChanceIncs(u, r, EntityChances.baseChances(filters), extraincs);
	}

	

	
	
	/**
	 * The main method for handling chanceincs. This overrides basechances.
	 * @param u
	 * @param chances
	 * @return
	 */
	public <T extends Filter> EntityChances<T> handleChanceIncs(Unit u, EntityChances<T> chances)
	{

		return handleChanceIncs(u, null, chances, null);
	}
	
	/**
	 * The main method for handling chanceincs. This overrides basechances.
	 * @param u
	 * @param chances
	 * @param extraincs
	 * @return
	 */
	public <T extends Filter> EntityChances<T> handleChanceIncs(Unit u, Race race, EntityChances<T> chances, List<ThemeInc> extraincs)
	{
		if (race == null) {
			if (u != null && u.race != null)
				race = u.race;
			else if (n.races.size() > 0 && n.races.get(0) != null)
				race = n.races.get(0);
		}
		
		

		List<ThemeInc> miscincs = new ArrayList<>();
		
		if(extraincs != null)
			miscincs.addAll(extraincs);
		
	
		if(u != null)
		{
			for(Filter f : u.appliedFilters)
				miscincs.addAll(f.themeincs);
		}

		
		// Actually handle the stuff
		processChanceIncs(chances, u, race);
		
		handleThemeIncs(chances, miscincs, race);

		return chances;
	}
	
	public static boolean suitableFor(Unit u, Filter f, Nation n)
	{
		for(String role : u.pose.roles)
		{
			if (f.tags.getAllStrings("nopose").stream().anyMatch(role::contains)) {
				return false;
			}
		}
		
		if(n != null && n.races.size() > 0)
		{
			Race primaryRace = n.races.get(0);
			return (!f.tags.contains("norace", "primary") || u.race != primaryRace)
				&& (!f.tags.contains("norace", "secondary") || u.race == primaryRace);
		}
		return true;
	}
	
	public static boolean canAdd(Unit u, Filter f)
	{
		if(!suitableFor(u, f, null))
		{
			return false;
		}
		
		List<String> primaries = f.tags.getAllStrings("primarycommand");
		
		
		boolean shapeshift = false;
		for(Command c : f.getCommands())
			if(c.command.equals("#shapechange") || c.command.equals("#secondshape") || c.command.equals("#secondtmpshape"))
			{
				shapeshift = true;
				break;
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
	
			if(shapeshift && (c.command.equals("#shapechange") || c.command.equals("#secondshape") || c.command.equals("#secondtmpshape")))
			{
				ok = false;
				break;
			}
			else if(shapeshift)

			
			for(Command fc : f.getCommands())
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
		

		
		if(f.getCommands().size() == 0 && !primarycommandfail && ok)
		{
			Optional<Integer> threshold = f.tags.getInt("lowenctreshold");
			if (threshold.isPresent()) {
				int enc = 0;
				if (u.getSlot("armor") != null)
					enc += u.nationGen.armordb.GetInteger(u.getSlot("armor").id, "enc");
				if (u.getSlot("offhand") != null && u.getSlot("offhand").armor)
					enc += u.nationGen.armordb.GetInteger(u.getSlot("offhand").id, "enc");
				if (u.getSlot("helmet") != null)
					enc += u.nationGen.armordb.GetInteger(u.getSlot("helmet").id, "enc");
				
				
				ok = (enc <= threshold.get());
			}
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
		List<Filter> newList = new ArrayList<>();
		for(Filter f : orig)
		{
			if(f.power <= max && f.power >= min)
				newList.add(f);
		}
		return newList;
	}
	
	
	public static <E extends Filter> List<E> getFiltersWithType(String type, List<E> orig)
	{
		List<E> newList = new ArrayList<>();
		for(E f : orig)
		{
			if(f.types.contains(type))
			{
				newList.add(f);
			}
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
		List<Filter> list = new ArrayList<>();
		

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
	
	public static List<Filter> getValidUnitFilters(List<Filter> filters, Unit unit)
	{
		List<Unit> l = new ArrayList<>();
		l.add(unit);


		return getValidUnitFilters(filters, l);
	}
	
	/**
	 * Checks ChanceIncHandler.canAdd() for all filters and removes bad ones.
	 * @param filters
	 * @param oldfilters
	 * @return
	 */
	public static <E extends Filter> List<E> getValidFilters(List<E> filters, List<E> oldfilters)
	{
		List<E> list = new ArrayList<>();
		
		for(E f : filters)
		{
			if(ChanceIncHandler.canAdd(oldfilters, f))
				list.add(f);
	
		}
		
		return list;
	}
	
	
	private static <E extends Filter> void removeRemovableFilters(List<E> filters)
	{
		List<E> remov = new ArrayList<>();
		for(E e : filters)
			if(!e.tags.containsName("unremovable"))
				remov.add(e);
		
		filters.removeAll(remov);
	}
	
	
	private static <E extends Filter> List<E> retrieveFiltersFromTags(Tags tags, String lookfor, ResourceStorage<E> source)
	{
		List<E> filters = new ArrayList<>();
		
		tags.getAllStrings(lookfor).forEach(setname -> {
			if (setname.equals("clear"))
				removeRemovableFilters(filters);
			else
				filters.addAll(Optional.ofNullable(source.get(setname)).orElseThrow(
					() -> new IllegalArgumentException("#" + lookfor + " " + setname + " could not find the set " + setname)));
		});
		
		return filters;
	}
	
	
	public static <E extends Filter> List<E> retrieveFilters(String lookfor, String defaultset, ResourceStorage<E> source, Pose p, Race r)
	{
		String[] derp = {defaultset};
		return retrieveFilters(lookfor, derp, source, p, r);
	}
	
	public static <E extends Filter> List<E> retrieveFilters(String lookfor, String[] defaultset, ResourceStorage<E> source, Pose p, Race r)
	{
		List<E> filters = new ArrayList<E>();
		
		
		if(r != null)
			filters.addAll(retrieveFiltersFromTags(r.tags, lookfor, source));
		
		if(p != null)
			filters.addAll(retrieveFiltersFromTags(p.tags, lookfor, source));

	

		
		
		if(filters.size() == 0)
		{
			for(String str : defaultset)
			{
				if(source.get(str) != null)
					filters.addAll(source.get(str));
				else
					System.out.println("Default set " + str + " for " + lookfor + " was not found from " + source);
			}

		}

		
		return filters;

	}
	
	private Set<MagicPath> pathsAtHighest(MagicPathInts unitpaths)
	{
		NavigableMap<Integer, Set<MagicPath>> pathLevels = unitpaths.byAmount();
		int highest = pathLevels.lastKey();
		int secondHighest = Optional.ofNullable(pathLevels.lowerKey(highest)).orElse(0);
		Set<MagicPath> atHighest = highest == 0 ? Collections.emptySet() : pathLevels.get(highest);
		Set<MagicPath> atSecondHighest = secondHighest == 0 ? Collections.emptySet() : pathLevels.get(secondHighest);

		if(highest - secondHighest == 1 && atHighest.size() == 1 && atHighest.size() + atSecondHighest.size() < 4)
		{
			atHighest.addAll(atSecondHighest);
		}
		
		return atHighest;
	}
	
	/**
	 * This is a separate method for chanceincs that target the filter/item/whatever itself - ie stuff that is relevant 
	 * for themes only since in general targetting the thing itself is pointless.
	 * 
	 * All theme chanceincs SHOULD NOT be implemented here. If the chanceinc does not target the filter itself,
	 * for example not something like "higher chance if item contains theme 'advanced'", it is usable in other
	 * places as well.
	 * 
	 * @param chances
	 * @param miscincs
	 * @param r
	 */
	private <T extends Filter> void handleThemeIncs(EntityChances<T> chances, List<ThemeInc> miscincs, Race r)
	{
		List<ThemeInc> chanceincs = new ArrayList<>();
		
		// Add race themes if appliceable!
		if(r != null)
			chanceincs.addAll(getRaceThemeIncs(r)); // Should never be null.

		// Add all nation theme themeincs!
		for(Theme t : n.nationthemes)
			chanceincs.addAll(t.themeincs);
		
		chanceincs.addAll(miscincs);
		
		for(T f : chances.getEntities())
		{
			ThemeIncData data = new ThemeIncData(this.n.nationGen, f);

			for(ThemeInc themeInc : chanceincs)
			{
				if(themeInc.condition.test(data))
				{
					chances.modifyChance(f, themeInc.modificationAmount);
				}
			}
		}
	}

	public <T extends Filter> List<T> getPossibleFilters(List<T> list, Unit u)
	{
		return handleChanceIncs(u, list).getPossible();
	}
	
	public <T extends Filter> int countPossibleFilters(List<T> list, Unit u)
	{
	
		return handleChanceIncs(u, list).countPossible();
	}
	
	public <T extends Filter> T getRandom(List<T> list, List<Unit> units)
	{
		return handleChanceIncs(units.get(0), list).getRandom(n.random);
	}
	
	public <T extends Filter> T getRandom(List<T> list, Unit u)
	{
		return handleChanceIncs(u, list).getRandom(n.random);
	}
	
	public Pose getRandom(List<Pose> list, Race race)
	{
		return handleChanceIncs(race, list).getRandom(n.random);
	}
	
	public <T extends Filter> T getRandom(List<T> list)
	{
		return this.handleChanceIncs(list).getRandom(n.random);
	}

	private <T extends Filter> void processChanceIncs(EntityChances<T> chances, Unit un, Race race)
	{
		
		ChanceIncData data = createChanceIncData(un, race);
		
		// Do chanceincs!
		for(T f : chances.getEntities())
		{
			for(ChanceInc chanceInc : f.chanceincs)
			{
				try {
					if (chanceInc.condition.test(data)) {
						chances.modifyChance(f, chanceInc.modificationAmount);
					}
				} catch (Exception e) {
					throw new IllegalStateException(String.format("Chanceinc failed for %s filter \"%s\": [%s]",
							f.getClass().getSimpleName(), f, chanceInc.source), e);
				}
			}
		}
		

		// Do national affinities
		if(n.races.size() > 0)
			for(Args args : n.races.get(0).tags.getAllArgs("filteraffinity"))
			{
				ArgParser parser = args.parse();
				
				String mode = parser.nextString();
				
				if (!mode.equals("type") && !mode.equals("name"))
					throw new IllegalArgumentException("Invalid #filteraffinity (no type/name): " + args);
				
				boolean type = mode.equals("type");
				
				String value = parser.nextString();
				Arg modifier = parser.next("modifier");
				
				for(T f : chances.getEntities())
				{
					if (type ? f.types.contains(value) : f.name.equals(value)) {
						chances.modifyChance(f, modifier);
					}
				}
			}
		
	}
	
	private ChanceIncData createChanceIncData(Unit un, Race race) {
		
		ChanceIncData data = new ChanceIncData();
		data.moduleId = this.identifier;
		data.n = this.n;
		data.u = un;
		data.race = race;
		
		data.nonrandom_paths = new MagicPathInts();
		n.selectCommanders("mage")
				.filter(u -> u.tags.containsName("schoolmage"))
				.forEach(u -> data.nonrandom_paths.maxWith(u.getMagicPicks(false)));
		
		MagicPathInts paths = new MagicPathInts();
		n.selectCommanders("mage")
				.filter(u -> u.tags.containsName("schoolmage"))
				.forEach(u -> paths.maxWith(u.getMagicPicks(true)));
		
		data.atHighest = this.pathsAtHighest(paths);
		
		data.diversity = 0;
		data.at = new int[10];
		for(MagicPath path : MagicPath.values())
		{
			if(paths.get(path) > 1
					|| (path == MagicPath.ASTRAL && paths.get(path) == 1)
					|| (path == MagicPath.BLOOD && paths.get(path) == 1)) {
				
				data.diversity++;
			}
			
			data.at[path.number]++;
		}
		
		final List<Unit> troopList = n.selectTroops().collect(Collectors.toList());

		// Avg resources and gold
		int unitCount = troopList.size();
		double totalgold = 0;
		double totalres = 0;

		for(Unit u : troopList)
		{
			if(u.pose.roles.contains("mounted") || u.pose.roles.contains("chariot"))
				totalgold += u.getGoldCost()  * 0.66;
			else
				totalgold += u.getGoldCost();

			totalres += u.getResCost(true);
		}

		data.avgres = totalres / unitCount;
		data.avggold = totalgold / unitCount;
		
		return data;
	}
}
