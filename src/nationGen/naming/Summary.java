package nationGen.naming;


import com.elmokki.Generic;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.misc.Arg;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class Summary {

	public String race = "";
	public String military = "";
	public String magic = "";
	public String priest = "";
	
	private Nation n;
	
	public Summary(Nation nation) {
		this.n = nation;
	}
	
	public void update()
	{
		MagicPathInts maxPaths = findMaxPaths(n);
		NationFeatures features = new NationFeatures(n);

		race = summarizeRace(n);
		military = summarizeMilitary(n, features);
		magic = summarizeMagic(n, maxPaths, features);
		priest = summarizePriests(maxPaths, features);
	}
	
	
	private static String summarizeMilitary(Nation n, NationFeatures features)
	{
		List<String> rangeds = new ArrayList<>();
		List<String> lightmounts = new ArrayList<>();
		List<String> heavymounts = new ArrayList<>();
		List<String> elites = new ArrayList<>();
		int lightinf = 0;
		int heavyinf = 0;
		int lightcav = 0;
		int heavycav = 0;
		int chariots = 0;


		for(List<Unit> list : n.getListsOfType("infantry", false, true))
		{
			if(list.size() == 0)
				continue;
			
			if(list.get(0).getTotalProt() < 12)
				lightinf += list.size();
			else
				heavyinf += list.size();
		}
		
		for(List<Unit> list : n.getListsOfType("ranged", false, true))
		{
			for(Unit u : list)
			{
				if(u.tags.containsName("elite") && !elites.contains(u.name.type.toString()))
				{
					elites.add(u.name.type.toString());
				}
				else if(!rangeds.contains(u.name.type.toString()))
					rangeds.add(u.name.type.toString());	
			}
		}
		
		for(List<Unit> list : n.getListsOfType("mounted", false, true))
		{
			if(list.size() == 0)
				continue;
			
			if(list.get(0).getTotalProt() < 12)
			{
				lightcav += list.size();
				for(Unit u : list)
				{
					if(u.tags.containsName("elite") && !elites.contains("cavalry"))
					{
						elites.add("cavalry");
					}
					
					if(u.getSlot("mount") != null)
					{
						for (Arg value : u.getSlot("mount").tags.getAllValues("animal")) {
							String mounttype = value.get();
							if (!lightmounts.contains(mounttype))
								lightmounts.add(mounttype);
						}
					}
				}
			}
			else
			{
				heavycav += list.size();
				for(Unit u : list)
				{
					if(u.getSlot("mount") != null)
					{
						for (Arg value : u.getSlot("mount").tags.getAllValues("animal")) {
							String mounttype = value.get();
							if (!heavymounts.contains(mounttype))
								heavymounts.add(mounttype);
						}
					}
				}
			}

		}
		
		for(List<Unit> list : n.getListsOfType("chariot", false, true))
		{
			if(list.size() == 0)
				continue;
			
			chariots += list.size();
			
			for(Unit u : list)
			{
				if(u.tags.containsName("elitefied") && !elites.contains("chariot"))
				{
					elites.add("chariot");
				}
			}
		}
		
		
		// SUPER MONSTERS!
		
		
		
		// Sacreds
		
		List<String> sacreds = new ArrayList<>();
		n.selectTroops("sacred").forEach(sacredunit -> {
			StringBuilder sacred = new StringBuilder();

			if(sacredunit.race != n.races.get(0))
				sacred.append(sacredunit.race.visiblename).append(" ");

			if(sacredunit.getTotalProt() >= 12)
				sacred.append("heavy ");
			else
				sacred.append("light ");

			
			if((sacredunit.pose.roles.contains("mounted") || sacredunit.pose.roles.contains("sacred mounted")) && sacredunit.getSlot("mount") != null)
			{
				String mount = sacredunit.getSlot("mount").tags.getString("animal").orElseThrow();
				if(!"horse".equals(mount))
				{
					sacred.append(mount).append(" ");
				}

				sacred.append("cavalry");
			}
			else if(sacredunit.pose.roles.contains("infantry") || sacredunit.pose.roles.contains("sacred infantry"))
				sacred.append("infantry");
			else if(sacredunit.pose.roles.contains("chariot") || sacredunit.pose.roles.contains("sacred chariot"))
				sacred.append("chariot");
			else if(sacredunit.pose.roles.contains("ranged") || sacredunit.pose.roles.contains("sacred ranged"))
			{
				if(n.nationGen.weapondb.GetInteger(sacredunit.getSlot("weapon").id, "rng") > 12)
					sacred.append("ranger");
				else
					sacred.append("skirmisher");
			}
			sacreds.add(sacred.toString());
		});

		StringBuilder military = new StringBuilder("Military: ");

		// inf
		List<String> infantryList = new ArrayList<>();
		if(lightinf > 0)
			infantryList.add("light infantry");
		if(heavyinf > 0)
			infantryList.add("heavy infantry");

		if(!infantryList.isEmpty())
			military.append(Generic.capitalizeFirst(String.join(" and ", infantryList))).append(". ");
		
		// ranged
		if(!rangeds.isEmpty())
			military.append(Generic.capitalizeFirst(NameGenerator.writeAsList(rangeds, " and ", NameGenerator::plural))).append(". ");
		
		// cav
		List<String> cavDescriptions = new ArrayList<>();
		if (lightcav > 0) {
			String lightmount = NameGenerator.writeAsList(lightmounts.stream()
					.map(m -> String.join(" ", Generic.parseArgs(m, "'")))
					.collect(Collectors.toList()), " and ");

			cavDescriptions.add("light" + (lightmount.equals("horse") ? "" : (" " + lightmount)) + " cavalry");
		}
		if (heavycav > 0) {
			String heavyMount = NameGenerator.writeAsList(heavymounts.stream()
					.map(m -> String.join(" ", Generic.parseArgs(m, "'")))
					.collect(Collectors.toList()), " and ");

			cavDescriptions.add("heavy" + (heavyMount.equals("horse") ? "" : (" " + heavyMount)) + " cavalry");
		}

		if(!cavDescriptions.isEmpty())
			military.append(Generic.capitalizeFirst(NameGenerator.writeAsList(cavDescriptions, " and "))).append(". ");
		
		if(chariots > 0)
			military.append("Chariots. ");
		
		// elites
		if(elites.size() > 0)
			military.append(Generic.capitalizeFirst(
					NameGenerator.writeAsList(elites, " and ", s -> "Elite " + s)))
				.append(". ");
		
		/*
		// supermobs
		List<String> supermobs = new ArrayList<String>();
		for(ShapeChangeUnit su : n.specialmonsters)
		{
			String name = "";
			int id = -1;
			for(Command c : su.thisForm.commands)
			{
				if(c.command.equals("#name"))
					name = c.argument.replaceAll("'", "").replaceAll("\"", "");
				else if(c.command.equals("#copystats"))
					id = Integer.parseInt(c.argument);
			}
			
			if(name.equals("") && id != -1)
			{
				name = n.nationgen.units.GetValue(id, "unitname");
			}
			
			supermobs.add(name);
				
		}
		
		if(supermobs.size() > 0)
		{
			temp = "";
			for(int i = 0; i < supermobs.size(); i++)
			{
				temp = temp + supermobs.get(i);
				
				if(i == supermobs.size() - 2)
					temp = temp + " and ";
				else if(i < supermobs.size() - 2)
					temp = temp + ", ";	
			}
			if(!temp.equals(""))
				military = military + Generic.capitalizeFirst(temp) + ". ";
		}
		*/
		
		military.append("Sacred ").append(NameGenerator.writeAsList(sacreds, false)).append(".");

		if(features.fortcost < 0)
			military.append(" Cheaper forts.");
		if(features.fortcost > 0)
			military.append(" Expensive forts.");

		return military.toString();
	}
	
	
	private static String summarizeRace(Nation n)
	{
		StringBuilder output = new StringBuilder("Race: ");

		output.append(Generic.capitalize(NameGenerator.plural(n.races.get(0).visiblename)));

		n.selectTroops()
				.map(u -> u.race)
				.distinct()
				.filter(r -> r != n.races.get(0))
				.forEach(r -> {
					long amount = n.selectUnits().filter(u -> u.race == r).count();

					if (amount > 1 && !r.visiblename.equals(n.races.get(0).visiblename))
						output.append(", some ").append(Generic.capitalize(NameGenerator.plural(r.visiblename)));
				});

		n.selectTroops()
				.filter(u -> u.tags.containsName("auxillary"))
				.map(u -> u.race)
				.distinct()
				.forEach(r -> output.append(", ").append(Generic.capitalize(r.name)).append(" auxillaries"));

		for(Command c : n.getCommands())
		{
			if(c.command.equals("#idealcold"))
			{
				int cold = c.args.get(0).getInt();
				if(cold < 0)
					output.append(", prefers Heat scale +").append(Math.abs(cold));
				else if(cold > 0)
					output.append(", prefers Cold scale +").append(Math.abs(cold));
			}
		}

		output.append(".");
		return output.toString();
	}

	private static MagicPathInts findMaxPaths(Nation n) {
		MagicPathInts maxPaths = new MagicPathInts();
		n.selectCommanders().forEach(u -> {
			MagicPathInts rpaths = u.getMagicPicks(true);
			MagicPathInts paths = u.getMagicPicks(false);

			for (MagicPath path : MagicPath.values()) // Get rid of single randoms that don't reinforce non-random paths
				if (paths.get(path) != 0 || rpaths.get(path) > 1) // If a non-random path is not zero or randoms aren't linking
					paths.set(path, rpaths.get(path)); // use randoms and non-randoms on that path

			maxPaths.maxWith(paths);
		});
		return maxPaths;
	}
	
	private static String summarizeMagic(Nation n, MagicPathInts maxPaths, NationFeatures features) {


		// Divide to weak and strong
		List<String> strong = new ArrayList<>();
		List<String> weak = new ArrayList<>();
		for (MagicPath path : MagicPath.NON_HOLY) {
			if (maxPaths.get(path) > 2) // Strong if 3+ available
				strong.add(path.name);
			else if (maxPaths.get(path) > 0) // Weak if 1-2 is available
				weak.add(path.name);
		}

		StringBuilder magic = new StringBuilder("Magic: ");

		if (strong.size() > 0)
			magic.append(NameGenerator.writeAsList(strong, true)).append(".");

		if (weak.size() > 0)
			magic.append(" Weak ").append(NameGenerator.writeAsList(weak, true)).append(".");

		boolean primarydrainimmune = false; // #drainimmune (on units);
		boolean secondarydrainimmune = false; // #drainimmune (on units);
		for (Unit u : n.listCommanders("mage")) {
			if (u.getCommands().stream().anyMatch(c -> c.command.equals("#drainimmune"))) {
				if (u.tags.containsName("schoolmage")) {
					primarydrainimmune = true;
				} else {
					secondarydrainimmune = true;
				}
			}
		}

		if (primarydrainimmune) {
			magic.append(" National mages are not affected by Drain scale.");
		} else if (secondarydrainimmune) {
			n.selectCommanders("mages-2").findFirst()
					.ifPresent(u -> magic.append(n.comlists.get("mages-2").get(0).name).append(" is not affected by Drain scale."));
		}

		n.selectCommanders("mage")
			.filter(u -> u.race != n.races.get(0))
			.filter(u -> !u.race.visiblename.equals(n.races.get(0).visiblename))
			.distinct()
			.forEach(r -> magic.append(" Some ").append(r.name).append(" mages."));


		// Lab cost
		if (features.labcost != 500)
			magic.append(" Laboratories cost ").append(features.labcost).append(" gold.");

		return magic.toString();
	}

	private static String summarizePriests(MagicPathInts maxPaths, NationFeatures features) {
		// Priests;
		
		StringBuilder priest = new StringBuilder("Priests: ");
		if(maxPaths.get(MagicPath.HOLY) == 1)
			priest.append("Weak");
		else if(maxPaths.get(MagicPath.HOLY) == 2)
			priest.append("Moderate");
		else if(maxPaths.get(MagicPath.HOLY) >= 3)
			priest.append("Strong");
		

		if(features.nopreach)
			priest.append(", Dominion does not spread unless blood is sacrificed");
		if(features.templecost != 400)
			priest.append(", temples cost ").append(features.templecost).append(" gold");
		if(features.reanim)
			priest.append(", can reanimate the dead");
		if(features.bloodsac)
			priest.append(", can perform blood sacrifices");
		if(features.manikin)
			priest.append(", can reanimate manikin");
		if(features.golemcult > 0)
			priest.append(", their constructs are stronger within their Dominion");
		
		priest.append(".");

		return priest.toString();
	}
	
	public String toString()
	{
		return race + "\n\n" + military + "\n\n" + magic + "\n\n" + priest;
	}

	private static class NationFeatures {
		boolean nopreach = false; // #nopreach
		boolean reanim = false; // #priestreanim
		boolean bloodsac = false; // #sacrificedom
		boolean manikin = false; // #manikinreanim
		int templecost = 400;
		int labcost = 500;
		int fortcost = 0;
		int golemcult = 0;

		private NationFeatures(Nation n) {
			for(Command c : n.commands)
			{
				if(c.command.equals("#nopreach"))
					nopreach = true;
				if(c.command.equals("#priestreanim"))
					reanim = true;
				if(c.command.equals("#sacrificedom"))
					bloodsac = true;
				if(c.command.equals("#manikinreanim"))
					manikin = true;
				if(c.command.equals("#golemhp"))
					golemcult = c.args.get(0).getInt();
				if(c.command.equals("#templecost"))
					templecost = c.args.get(0).getInt();
				if(c.command.equals("#labcost"))
					labcost = c.args.get(0).getInt();
				if(c.command.equals("#fortcost"))
					fortcost = c.args.get(0).getInt();
			}
		}
	}
}
