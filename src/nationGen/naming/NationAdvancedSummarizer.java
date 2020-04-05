package nationGen.naming;


import com.elmokki.Dom3DB;
import com.elmokki.Generic;
import nationGen.entities.Filter;
import nationGen.entities.Race;
import nationGen.entities.Theme;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.misc.Command;
import nationGen.misc.FileUtil;
import nationGen.misc.Site;
import nationGen.nation.Nation;
import nationGen.nation.PDSelector;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;
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
		List<Unit> troops = n.listTroops(role);
		if(!troops.isEmpty())
		{
			lines.add("- " + tag + ":");
			for(Unit u : troops)
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
				else if(c.command.equals("#uwbuild") && c.args.get(0).get().equals("1"))
				{
					traits.add("Can build forts underwater");
				}
				else if(c.command.equals("#buildfort") && c.args.get(0).get().equals("11"))
				{
					traits.add("Fortified cities");
				}
				else if(c.command.equals("#buildfort") && c.args.get(0).get().equals("15"))
				{
					traits.add("Giant forts");
				}
				else if(c.command.equals("#buildfort") && c.args.get(0).get().equals("20"))
				{
					traits.add("Ice forts");
				}
				else if(c.command.equals("#buildfort") && c.args.get(0).get().equals("27"))
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
					
					tw.println("** " + Generic.capitalize(name));
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
			n.selectCommanders("scout").forEach(u -> lines.addAll(getTroopInfo(u)));

			lines.add("- Commanders:");
			n.selectCommanders("commander").forEach(u -> lines.addAll(getTroopInfo(u)));

			if(n.selectCommanders("specialcoms").findAny().isPresent()) {
				lines.add("- Special commanders:");
				n.selectCommanders("specialcoms").forEach(u -> lines.addAll(getTroopInfo(u)));
			}
			lines.add("- Priests:");
			n.selectCommanders("priest").forEach(u -> lines.addAll(getTroopInfo(u)));

			lines.add("- Mages:");
			n.selectCommanders("mage").forEach(u -> lines.addAll(getTroopInfo(u)));

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
			StringBuilder line = new StringBuilder();
	
			for(String str : n.getSpells())
			{
				if(line.length() == 0)
					line = new StringBuilder(str);
				else if(line.length() + str.length() + 2 <= 160)
					line.append(", ").append(str);
				else
				{
					lines.add(line + ",");
					line = new StringBuilder(str);
				}
			}
			
			lines.add(line.toString());
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
		
		return u.slotmap.items()
				.flatMap(i -> i.tags.getString("subrace").stream())
				.findAny()
				.orElseGet(() -> u.pose.tags.getString("subrace").orElse(null));
	}
	
	private List<String> getWeaponNames(Unit u) {
		return u.slotmap.items()
			.filter(i -> !i.armor)
			.filter(i -> !i.id.equals("-1"))
			.map(i -> weapondb.GetValue(i.id, "weapon_name"))
			.collect(Collectors.toList());
	}
	
	private List<String> getArmorNames(Unit u) {
		return u.slotmap.items()
			.filter(i -> i.armor)
			.filter(i -> !i.id.equals("-1"))
			.map(i -> armordb.GetValue(i.id, "armorname"))
			.collect(Collectors.toList());
	}
	
	private Optional<String> getMountName(Unit u) {
		if(u.getSlot("mount") != null)
		{
			String mountname = u.getSlot("mount").tags.getString("animal")
				.orElseThrow(() -> new IllegalStateException("Mount of unit " + u + " doesn't have an 'animal' tag"));
					
			return Optional.of(Generic.capitalize(mountname) + " mount");
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
		MagicPathInts paths = new MagicPathInts();
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
						paths.add(c.args.get(0).getMagicPath(), c.args.get(1).getInt());
					}
					else if(c.command.equals("#custommagic"))
					{
						for(MagicPath path : MagicPath.listFromMask(c.args.get(0).getInt()))
							if(!randoms.contains(path))
								randoms.add(path);
						double thisRand = c.args.get(1).getDouble() / 100;
						rand += thisRand;
						
						if(thisRand > 1)
							links = true;
						
					}
				}
			}
		}
		
		
		StringBuilder magicstuff = new StringBuilder();
		List<String> pathDescriptions = new ArrayList<>();
		
		paths.stream()
			.filter(p -> p.level > 0)
			.forEach(p -> pathDescriptions.add(p.level + p.path.letter));
		
		if (rand > 0) {
			pathDescriptions.add(rand + "?");
		}
		magicstuff.append(String.join(" ", pathDescriptions));
		
		
		if(randoms.size() > 0)
		{
			magicstuff.append(" (").append(randoms.stream().map(p -> p.name).collect(Collectors.joining(", ")));
			
			if(links)
				magicstuff.append(", at least partially linked");
			
			magicstuff.append(")");
		}


		if(magicstuff.length() != 0)
		{
			return Optional.of(magicstuff.append(". ").toString());
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
		return u.slotmap.items()
			.flatMap(i -> i.tags.getAllStrings("description").stream())
			.collect(Collectors.toList());
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
			lines.add("--- " + NameGenerator.writeAsList(features));
		
		
		List<String> itemDescriptions = getTroopItemDescriptions(u);
		
		if(!itemDescriptions.isEmpty())
			lines.add("--- " + NameGenerator.writeAsList(itemDescriptions));
		
		
		return lines;
	}
}
