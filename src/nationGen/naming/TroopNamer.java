package nationGen.naming;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;

import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;


public class TroopNamer {

	private Nation n;

	List<String> given = new ArrayList<String>();
	private Random r;
	
	private List<NamePart> basenames;
	private List<NamePart> weaponnames;
	private List<NamePart> weaponprefixes;
	private List<NamePart> mountprefixes;
	private List<NamePart> specialprefixes;
	private List<NamePart> specialnames;
	private List<NamePart> miscitemprefixes;
	private List<NamePart> miscitemnames;
	private List<NamePart> miscguaranteed;
	private List<NamePart> combases;
	
	private ChanceIncHandler chandler;
	
	public TroopNamer()
	{


	}
	
	public void execute(Nation n) {
		this.n = n;
		r = new Random(n.random.nextInt());
		chandler = new ChanceIncHandler(n);
		
		basenames = ChanceIncHandler.retrieveFilters("troopbasenames", "troopbasenames", n.nationGen.miscnames, null, n.races.get(0));
		weaponnames = ChanceIncHandler.retrieveFilters("troopweaponnames", "troopweaponnames", n.nationGen.miscnames, null, n.races.get(0));
		weaponprefixes = ChanceIncHandler.retrieveFilters("troopweaponprefixes", "troopweaponprefixes", n.nationGen.miscnames, null, n.races.get(0));
		mountprefixes = ChanceIncHandler.retrieveFilters("troopmountprefixes", "troopmountprefixes", n.nationGen.miscnames, null, n.races.get(0));
		specialprefixes = ChanceIncHandler.retrieveFilters("troopspecialprefixes", "troopspecialprefixes", n.nationGen.miscnames, null, n.races.get(0));
		specialnames = ChanceIncHandler.retrieveFilters("troopspecialnames", "troopspecialnames", n.nationGen.miscnames, null, n.races.get(0));
		miscitemnames = ChanceIncHandler.retrieveFilters("troopmiscitemnames", "troopmiscitemnames", n.nationGen.miscnames, null, n.races.get(0));
		miscitemprefixes = ChanceIncHandler.retrieveFilters("troopmiscitemprefixes", "troopmiscitemprefixes", n.nationGen.miscnames, null, n.races.get(0));
		miscguaranteed = ChanceIncHandler.retrieveFilters("troopmiscguaranteedparts", "troopmiscguaranteedparts", n.nationGen.miscnames, null, n.races.get(0));
		combases = ChanceIncHandler.retrieveFilters("commanderbasenames", "commanderbasenames", n.nationGen.miscnames, null, n.races.get(0));

		
		nameNation();
	}
	
	

