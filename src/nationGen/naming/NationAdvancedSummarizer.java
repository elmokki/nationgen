package nationGen.naming;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nationGen.entities.Filter;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.Command;
import nationGen.misc.Site;
import nationGen.nation.Nation;
import nationGen.units.ShapeChangeUnit;
import nationGen.units.Unit;

import com.elmokki.Dom3DB;
import com.elmokki.Generic;


public class NationAdvancedSummarizer {
	
	Dom3DB weapondb;
	Dom3DB armordb;
	
	public NationAdvancedSummarizer(Dom3DB armor, Dom3DB weapon)
	{
		this.armordb = armor;
		this.weapondb = weapon;
	}
	

	
	
	private void printUnits(PrintWriter tw, String role, String tag, Nation n)
	{
		if(n.generateUnitList(role).size() > 0)
		{
			tw.println("- " + tag + ":");
			for(Unit u : n.generateUnitList(role))
				getTroopInfo(u, tw);
		}
	}
	

	
	private void writeDescription(Nation n, PrintWriter tw)
	{
		tw.println("Nation " + n.nationid + ": " + n.name);
		tw.println("-----------------------------------");
		tw.println(n.summary.race);
		tw.println(n.summary.military);
		tw.println(n.summary.magic);
		tw.println(n.summary.priest);

	}
			
	public void writeDescriptionFile(List<Nation> nations, String modname) throws IOException
	{
		String dir = "/nationgen_" + modname.toLowerCase().replaceAll(" ", "_") + "/";
		FileWriter fstream = new FileWriter("mods/" + dir + "descriptions.txt");
		PrintWriter tw = new PrintWriter(fstream);
		
		System.out.print("Writing descriptions");
		for(Nation n : nations)
		{
			writeDescription(n, tw);
			tw.println();
			System.out.print(".");
		}
		System.out.println(" Done!");
		tw.flush();
		tw.close();
	}
	
