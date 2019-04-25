package nationGen.naming;


import com.elmokki.Dom3DB;
import com.elmokki.Generic;
import nationGen.entities.Filter;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.items.Item;
import nationGen.magic.MagicPath;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;
import nationGen.misc.Site;
import nationGen.nation.Nation;
import nationGen.nation.PDSelector;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class NationAdvancedSummarizer {
	
	
	private Dom3DB weapondb;
	private Dom3DB armordb;
	
	public NationAdvancedSummarizer(Dom3DB armor, Dom3DB weapon)
	{
		this.armordb = armor;
		this.weapondb = weapon;
	}
	

	private List<String> printPDInfo(Nation n)
	{
		PDSelector pds = new PDSelector(n, n.nationGen);
		
		List<String> lines = new ArrayList<>();
		lines.add("Province defence:");
        lines.add("* Commander 1: " + pds.getPDCommander(1).name);
		lines.add("* Commander 2: " + pds.getPDCommander(2).name);
		
		lines.add("* Troop 1a: " + pds.getMilitia(1, 1).name + " - "  + pds.getStartArmyAmount(pds.getMilitia(1, 1)) + " per 10 PD");
		lines.add("* Troop 1b: " + pds.getMilitia(2, 1).name + " - " + pds.getStartArmyAmount(pds.getMilitia(2, 1)) + " per 10 PD");
		if(n.PDRanks > 2)
		{
			lines.add("* Troop 1c: " + pds.getMilitia(3, 1).name + " - " + pds.getStartArmyAmount(pds.getMilitia(3, 1)) + " per 10 PD");
			if(n.PDRanks > 3)
			{
				lines.add("* Troop 1d: " + pds.getMilitia(4, 1).name + " - " + pds.getStartArmyAmount(pds.getMilitia(4, 1)) + " per 10 PD");
			}
		}
		
		lines.add("* Troop 2a: " + pds.getMilitia(1, 2).name + " - " + pds.getStartArmyAmount(pds.getMilitia(1, 2)) + " per 10 PD");
		lines.add("* Troop 2b: " + pds.getMilitia(2, 2).name + " - " + pds.getStartArmyAmount(pds.getMilitia(2, 2)) + " per 10 PD");
		
		return lines;
	}
	
	
	private List<String> printUnits(String role, String tag, Nation n)
	{
		List<String> lines = new ArrayList<>();
		if(n.generateUnitList(role).size() > 0)
		{
			lines.add("- " + tag + ":");
			for(Unit u : n.generateUnitList(role))
				lines.addAll(getTroopInfo(u));
		}
		return lines;
	}
	

	
	private List<String> writeDescription(Nation n)
	{
		return List.of("Nation " + n.nationid + ": " + n.name,
			"-----------------------------------",
			n.summary.race,
			n.summary.military,
			n.summary.magic,
			n.summary.priest);
	}
			
	public void writeDescriptionFile(List<Nation> nations, String modDirectory)
	{
		FileUtil.writeLines("/mods/" + modDirectory + "/descriptions.txt", writeDescriptionLines(nations));
	}
	
	List<String> writeDescriptionLines(List<Nation> nations)
	{
		System.out.print("Writing descriptions");
		
		List<String> lines = new ArrayList<>();
		for(Nation n : nations)
		{
			lines.addAll(writeDescription(n));
			lines.add("");
			System.out.print(".");
		}
		System.out.println(" Done!");
		
		return lines;
	}
	
	public void writeAdvancedDescriptionFile(List<Nation> nations, String modDirectory)
	{
		FileUtil.writeLines("/mods/" + modDirectory + "/advanceddescriptions.txt", writeAdvancedDescriptionLines(nations));
	}
	
	List<String> writeAdvancedDescriptionLines(List<Nation> nations)
	{
		System.out.print("Writing advanced descriptions");
		
		List<String> lines = new ArrayList<>();
		for(Nation n : nations)
		{
			lines.addAll(writeDescription(n));
			lines.add("-----------------------------------");


			List<String> traits = new ArrayList<>();
			for(Command c : n.getCommands())
				if(c.command.equals("#idealcold"))
				{
					traits.add("Ideal cold level " + c.args.get(0));
				}
				else if(c.command.equals("#uwbuild") && c.args.get(0).equals("1"))
				{
					traits.add("Can build forts underwater");
				}
				else if(c.command.equals("#buildfort") && c.args.get(0).equals("11"))
				{
					traits.add("Fortified cities");
				}
				else if(c.command.equals("#buildfort") && c.args.get(0).equals("15"))
				{
					traits.add("Giant forts");
				}
				else if(c.command.equals("#buildfort") && c.args.get(0).equals("20"))
				{
					traits.add("Ice forts");
				}
				else if(c.command.equals("#buildfort") && c.args.get(0).equals("27"))
				{
					traits.add("Fortified villages");
				}
			
			
			for(Theme f : n.nationthemes)
			{
				traits.add(f.tags.getString("desc").orElse(f.name));
			}

			if(traits.size() > 0)
			{
				lines.add("National traits:");
				for(String trait : traits)
					lines.add("- " + trait);
				lines.add("");
			}
			
			// Write themes
			for(Race r : n.races)
			{
				lines.add(r.name + " themes: " + r.themefilters.stream()
						.map(f -> f.tags.getString("desc").orElse(f.name))
						.collect(Collectors.joining(", ")));
			}
			lines.add("");
			
			// Site features
			List<String> sitef = new ArrayList<>();
			for(Site s : n.sites)
				for(Filter f : s.appliedfilters)
					sitef.add(f.toString());
			
			if(sitef.size() > 0)
			{
				lines.add("Magic site features: " + Generic.listToString(sitef, ","));
				lines.add("");
			}
				
			
			
			// Units
			lines.add("Troops:");
			
			lines.addAll(printUnits("ranged", "Ranged", n));
			lines.addAll(printUnits("infantry", "Infantry", n));
			lines.addAll(printUnits("mounted", "Cavalry", n));
			lines.addAll(printUnits("chariot", "Chariots", n));
			lines.addAll(printUnits("special", "Special units", n));
			lines.addAll(printUnits("sacred", "Sacreds", n));
			lines.addAll(printUnits("monsters", "Monsters", n));
			

			
			/*
			if(n.specialmonsters.size() > 0)
			{
				tw.println("Super monsters:");
				// supermobs
			
				for(ShapeChangeUnit su : n.specialmonsters)
				{
					String name = "";
					int id = -1;
					for(Command c : su.thisForm.commands)
					{
						if(c.command.equals("#name"))
							name = c.args.get(0).replaceAll("'", "").replaceAll("\"", "");
						else if(c.command.equals("#copystats"))
							id = Integer.parseInt(c.args.get(0));
					}
					
					if(name.equals("") && id != -1)
					{
						name = n.nationgen.units.GetValue(id, "unitname");
					}
					
					tw.println("** " + NameGenerator.capitalize(name));
					if(su.thisForm.tags.contains("caponly"))
						tw.println("--- Capital only");
					if(id != -1)
						tw.println("--- Based on non-UnitGen unit " + id);
						
				}				
			}
			*/
			lines.add("");
			lines.add("Commanders:");
			lines.add("- Scouts:");
			for(Unit u : n.generateComList("scout"))
				lines.addAll(getTroopInfo(u));
			lines.add("- Commanders:");
			for(Unit u : n.generateComList("commander"))
				lines.addAll(getTroopInfo(u));
			if(n.generateComList("specialcoms").size() > 0)
			{
				lines.add("- Special commanders:");
				for(Unit u : n.generateComList("specialcoms"))
					lines.addAll(getTroopInfo(u));
			}
			lines.add("- Priests:");
			for(Unit u : n.generateComList("priest"))
				lines.addAll(getTroopInfo(u));
			lines.add("- Mages:");
			for(Unit u : n.generateComList("mage"))
				lines.addAll(getTroopInfo(u));
			lines.add("");
			lines.add("Heroes:");
			for(Unit u : n.heroes)
				lines.addAll(getTroopInfo(u));
			lines.add("");

		
			lines.addAll(printPDInfo(n));
			lines.add("");

			// Spells
			lines.add("National spells:");
			lines.add("--------------");
			String line = "";
	
			for(String str : n.getSpells())
			{
				if(line.length() == 0)
					line = str;
				else if(line.length() + str.length() + 2 <= 160)
					line = line + ", " + str;
				else
				{
					lines.add(line + ",");
					line = str;
				}
			}
			
			lines.add(line);
			lines.add("");
			
			lines.add("Montag units:");
			lines.add("--------------");
			lines.addAll(printUnits("montagmages", "Mages", n));
			lines.addAll(printUnits("montagsacreds", "Sacreds and Elites", n));
			lines.addAll(printUnits("montagtroops", "Troops", n));
			lines.add("");
			
			
			System.out.print(".");
		}
		System.out.println(" Done!");
		
		return lines;
	}
	
	private String getSubrace(Unit u) {
		
		return u.slotmap.keySet().stream()
				.map(u::getSlot)
				.filter(Objects::nonNull)
				.flatMap(i -> i.tags.getString("subrace").stream())
				.findAny()
				.orElseGet(() -> u.pose.tags.getString("subrace").orElse(null));
	}
	
	private List<String> getWeaponNames(Unit u) {
		List<String> weaponNames = new ArrayList<>();
		for(String str : u.slotmap.keySet())
		{
			if(u.getSlot(str) == null)
				continue;
			
			if(!u.getSlot(str).armor && !u.getSlot(str).id.equals("-1"))
				weaponNames.add(weapondb.GetValue(u.getSlot(str).id, "weapon_name"));
			
		}
		return weaponNames;
	}
	
	private List<String> getArmorNames(Unit u) {
		List<String> armorNames = new ArrayList<>();
		for(String str : u.slotmap.keySet())
		{
			if(u.getSlot(str) == null)
				continue;
			if(u.getSlot(str).armor && !u.getSlot(str).id.equals("-1"))
				armorNames.add(armordb.GetValue(u.getSlot(str).id, "armorname"));
		}
		return armorNames;
	}
	
	private Optional<String> getMountName(Unit u) {
		if(u.getSlot("mount") != null)
		{
			String mountname = u.getSlot("mount").tags.getString("animal")
				.orElseThrow(() -> new IllegalStateException("Mount of unit " + u + " doesn't have an 'animal' tag"));
					
			return Optional.of(NameGenerator.capitalize(mountname) + " mount");
		}
		return Optional.empty();
	}
	
	private String getTroopNameCostGear(Unit u)
	{
		StringBuilder line = new StringBuilder("** " + u.getName());
		
		String subrace = getSubrace(u);
		
		line.append(" (").append(u.race.name);
		if(subrace != null) line.append(" - ").append(subrace);
		line.append("), ");
		
		
		line.append(u.getGoldCost()).append("g, ").append(u.getResCost(true)).append("r, ");
		
		List<String> gear = new ArrayList<>();
		
		gear.addAll(getWeaponNames(u));
		gear.addAll(getArmorNames(u));
		getMountName(u).ifPresent(gear::add);
		
		line.append(String.join(", ", gear)).append(".");
		
		return line.toString();
	}
	
	private Optional<String> getTroopMagicStuff(Unit u) {
		// Magic things!
		int[] paths = new int[10];
		double rand = 0;
		List<MagicPath> randoms = new ArrayList<>();
		
		
		boolean links = false;
		for(Filter f : u.appliedFilters)
		{
			if(f.name.equals("MAGICPICKS") || f.name.equals("PRIESTPICKS"))
			{
				
				for(Command c : f.getCommands())
				{
					if(c.command.equals("#magicskill"))
					{
						paths[c.args.get(0).getInt()] += c.args.get(1).getInt();
					}
					else if(c.command.equals("#custommagic"))
					{
						for(MagicPath path : MagicPath.listFromMask(c.args.get(0).getInt()))
							if(!randoms.contains(path))
								randoms.add(path);
						paths[9] += c.args.get(0).getInt() / 100;
						rand += c.args.get(0).getDouble() / 100;
						
						if(c.args.get(0).getDouble() / 100 > 1)
							links = true;
						
					}
				}
			}
		}
		
		
		String magicstuff = " ";
		String[] pathnames = {"F", "A", "W", "E", "S", "D", "N", "B", "H", "?"};
		for(int i = 0; i < 10; i++)
		{
			if(paths[i] > 0 || (i == 9 && rand > 0))
			{
				
				if(rand > paths[i] && i == 9)
					magicstuff = magicstuff + rand + pathnames[i];
				else
					magicstuff = magicstuff + paths[i] + pathnames[i];
				magicstuff = magicstuff + " ";
			}
		}
		magicstuff = magicstuff.trim();
		
		
		if(randoms.size() > 0)
		{
			magicstuff = magicstuff + " (";
			for(MagicPath path : randoms)
				magicstuff = magicstuff + path.name + ", ";
			
			if(links)
				magicstuff = magicstuff + "at least partially linked, ";
			
			magicstuff = magicstuff.substring(0, magicstuff.length() - 2) + ")";
		}
		magicstuff = magicstuff + ". ";
		
		
		if(!magicstuff.equals(". "))
		{
			magicstuff = magicstuff.trim();
			return Optional.of(magicstuff);
		}
		return Optional.empty();
	}
	
	private List<String> getTroopSpecialFeatures(Unit u)
	{
		// Filters and item special things
		List<String> stuff = new ArrayList<>();
		if(u.caponly)
			stuff.add("Capital only");
		
		boolean str = false;
		boolean holy = false;
		for(Command c : u.getCommands())
		{
			if(c.command.equals("#holy"))
				holy = true;
			if(c.command.equals("#slowrec"))
				str = true;
		}
		if(str)
			stuff.add("Slow to recruit");
		if(holy)
			stuff.add("Sacred");
		
		if(u.tags.containsName("montagunit"))
		{
			int shape = -1;
			for(Command c : u.getCommands())
				if(c.command.equals("#firstshape"))
					shape = -1 * c.args.get(0).getInt();
			
			if(shape != -1)
				stuff.add("Becomes unit of montag " + shape + " when recruited");
		}
		
		
		if(u.tags.containsName("hasmontag"))
		{
			int shape = -1;
			for(Command c : u.getCommands())
				if(c.command.equals("#montag"))
					shape = c.args.get(0).getInt();
			
			if(shape != -1)
				stuff.add("Has montag " + shape);
		}
		
		for(Filter f : u.appliedFilters)
		{
			if(f.tags.containsName("do_not_show_in_descriptions"))
				continue;
			
			String text = f.tags.getString("description").orElse(f.name);
			
			if(text != null)
				stuff.add(text);
		}
		
		return stuff;
	}
	
	private List<String> getTroopItemDescriptions(Unit u) {
		List<String> descriptions = new ArrayList<>();
		for(Item item : u.slotmap.values())
		{
			if(item == null)
				continue;
			
			descriptions.addAll(item.tags.getAllStrings("description"));
		}
		return descriptions;
	}
	
	private List<String> getTroopInfo(Unit u)
	{
		// Gear
		List<String> lines = new ArrayList<>();
		lines.add(getTroopNameCostGear(u));
		
		// Commands
		
		getTroopMagicStuff(u).ifPresent(magic -> lines.add("--- " + magic));
		
		
		List<String> features = getTroopSpecialFeatures(u);
		if(!features.isEmpty())
			lines.add("--- " + writeAsList(features, false));
		
		
		List<String> itemDescriptions = getTroopItemDescriptions(u);
		
		if(!itemDescriptions.isEmpty())
			lines.add("--- " + writeAsList(itemDescriptions, false));
		
		
		return lines;
	}

	
	private String writeAsList(List<String> list, boolean capitalize)
	{
		String str = "";
		for(int i = 0; i < list.size(); i++)
		{
			if(capitalize)
				str = str + NameGenerator.capitalize(list.get(i));
			else
				str = str + list.get(i);
			
			if(i < list.size() - 2)
				str = str + ", ";
			if(i == list.size() - 2)
				str = str + " and ";
		}
		
		return str;
	}
}