	private void nameNation()
	{
		
		// Get base names ("warrior", "cavalry", "soldier" etc)
		setBaseNames();
		
		// Forces every unit to have a weapon/mount (mount prioritized) based name
		setWeaponMountNames();
		
	
		// Add all troops to one list
		List<Unit> alltroops = n.combineTroopsToList("infantry");
		alltroops.addAll(n.combineTroopsToList("chariot"));
		alltroops.addAll(n.combineTroopsToList("mounted"));
		alltroops.addAll(n.combineTroopsToList("ranged"));
		alltroops.addAll(n.combineTroopsToList("montagtroops"));

	
		// #forcedname
		List<Unit> forcednames = new ArrayList<Unit>();
		for(Unit u : alltroops)
			if(Generic.containsTag(Generic.getAllUnitTags(u), "forcedname"))
				forcednames.add(u);
		alltroops.removeAll(forcednames);
		for(Unit u : forcednames)
		{
			u.name.setType(Generic.getTagValue(Generic.getAllUnitTags(u), "forcedname"));
		}
		
		// Give special parts
		setGuaranteedParts(alltroops, specialnames);
		setGuaranteedParts(alltroops, specialprefixes);
		
		// Give misc guaranteed parts
		setGuaranteedParts(alltroops, miscguaranteed);
		
		// Differentiate units based on misc items
		List<NamePart> parts = new ArrayList<NamePart>();
		parts.addAll(miscitemnames);
		parts.addAll(miscitemprefixes);
		differentiateNames(alltroops, parts);
		

		// If we have overwritten a prefix we may want to differentiate infantry with weapon names
		differentiateNames(n.combineTroopsToList("infantry"), weaponnames);
		differentiateNames(n.combineTroopsToList("montagtroops"), weaponnames);
		

		// Remove as much as we can to keep things simple
		removeRedundantParts(n.combineTroopsToList("infantry"));
		removeRedundantParts(n.combineTroopsToList("mounted"));
		removeRedundantParts(n.combineTroopsToList("ranged"));
		removeRedundantParts(n.combineTroopsToList("montagtroops"));

	

		
		// National prefix/suffix
		addNationalPrefix(alltroops);
		
		// Give light/heavy prefixes and nationality prefix
		addHeavyLightPrefix(n.combineTroopsToList("infantry"));
		addHeavyLightPrefix(n.combineTroopsToList("mounted"));
		addHeavyLightPrefix(n.combineTroopsToList("ranged"));
		addHeavyLightPrefix(n.combineTroopsToList("montagtroops"));
		
		// Nation name ("Ulmish" etc)
		addNationPrefix(alltroops);
				

		
		nameCommanders(n);
		
	}
	
	

	private void addNationPrefix(List<Unit> units)
	{
		for(Unit unit : units)
		{
			unit.name.setRankPrefix(Generic.capitalize(n.name) + n.nationalitysuffix);
		}
	}
	

	private void addHeavyLightPrefix(List<Unit> units)
	{
		for(Unit named : units)
		{
			for(Unit unit : units)
			{
				if(named.getName().equals(unit.getName()) && unit.getTotalProt() != named.getTotalProt())
				{
					if(unit.getTotalProt() < named.getTotalProt() && named.isHeavy())
					{
						String part = "Heavy";
						if(named.name.prefixprefix == null)
							named.name.setPrefixprefix(part);
						else
							named.name.setPrefixprefixprefix(part);
						break;
					}
					else if(unit.getTotalProt() > named.getTotalProt() && named.isLight())
					{
						String part = "Light";
						
						if(named.name.prefixprefix == null)
							named.name.setPrefixprefix(part);
						else
							named.name.setPrefixprefixprefix(part);
						
						break;
					}

				}
			}
		}
	}
	
	private void addNationalPrefix(List<Unit> units)
	{
		
		for(Unit u : units)
		{
			String part = "";
			
			// Horribly unclear conditional parses out as follows:
			// 		Does the unit have a #subraceprefix? 
			//			If so, do the primary race + themes have a (different) #raceprefix, or if there is no #raceprefix, is the visible name different from #subraceprefix?
			//		Else, is the unit's visible name different than the primary race's visible name?
			
			if(Generic.containsTag(Generic.getAllUnitTags(u), "subraceprefix") && 
						((!Generic.containsTag(Generic.getAllNationTags(n), "raceprefix") && !Generic.getTagValue(Generic.getAllUnitTags(u), "subraceprefix").equals(n.races.get(0).visiblename))
								|| !Generic.getTagValue(Generic.getAllUnitTags(u), "subraceprefix").equals(Generic.getTagValue(Generic.getAllNationTags(n), "raceprefix"))))
			{
				part = Generic.getTagValue(Generic.getAllUnitTags(u), "subraceprefix");
				u.name.setPrefixprefix(part);
			}
			else if(!u.race.visiblename.equals(n.races.get(0).visiblename))
			{
				part = u.race.visiblename;
				u.name.setPrefixprefix(part);
			}
		}
			
	}
	
