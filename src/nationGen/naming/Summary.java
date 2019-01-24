package nationGen.naming;

import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

import nationGen.entities.Race;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;



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

				if(u.tags.contains("elite") && !elites.contains(u.name.type.toString()))
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
					if(u.tags.contains("elite") && !elites.contains("cavalry"))
					{
						elites.add("cavalry");
					}
					
					if(u.getSlot("mount") != null)
						for(String tag : u.getSlot("mount").tags)
						{
							if(tag.startsWith("animal"))
							{
								String mounttype = tag.replaceAll("\"", "");
								List<String> args = Generic.parseArgs(mounttype, "'");
								args.remove(0);
								String str = "";
								for(String s : args)
									str = str + s + " ";
								str = str.trim();
								if(!lightmounts.contains(str))
									lightmounts.add(str);
							}
						}
				}
			}
			else
			{
				heavycav += list.size();
				for(Unit u : list)
				{
					for(String tag : u.getSlot("mount").tags)
					{
						if(u.getSlot("mount") != null)
							if(tag.startsWith("animal"))
							{
								String mounttype = tag.replaceAll("\"", "");
								List<String> args = Generic.parseArgs(mounttype, "'");
								args.remove(0);
								String str = "";
								for(String s : args)
									str = str + s + " ";
								str = str.trim();
								if(!heavymounts.contains(str))
									heavymounts.add(str);
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
				if(u.tags.contains("elitefied") && !elites.contains("chariot"))
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

					for(String tag : sacredunit.getSlot("mount").tags)
					{
						if(tag.startsWith("animal"))
							if(!tag.split(" ")[1].equals("horse"))
							{
								String mounttype = tag.replaceAll("\"", "");
								List<String> args = Generic.parseArgs(mounttype, "'");
								args.remove(0);
								String str = "";
								for(String s : args)
									str = str + s + " ";
								str = str.trim();
								sacred = sacred + str;
							}
						
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
			if(u.tags.contains("auxillary") && !auxillaries.contains(u.race))
				auxillaries.add(u.race);
		
		for(Race r : auxillaries)
				race = race + ", " + NameGenerator.capitalize(r.name) + " auxillaries";
		
		
		race = race.trim();
		
		for(Command c : n.getCommands())
		{
			if(c.command.equals("#idealcold"))
			{
				String arg = c.args.get(0);
				if(arg.startsWith("+"))
					arg = arg.substring(1);
				
				int cold = Integer.parseInt(arg);
				if(cold < 0)
					race = race + ", prefers Heat scale +" + Math.abs(cold);
				else if(cold > 0)
					race = race + ", prefers Cold scale +" + Math.abs(cold);
			}
		}
		
		race = race + ".";
	}
	
	private void updateMagic()
	{
		double[] total = new double[9];
		for(Unit u : n.generateComList())
		{
		
			int[] rpaths = u.getMagicPicks(true);
			int[] paths = u.getMagicPicks(false);
			
			for(int i = 0; i < 9; i++) // Get rid of single randoms that don't reinforce non-random paths
				if(paths[i] != 0 || rpaths[i] > 1) // If a non-random path is not zero or randoms aren't linking
					paths[i] = rpaths[i]; // use randoms and non-randoms on that path
				
			for(int i = 0; i < 9; i++)
				if(total[i] < paths[i])
					total[i] = paths[i];
		}
		
		// Divide to weak and strong
		List<String> strong = new ArrayList<String>();
		List<String> weak = new ArrayList<String>();
		for(int i = 0; i < 8; i++)
		{
			if(total[i] > 2) // Strong if 3+ available
				strong.add(Generic.integerToPath(i));
			else if(total[i] > 0) // Weak if 1-2 is available
				weak.add(Generic.integerToPath(i));
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
			boolean primary = false;
			boolean drainimmune = false;
			for(String tag : u.tags)
				if(tag.startsWith("schoolmage"))
					primary = true;
			
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
				golemcult = Integer.parseInt(c.args.get(0));
			if(c.command.equals("#templecost"))
				templecost = Integer.parseInt(c.args.get(0));
			if(c.command.equals("#labcost"))
				labcost = Integer.parseInt(c.args.get(0));
			if(c.command.equals("#fortcost"))
				fortcost = Integer.parseInt(c.args.get(0));
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
		if(total[8] == 1)
			priest = priest + "Weak";
		else if(total[8] == 2)
			priest = priest + "Moderate";
		else if(total[8] >= 3)
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