	public void writeAdvancedDescriptionFile(List<Nation> nations, String modname) throws IOException
	{
		String dir = "/nationgen_" + modname.toLowerCase().replaceAll(" ", "_") + "/";
		FileWriter fstream = new FileWriter("mods/" + dir + "advanceddescriptions.txt");
		PrintWriter tw = new PrintWriter(fstream);
		
		
		
		System.out.print("Writing advanced descriptions");
		for(Nation n : nations)
		{
			writeDescription(n, tw);
			tw.println("-----------------------------------");


			List<String> traits = new ArrayList<String>();
			for(Command c : n.getCommands())
				if(c.command.equals("#idealcold"))
				{
					traits.add("Ideal cold level " + c.args.get(0));
				}
				else if(c.command.equals("#uwbuild") && c.args.get(0).equals("1"))
				{
					traits.add("Can build forts underwater");
				}
			
			
			for(Filter f : n.appliedfilters)
			{
				
				if(Generic.containsTag(f.tags, "desc"))
					traits.add(Generic.getTagValue(f.tags, "desc"));
				else
					traits.add(f.name);
			
			}

			if(traits.size() > 0)
			{
				tw.println("National traits:");
				for(String trait : traits)
					tw.println("- " + trait);
				tw.println();
			}
			
			// Write themes
			for(Race r : n.races)
			{
				tw.print(r.name + " themes: ");
				String str = "";
				for(Filter f : r.themefilters)
				{
					
					if(Generic.containsTag(f.tags, "desc"))
						str = str + Generic.getTagValue(f.tags, "desc") + ", ";
					else
						str = str + f.name + ", ";
				}
				if(str.length() > 0)
					str = str.substring(0, str.length() - 2);
				tw.println(str);
			}
			tw.println();
			
			// Site features
			List<String> sitef = new ArrayList<String>();
			for(Site s : n.sites)
				for(Filter f : s.appliedfilters)
					sitef.add(f.toString());
			
			if(sitef.size() > 0)
			{
				tw.println("Magic site features: " + Generic.listToString(sitef, ","));
				tw.println();
			}
				
			
			
			// Units
			tw.println("Troops:");
			
			printUnits(tw, "ranged", "Ranged", n);
			printUnits(tw, "infantry", "Infantry", n);
			printUnits(tw, "mounted", "Cavalry", n);
			printUnits(tw, "chariot", "Chariots", n);
			printUnits(tw, "special", "Special units", n);
			printUnits(tw, "sacred", "Sacreds", n);
			printUnits(tw, "monsters", "Monsters", n);
			
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
			tw.println();
			tw.println("Commanders:");
			tw.println("- Scouts:");
			for(Unit u : n.generateComList("scout"))
				getTroopInfo(u, tw);
			tw.println("- Commanders:");
			for(Unit u : n.generateComList("commander"))
				getTroopInfo(u, tw);
			if(n.generateComList("specialcoms").size() > 0)
			{
				tw.println("- Special commanders:");
				for(Unit u : n.generateComList("specialcoms"))
					getTroopInfo(u, tw);
			}
			tw.println("- Priests:");
			for(Unit u : n.generateComList("priest"))
				getTroopInfo(u, tw);
			tw.println("- Mages:");
			for(Unit u : n.generateComList("mage"))
				getTroopInfo(u, tw);
			tw.println();
			tw.println("Heroes:");
			for(Unit u : n.heroes)
				getTroopInfo(u, tw);
			tw.println();

			
			
			
			
			System.out.print(".");
		}
		System.out.println(" Done!");
		tw.flush();
		tw.close();
	}
	
	
	private String getTroopInfo(Unit u, PrintWriter tw)
	{
		// Gear
		String line = "** " + u.getName();
		
		String subrace = null;
		
		for(String str : u.slotmap.keySet())
		{
			if(u.getSlot(str) == null)
				continue;
			if(Generic.containsTag(u.getSlot(str).tags, "subrace"))
				subrace = Generic.getTagValue(u.getSlot(str).tags, "subrace");
		}
		
		if(subrace != null)
			line = line + " (" + u.race.name  + " - " + subrace +  "), ";
		else if(Generic.containsTag(u.pose.tags, "subrace"))
			line = line + " (" + u.race.name  + " - " + Generic.getTagValue(u.pose.tags, "subrace") +  "), ";
		else
			line = line + " (" + u.race.name +  "), ";
		
		
		line = line + u.getGoldCost() + "g, " + u.getResCost(true) + "r, ";
		

		
		for(String str : u.slotmap.keySet())
		{
			if(u.getSlot(str) == null)
				continue;
			
			if(!u.getSlot(str).armor && !u.getSlot(str).id.equals("-1"))
				line = line + weapondb.GetValue(u.getSlot(str).id, "weapon_name") + ", ";

		}
		
		for(String str : u.slotmap.keySet())
		{
			if(u.getSlot(str) == null)
				continue;
			if(u.getSlot(str).armor && !u.getSlot(str).id.equals("-1"))
				line = line + armordb.GetValue(u.getSlot(str).id, "armorname") + ", ";
		}
		
		
		if(u.getSlot("mount") != null)
		{
			String mountname = "";
			for(String tag : u.getSlot("mount").tags)
				if(tag.startsWith("animal"))
				{
					mountname =	Generic.getTagValue(u.getSlot("mount").tags, "animal");
				}
			line = line + NameGenerator.capitalize(mountname) + " mount, ";
		}
		
		
		
		line = line.substring(0, line.length() - 2) + ".";
		
		tw.println(line);
		
		// Commands
		
		// Magic things!
		int[] paths = new int[10];
		double rand = 0;
		List<String> randoms = new ArrayList<String>();
		

		boolean links = false;
		for(Filter f : u.appliedFilters)
		{
			if(f.name.equals("MAGICPICKS") || f.name.equals("PRIESTPICKS"))
			{

				for(Command c : f.getCommands())
				{
					if(c.command.equals("#magicskill"))
					{
						paths[Integer.parseInt(c.args.get(0).split(" ")[0])] += Integer.parseInt(c.args.get(0).split(" ")[1]);
					}
					else if(c.command.equals("#custommagic"))
					{
						for(String s : Generic.getListOfPathsInMask(Integer.parseInt(c.args.get(0).split(" ")[0])))
							if(!randoms.contains(s))
								randoms.add(s);
						paths[9] += Integer.parseInt(c.args.get(0).split(" ")[1]) / 100;
						rand += Double.parseDouble(c.args.get(0).split(" ")[1]) / 100;
						
						if(Double.parseDouble(c.args.get(0).split(" ")[1]) / 100 > 1)
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
			for(String s : randoms)
				magicstuff = magicstuff + s + ", ";
			
			if(links)
				magicstuff = magicstuff + "at least partially linked, ";
			
			magicstuff = magicstuff.substring(0, magicstuff.length() - 2) + ")";
		}
		magicstuff = magicstuff + ". ";
		

		if(!magicstuff.equals(". "))
		{
			magicstuff = magicstuff.trim();
			tw.println("--- " + magicstuff);
		}
		
		
		
		// Filters and item special things
		List<String> stuff = new ArrayList<String>();
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

		
		
		for(Filter f : u.appliedFilters)
		{
			String text = null;
			
			if(Generic.containsTag(f.tags, "description"))
				text = Generic.getTagValue(f.tags, "description");
			
			if(text == null)
				text = f.name;	
			
			if(text != null)
				stuff.add(text);
	
		}
		

		
		if(stuff.size() > 0)
			tw.println("--- " + writeAsList(stuff, false));
		
		stuff.clear();
		
		for(Item item : u.slotmap.values())
		{
			if(item == null)
				continue;
			
			for(String tag : item.tags)
			{

				
				List<String> args = Generic.parseArgs(tag);
				if(args.get(0).equals("description") && args.size() > 1)
				{
					stuff.add(args.get(1));
				}
			}
		}
		
		if(stuff.size() > 0)
			tw.println("--- " + writeAsList(stuff, false));
		
		
		return line;
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