	private void nameCommanders(Nation n)
	{
		List<Unit> units = new ArrayList<Unit>();
		units.addAll(n.comlists.get("commanders"));
		List<NamePart> used = new ArrayList<NamePart>();
		
		for(Unit u : units)
		{
			int tier = 0;
			for(Command c : u.commands)
			{
				if(c.command.startsWith("#okleader"))
					tier++;
				else if(c.command.startsWith("#inspirational"))
					tier++;
				if(c.command.startsWith("#goodleader"))
					tier++;
				if(c.command.startsWith("#expertleader"))
					tier++;
				
	
		
			}
			tier += r.nextInt(3) - 1; // -1 to 1
			
			tier = Math.max(Math.min(3, tier), 1);
			
			
			List<NamePart> all = getSuitableParts(this.combases, false, true);
			all.removeAll(used);
			
			List<NamePart> suitables = new ArrayList<NamePart>();
			for(NamePart p : all)
			{
				if(Generic.getTagValues(p.tags, "tier").contains("" + tier))
					suitables.add(p);
			}
			if(suitables.size() == 0)
				for(NamePart p : all)
				{
					if(Generic.getTagValues(p.tags, "tier").size() == 0)
						suitables.add(p);
				}
			
			
			List<Unit> only = new ArrayList<Unit>();
			only.add(u);
			setGuaranteedParts(only, weaponnames);
			setGuaranteedParts(only, specialnames);	

			List<NamePart> possibles = new ArrayList<NamePart>();
			if(u.name.type.toString().equals("UNNAMED"))
			{
				for(NamePart p : all)
					if(!p.tags.contains("prefix"))
						possibles.add(p);
			}
			else
				possibles.addAll(all);
			
			
			NamePart p = getSuitablePart(possibles, null, u);
			used.add(p);
			this.setNamePart(u, p);
			
	
			
			
		
			setGuaranteedParts(only, miscguaranteed);
			
			
			
			if(p.tags.contains("generic") && !p.tags.contains("prefix"))
			{
				setGuaranteedParts(only, weaponprefixes);
				setGuaranteedParts(only, specialprefixes);
			}

			
		}
		



			
		// Scouts
		units = n.comlists.get("scouts");
		for(Unit u : units)
		{
			int level = 0;
			for(Command c : u.commands)
			{
				if(c.command.startsWith("#spy"))
					level = 1;
				if(c.command.startsWith("#assassin"))
					level = 2;
				
				if(level == 1)
					u.name.setType("Spy");
				else if(level == 2)
					u.name.setType("Assassin");
				else
					u.name.setType("Scout");


			}
		}
	}
	
	private List<NamePart> getSuitableParts(List<NamePart> all, boolean elite, boolean commander)
	{
		List<NamePart> selected = new ArrayList<NamePart>();
		for(NamePart p : all)
		{
			
			if(p.themes.contains("elite") && elite)
				selected.add(p);
			if(p.themes.contains("commander") && commander && !selected.contains(p))
				selected.add(p);
			
			if(!p.themes.contains("elite") && !p.themes.contains("commander") && !elite && !commander && !selected.contains(p))
				selected.add(p);
			
		}	
		
		return selected;
	}
	
	private NamePart getSuitablePart(List<NamePart> all, List<NamePart> used, Unit u)
	{
		if(used == null)
			used = new ArrayList<NamePart>();
		
		List<NamePart> newall = new ArrayList<NamePart>();
		newall.addAll(all);
		
		NamePart part = null;
		List<NamePart> unitname = u.name.getAsNamePartList();
		List<NamePart> pointless = new ArrayList<NamePart>();
		for(NamePart p : all)
		{
			boolean ok = true;

			boolean prefix = p.tags.contains("prefix");
			boolean prefixprefix = p.tags.contains("prefixprefix");
			for(NamePart op : unitname)
			{
				if(op == null)
					continue;
				
				if(op == p)
					ok = false;
				

				
				if((prefix && !op.tags.contains("prefix")) || (prefixprefix && !op.tags.contains("prefixprefix")))
				{

					for(String t : op.types)
					{
						if(p.types.contains(t))
						{
							ok = false;
							break;
						}
					}
				}
			}
			
			if(!ok)
			{
				pointless.add(p);
			}
		}
		
		used.removeAll(pointless);
		newall.removeAll(pointless);
		
		
		// Use old part if possible
		if(chandler.countPossibleFilters(used, u) > 0)
		{
			part = chandler.getRandom(used, u);
		}
		else
		{
			part = chandler.getRandom(newall, u);
			if(part != null)
				used.add(part);
		}
		
		return part;
	}
	
