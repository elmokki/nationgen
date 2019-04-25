package nationGen.naming;


import com.elmokki.Generic;
import nationGen.entities.Race;
import nationGen.magic.MagicPath;
import nationGen.magic.MagicPathInts;
import nationGen.misc.Arg;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;

import java.util.ArrayList;
import java.util.List;



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
		updateMilitary();
		updateRace();
		updateMagic();
	}
	
	
	private void updateMilitary()
	{
		military = "";
		
		List<String> rangeds = new ArrayList<String>();
		List<String> lightmounts = new ArrayList<String>();
		List<String> heavymounts = new ArrayList<String>();
		List<String> elites = new ArrayList<String>();
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
			if(list.size() == 0)
				continue;
			
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
		
		List<String> sacreds = new ArrayList<String>();
		if(n.generateUnitList("sacred") != null && n.generateUnitList("sacred").size() > 0)
		{
			for(Unit sacredunit : n.generateUnitList("sacred"))
			{
				String sacred = "";
				
				if(sacredunit.race != n.races.get(0))
					sacred = sacred + sacredunit.race.visiblename + " ";
					
				if(sacredunit.getTotalProt() >= 12)
					sacred = sacred + "heavy ";
				else
					sacred = sacred + "light ";
				
				
				
				if((sacredunit.pose.roles.contains("mounted") || sacredunit.pose.roles.contains("sacred mounted")) && sacredunit.getSlot("mount") != null)
				{

					String mount = sacredunit.getSlot("mount").tags.getString("animal").orElseThrow();
					if(!"horse".equals(mount))
					{
						sacred = sacred + mount;
					}
					
					sacred = sacred + " cavalry";
				}
				else if(sacredunit.pose.roles.contains("infantry") || sacredunit.pose.roles.contains("sacred infantry"))
					sacred = sacred + " infantry";
				else if(sacredunit.pose.roles.contains("chariot") || sacredunit.pose.roles.contains("sacred chariot"))
					sacred = sacred + " chariot";
				else if(sacredunit.pose.roles.contains("ranged") || sacredunit.pose.roles.contains("sacred ranged"))
				{
					if(n.nationGen.weapondb.GetInteger(sacredunit.getSlot("weapon").id, "rng") > 12)
						sacred = sacred + " ranger";
					else
						sacred = sacred + " skirmisher";
				}
				sacreds.add(sacred);
			}
			
		}
		
		List<String> list = new ArrayList<String>();
		
		// inf
		if(lightinf > 0)
			list.add("light");
		if(heavyinf > 0)
			list.add("heavy");
		
		String temp = "";
		for(int i = 0; i < list.size(); i++)
		{
			temp  = temp  + list.get(i) + " infantry";
			if(i == list.size() - 2)
				temp  = temp  + " and ";
			else if(i < list.size() - 2)
				temp  = temp  + ", ";	
		}
		if(!temp.equals(""))
			military = military + NameGenerator.capitalizeFirst(temp) + ". ";
		
		// ranged
		temp = "";
		for(int i = 0; i < rangeds.size(); i++)
		{
			temp = temp + NameGenerator.plural(rangeds.get(i));
			
			if(i == rangeds.size() - 2)
				temp = temp + " and ";
			else if(i < rangeds.size() - 2)
				temp = temp + ", ";	
		}
		if(!temp.equals(""))
			military = military + NameGenerator.capitalizeFirst(temp) + ". ";
		
		// cav
		list.clear();
		if(lightcav > 0)
			list.add("light");
		if(heavycav > 0)
			list.add("heavy");
		
		temp = "";
		for(int i = 0; i < list.size(); i++)
		{
			
			temp = temp + list.get(i);
			
			if(list.get(i).equals("light"))
				for(int j = 0; j < lightmounts.size(); j++)
				{
					List<String> args = Generic.parseArgs(lightmounts.get(j), "'");
					String str = "";
					for(String s : args)
						str = str + s + " ";
					str = str.trim();
					
					if(lightmounts.size() == 1 && str.equals("horse"))
						continue;
					temp = temp + " " + str;
					if(j == lightmounts.size() - 2)
						temp = temp + " and";
					else if(j < lightmounts.size() - 2)
						temp = temp + ", ";	
				}
			else if(list.get(i).equals("heavy"))
				for(int j = 0; j < heavymounts.size(); j++)
				{
					List<String> args = Generic.parseArgs(heavymounts.get(j), "'");
					String str = "";
					for(String s : args)
						str = str + s + " ";
					str = str.trim();
					
					if(heavymounts.size() == 1 && str.equals("horse"))
						continue;
					temp = temp + " " + str;
					if(j == heavymounts.size() - 2)
						temp = temp + " and";
					else if(j < heavymounts.size() - 2)
						temp = temp + ", ";	
					
				}
			
			temp = temp + " cavalry";
			

			
			if(i == list.size() - 2)
				temp = temp + " and ";
			else if(i < list.size() - 2)
				temp = temp + ", ";	
		}
		if(!temp.equals(""))
			military = military + NameGenerator.capitalizeFirst(temp) + ". ";
		
		if(chariots > 0)
			military = military + "Chariots. ";
		
		// elites
		if(elites.size() > 0)
		{
			temp = "";
			for(int i = 0; i < elites.size(); i++)
			{
				temp = temp + "Elite " + elites.get(i);
				
				if(i == elites.size() - 2)
					temp = temp + " and ";
				else if(i < elites.size() - 2)
					temp = temp + ", ";	
			}
			if(!temp.equals(""))
				military = military + NameGenerator.capitalizeFirst(temp) + ". ";
		}
		
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
				military = military + NameGenerator.capitalizeFirst(temp) + ". ";
		}
		*/
		
		military = military + "Sacred " + this.writeAsList(sacreds, false) + ".";
		military = "Military: " + military;
		military = military.replaceAll("  ", " ");
	}
	
	
	private void updateRace()
	{
		race = "Race: ";

		
		List<Race> races = new ArrayList<Race>();
		for(Unit u : n.generateTroopList())
		{
			if(!races.contains(u.race) && u.race != n.races.get(0))
				races.add(u.race);
		}
		
		race = race + NameGenerator.capitalize(NameGenerator.plural(n.races.get(0).visiblename));

		for(Race r : races)
		{
			int amount = 0;
			List<Unit> ulist = new ArrayList<Unit>();
			ulist.addAll(n.generateTroopList());
			ulist.addAll(n.generateComList());
			for(Unit u : ulist)
				if(u.race == r)
					amount++;
			
			if(amount > 1 && !r.visiblename.equals(n.races.get(0).visiblename))	
				race = race + ", some " + NameGenerator.capitalize(NameGenerator.plural(r.visiblename));
		}
		
		List<Race> auxillaries = new ArrayList<Race>();
		for(Unit u : n.generateTroopList())
			if(u.tags.containsName("auxillary") && !auxillaries.contains(u.race))
				auxillaries.add(u.race);
		
		for(Race r : auxillaries)
				race = race + ", " + NameGenerator.capitalize(r.name) + " auxillaries";
		
		
		race = race.trim();
		
		for(Command c : n.getCommands())
		{
			if(c.command.equals("#idealcold"))
			{
				int cold = c.args.get(0).getInt();
				if(cold < 0)
					race += ", prefers Heat scale +" + Math.abs(cold);
				else if(cold > 0)
					race += ", prefers Cold scale +" + Math.abs(cold);
			}
		}
		
		race = race + ".";
	}
	
	private void updateMagic()
	{
		MagicPathInts total = new MagicPathInts();
		for(Unit u : n.generateComList())
		{
		
			MagicPathInts rpaths = u.getMagicPicks(true);
			MagicPathInts paths = u.getMagicPicks(false);
			
			for(MagicPath path : MagicPath.values()) // Get rid of single randoms that don't reinforce non-random paths
				if(paths.get(path) != 0 || rpaths.get(path) > 1) // If a non-random path is not zero or randoms aren't linking
					paths.set(path, rpaths.get(path)); // use randoms and non-randoms on that path
				
			for(MagicPath path : MagicPath.values())
				if(total.get(path) < paths.get(path))
					total.set(path, paths.get(path));
		}
		
		// Divide to weak and strong
		List<String> strong = new ArrayList<String>();
		List<String> weak = new ArrayList<String>();
		for(MagicPath path : MagicPath.NON_HOLY)
		{
			if(total.get(path) > 2) // Strong if 3+ available
				strong.add(path.name);
			else if(total.get(path) > 0) // Weak if 1-2 is available
				weak.add(path.name);
		}
		
		magic = "Magic: ";
		for(int i = 0; i < strong.size(); i++)
		{
			String str = strong.get(i);
			magic = magic + NameGenerator.capitalize(str);
			
			if(i < strong.size() - 2)
				 magic = magic + ", ";
			else if(i == strong.size() - 2)
				 magic = magic + " and ";
		}
		
		if(magic.endsWith(", "))
			magic = magic.substring(0, magic.length() - 2);
		if(strong.size() > 0)
			magic = magic + ".";
		
		if(weak.size() > 0)
			magic = magic + " Weak ";
		
		for(int i = 0; i < weak.size(); i++)
		{
			String str = weak.get(i);
			magic = magic + NameGenerator.capitalize(str);
			
			if(i < weak.size() - 2)
				 magic = magic + ", ";
			else if(i == weak.size() - 2)
				 magic = magic + " and ";
		}
		
		if(magic.endsWith(", "))
			magic = magic.substring(0, magic.length() - 2);
		if(weak.size() > 0)
			magic = magic + ".";
		
		boolean primarydrainimmune = false; // #drainimmune (on units);
		boolean secondarydrainimmune = false; // #drainimmune (on units);
		for(Unit u : n.generateComList("mage"))
		{
			boolean primary = u.tags.containsName("schoolmage");
			boolean drainimmune = false;
			
			for(Command c : u.getCommands())
				if(c.command.equals("#drainimmune"))
					drainimmune = true;
			
			if(primary && drainimmune)
				primarydrainimmune = true;
			if(!primary && drainimmune)
				secondarydrainimmune = true;
		}
		
		if(primarydrainimmune)
			magic = magic + " National mages are not affected by Drain scale.";
		else if(secondarydrainimmune && n.comlists.get("mages-2") != null && n.comlists.get("mages-2").size() > 0)
		{
			magic = magic + n.comlists.get("mages-2").get(0).name + " is not affected by Drain scale.";
		}
		
		List<Race> races = new ArrayList<Race>();
		for(Unit u : n.generateComList("mage"))
		{
			if(u.race != n.races.get(0) && !races.contains(u.race) && !u.race.visiblename.equals(n.races.get(0).visiblename))
				races.add(u.race);
		}
		
		for(Race r : races)
			magic = magic + " Some " + r.name + " mages."; 
		
		
		
		// Get features 
		boolean nopreach = false; // #nopreach
		boolean reanim = false; // #priestreanim
		boolean bloodsac = false; // #sacrificedom
		boolean manikin = false; // #manikinreanim
		int templecost = 400;
		int labcost = 500;
		int fortcost = 0;
		int golemcult = 0;
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
		
		// Lab cost
		if(labcost != 500)
			magic = magic + " Laboratories cost " + labcost + " gold.";

		if(fortcost < 0)
			military = military + " Cheaper forts.";
		if(fortcost > 0)
			military = military + " Expensive forts.";
		
		// Priests;
		
		priest = "Priests: ";
		if(total.get(MagicPath.HOLY) == 1)
			priest = priest + "Weak";
		else if(total.get(MagicPath.HOLY) == 2)
			priest = priest + "Moderate";
		else if(total.get(MagicPath.HOLY) >= 3)
			priest = priest + "Strong";
		




			
		if(nopreach)
			priest = priest + ", Dominion does not spread unless blood is sacrificed";
		if(templecost != 400)
			priest = priest + ", temples cost " + templecost + " gold";
		if(reanim)
			priest = priest + ", can reanimate the dead";
		if(bloodsac)
			priest = priest + ", can perform blood sacrifices";
		if(manikin)
			priest = priest + ", can reanimate manikin";
		if(golemcult > 0)
			priest = priest + ", their constructs are stronger within their Dominion";
		
		priest = priest + ".";
		
	}
	
	public String toString()
	{
		return race + "\n\n" + military + "\n\n" + magic + "\n\n" + priest;
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