	private void setWeaponMountNames()
	{
		List<String> roles = new ArrayList<String>();
		roles.add("infantry");
		roles.add("mounted");
		roles.add("ranged");
		roles.add("chariot");
		
		List<NamePart> used = new ArrayList<NamePart>();
		for(String role : roles)
		{
			List<Unit> units = n.generateUnitList(role);
			for(Unit u : n.generateUnitList("montagtroops"))
				if(u.guessRole().equals(role))
					units.add(u);
			
			if(units.size() == 0)
				continue;
			
			
			for(Unit u : units)
			{
				
				List<NamePart> parts;
				// Mount stuff
				if(u.getSlot("mount") != null)
				{
					parts = getSuitableParts(mountprefixes, false, false);
				}
				// Other stuff
				else
				{
					List<NamePart> temp = new ArrayList<NamePart>();
					temp.addAll(this.weaponnames);
					temp.addAll(this.weaponprefixes);
					parts = getSuitableParts(temp, false, false);
				}
				
				List<NamePart> usedhere = new ArrayList<NamePart>();
				usedhere.addAll(used);
				usedhere.retainAll(parts);
				
				NamePart part = this.getSuitablePart(parts, usedhere, u);
				
				usedhere.removeAll(used);
				used.addAll(usedhere);
				
				if(part != null)
				{
					setNamePart(u, part);

				}
				
				
				
			}
			
			
	
			
		}
	}
	
	private void setNamePart(Unit u, NamePart part)
	{
		if(part.tags.contains("prefix"))
			u.name.prefix = part;
		else if(part.tags.contains("prefixprefix"))
			u.name.prefixprefix = part;
		else
			u.name.type = part;
	}
	
	
	
	private void setBaseNames()
	{

		List<String> roles = new ArrayList<String>();
		roles.add("infantry");
		roles.add("mounted");
		roles.add("ranged");
		roles.add("chariot");
		
		
		for(String role : roles)
		{
			List<Unit> units = n.generateUnitList(role);
			for(Unit u : n.generateUnitList("montagtroops"))
				if(u.guessRole().equals(role))
					units.add(u);
			
			
			if(units.size() == 0)
				continue;
			
			NamePart part = chandler.getRandom(getSuitableParts(basenames, false, false), units);
			
			if(part != null)
			{
				for(Unit u : units)
				{
					setNamePart(u, part);
				}
			}
			else
			{
				System.out.println("Unable to find a base name for " + role + " of a nation of race " + n.races.get(0));
			}
			
		}
		

	}
	
	private List<NamePart> getPointlessParts(List<NamePart> possibleParts, Unit u)
	{
		List<NamePart> pointlessParts = new ArrayList<NamePart>();
		
		// Remove parts of already existing types
		for(NamePart p : possibleParts)
			for(String t : p.types)
				for(NamePart p2 : u.name.getAsNamePartList())
					if(p2 != null && p2.types.contains(t))
						pointlessParts.add(p);
		
		return pointlessParts;
		
	}
	private void differentiateNames(List<Unit> units, List<NamePart> parts)
	{
		
		for(Unit u : units)
		{		
			
			// Get all units with the same name
			List<Unit> matches = new ArrayList<Unit>();
			for(Unit u2 : units)
			{
				
				if(u.getName().equals(u2.getName()))
				{
					matches.add(u);
				}
			}
			

			// If we found more than one, we should try changing the names
			if(matches.size() > 1)
			{
				List<NamePart> possibleParts = this.getSuitableParts(parts, false, false);
				possibleParts = chandler.getPossibleFilters(possibleParts, u);
					
				
				// Remove all parts that don't help differentiating at all
				List<NamePart> pointlessParts = new ArrayList<NamePart>();
				
				// Remove parts of already existing types
				for(NamePart p : possibleParts)
					for(String t : p.types)
						for(NamePart p2 : u.name.getAsNamePartList())
							if(p2 != null && p2.types.contains(t))
								pointlessParts.add(p);
				
				
				
				for(NamePart p : possibleParts)
				{
					
					int samematches = 1;
					int sames = 1;
					for(Unit u2 : units)
					{
						if(u2 == u)
							continue;
					
						List<NamePart> possibleParts2 = this.getSuitableParts(parts, false, false);
						possibleParts2 = chandler.getPossibleFilters(possibleParts2, u2);
						
						if(possibleParts2.contains(p))
						{
							sames++;
							if(matches.contains(u2))
								samematches++;
						}
					}
					
					if(samematches == matches.size() || sames > matches.size() + 2)
						pointlessParts.add(p);
				
	
				}
					
				possibleParts.removeAll(pointlessParts);
				possibleParts.removeAll(u.name.getAsNamePartList());
				
				
				// If we have possible parts, set one.
				if(possibleParts.size() > 0)
				{
					NamePart p = chandler.getRandom(possibleParts, u);
					

					for(Unit u2 : units)
					{
						List<NamePart> possibleParts2 = this.getSuitableParts(parts, false, false);
						possibleParts2 = chandler.getPossibleFilters(possibleParts2, u2);
						possibleParts2.removeAll(getPointlessParts(possibleParts2, u2));
				
						
						if(possibleParts2.contains(p))
						{
							setNamePart(u2, p);
						}
					}
					
					
				}
					
				
			}
			
		}
	}
	
	private void removeRedundantParts(List<Unit> units)
	{
		
		// Prefix prefix
		for(Unit u : units)
		{

			if(u.name.prefixprefix == null)
				continue;
			
			Name ntemp = u.name.getCopy();
			ntemp.prefixprefix = null;
			boolean canremove = true;
			
			for(Unit u2 : units)
			{
				Name ntemp2 = u2.name.getCopy();
				ntemp2.prefixprefix = null;
			
				if(ntemp2.toString().equals(ntemp.toString()) && u != u2)
				{
					canremove = false;
				}
			}
			if(canremove && !Generic.containsTag(u.name.prefixprefix.tags, "neverredundant"));
			{
				u.name.prefixprefix = null;
			}
			
			
		}
		
		
		// Prefix
		for(Unit u : units)
		{

			if(u.name.prefix == null)
				continue;
			
			Name ntemp = u.name.getCopy();
			ntemp.prefix = null;
			boolean canremove = true;
			
			for(Unit u2 : units)
			{
				Name ntemp2 = u2.name.getCopy();
				ntemp2.prefix = null;
				
				if(ntemp2.toString().equals(ntemp.toString())  && u != u2)
				{
					canremove = false;
				}
			}
			if(canremove && !Generic.containsTag(u.name.prefix.tags, "neverredundant"))
			{
				u.name.prefix = null;
			}
			
		}



		
	}
	
	private void setGuaranteedParts(List<Unit> units, List<NamePart> all)
	{
		
		List<NamePart> used = new ArrayList<NamePart>();
		for(Unit u : units)
		{
			List<NamePart> parts = new ArrayList<NamePart>();
			
			boolean elite = Generic.getAllUnitTags(u).contains("allowelitenaming");
			
			parts = getSuitableParts(all, elite, false);
		
			
			List<NamePart> usedhere = new ArrayList<NamePart>();
			usedhere.addAll(used);
			usedhere.retainAll(parts);
			
			NamePart part = this.getSuitablePart(parts, usedhere, u);

			usedhere.removeAll(used);
			used.addAll(usedhere);
			
			if(part != null)
			{
				setNamePart(u, part);
				
			}
			
			
			
		}
		
	}
	






}
