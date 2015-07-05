package nationGen.naming;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import com.elmokki.Generic;

import nationGen.items.Item;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.units.Unit;


public class TroopNamer {

	private Nation n;

	List<String> given = new ArrayList<String>();
	
	public TroopNamer(Nation n)
	{
		this.n = n;
	}
	
	public void execute() {
		nameNation(n);
	}
	


	private void nameNation(Nation n)
	{

		this.n = n;
		nameAfterArmor(n);
		
		// Give light/heavy prefixes and nationality prefix
		List<Unit> alltroops = n.combineTroopsToList("infantry");
		alltroops.addAll(n.combineTroopsToList("chariot"));
		alltroops.addAll(n.combineTroopsToList("mounted"));
		alltroops.addAll(n.combineTroopsToList("ranged"));
		
		NameGenerator.addHeavyLightPrefix(n.combineTroopsToList("infantry"));
		NameGenerator.addHeavyLightPrefix(n.combineTroopsToList("mounted"));
		NameGenerator.addHeavyLightPrefix(n.combineTroopsToList("ranged"));
		NameGenerator.addNationalPrefix(alltroops, n);
		
		nameCommanders(n);
		
	}
	
	
	private void nameCommanders(Nation n)
	{
		List<Unit> units = n.comlists.get("commanders");
		String[] basicnames = {"Commander", "Lord", "Castellan", "Sergeant", "Lieutenant"};
		String[] betternames = {"General", "Colonel", "Warlord", "Lord Commander", "Strategus"};
		List<String> used = new ArrayList<String>();;
		
		for(Unit u : units)
		{
			boolean bad = true;
			boolean unnamed = true;
			
			for(Command c : u.commands)
			{
				if(c.command.startsWith("#inspirational") || c.command.startsWith("#goodleader") || c.command.startsWith("#expertleader"))
					bad = false;
			}

			if(n.random.nextBoolean())
			{
				String name = null;
				unnamed = (null == (name = getWeaponBasedName(u, used, unnamed, true)));
				if(name != null)
					u.name.setType(name);
			}
			
			if(bad && unnamed)
				u.name.setType(basicnames[n.random.nextInt(basicnames.length)]);
			else if(unnamed)
				u.name.setType(betternames[n.random.nextInt(betternames.length)]);

			used.add(u.getName());
		}
		
		
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
	

	private void nameAfterArmor(Nation n)
	{
		List<String> used = new ArrayList<String>();
		List<String> wpnused = new ArrayList<String>();
		
		// Get non-weapon nameparts

		
		
		List<String> roles = new ArrayList<String>();
		roles.add("infantry");
		roles.add("mounted");
		roles.add("ranged");
		roles.add("chariot");
		
		
		for(String role : roles)
		{
			List<List<Unit>> unitlists = n.getListsOfType(role, false, true);
			NamePart defaultpart = new NamePart(n.nationGen);
			defaultpart = NameGenerator.getTemplateName(n, role);
			
			for(List<Unit> list : unitlists)
			{	
				if(list.size() == 0)
					continue;
				
				Name name = new Name();
				name.type = defaultpart;
				
				if(true)
				{
					String str = getArmorBasedName(list.get(0), used);
					
		
					
					if(str != null)
					{
						if(!used.contains(str))
							used.add(str);
						
						
						String namepart = "";
						List<String> args = Generic.parseArgs(str);
						
						if(args.size() == 2)
							namepart = args.get(1);
						else if(args.size() == 3)
							namepart = args.get(2);
						else
							System.out.println("Faulty naming line: " + str);
						
						
						
						NamePart part = new NamePart(n.nationGen);
						part.name = namepart;
						
						boolean canGive = canGive(str, "armor", unitlists);
						if(!canGive && given.contains(str))
							canGive = true;
						
						if(args.get(0).equals("name") && canGive)
						{
							given.add(str);
							name.type = part;
						}
						else if((args.get(0).equals("prefix") && canGive) || args.get(0).equals("guaranteedprefix"))
						{
							given.add(str);
							name.prefix = part;
						}
					}
				}
				
				
				for(Unit u : list)
				{
					if(u.tags.contains("auxillary"))
						continue;
					
					u.name = name.getCopy();
					
					boolean unnamed = u.name.type.name.equals("UNNAMED");

					String str = getWeaponBasedName(u, wpnused, unnamed, false);
			
			
		
					
					if(str != null)
					{
						if(!wpnused.contains(str))
							wpnused.add(str);
						
						String namepart = "";
						List<String> args = Generic.parseArgs(str);
						
						if(args.size() == 2)
							namepart = args.get(1);
						else if(args.size() == 3)
							namepart = args.get(2);
						else
							System.out.println("Faulty naming line: " + str);
						
						NamePart part = new NamePart(n.nationGen);
						part.name = namepart;
						
						boolean canGive = canGive(str, "weapon", unitlists);
						if(!canGive)
							canGive = canGive(str, "bonusweapon", unitlists);
						if(!canGive && given.contains(str))
							canGive = true;
						

						
						if(args.get(0).equals("name") && (canGive || unnamed))
						{
							given.add(str);
							u.name.type = part;
						}
						else if((args.get(0).equals("prefix") && (canGive || unnamed)) || args.get(0).equals("guaranteedprefix"))
						{
							given.add(str);
							u.name.prefix = part;
						}
						
				
				
					}
					
				
				}
				
				for(Unit u : list)
				{
					if(n.percentageOfRace(u.race) < 0.1 && u.race != n.races.get(0))
					{
						u.name.setType("Auxillary");
					}
				}
				
				
			}
		}
		

	}
	
	private boolean canGive(String line, String slot, List<List<Unit>> unitlists)
	{
		int all = 0;
		int count = 0;
		for(List<Unit> list : unitlists)
			for(Unit u : list)
			{
				if(u.getSlot(slot) != null && u.getSlot(slot).tags.contains(line))
					count++;
				all++;
			}
				
		
		double perc = (double)count/(double)all;
		return  n.random.nextDouble() < count * 0.1 + perc;
	}
	
	private String getArmorBasedName(Unit u, List<String> used)
	{
		List<String> prefixes = new ArrayList<String>();
		List<String> guaranteedprefixes = new ArrayList<String>();
		List<String> names = new ArrayList<String>();
		List<String> old = new ArrayList<String>();
		
		for(Item item : u.slotmap.values())
		{
			
			if(item != null)
			{
				
				if(item.slot.equals("weapon") || item.slot.equals("bonusweapon"))
					continue;
				

				for(String s : item.tags)
				{
					parseNames(s, u, used, guaranteedprefixes, prefixes, names, old);
				}
			}
		}
		for(String s : u.tags)
		{
			parseNames(s, u, used, guaranteedprefixes, prefixes, names, old);
		}
		
		Random r = n.random;
		boolean prefix = r.nextBoolean();
		
		if(guaranteedprefixes.size() > 0)
		{
			return guaranteedprefixes.get(r.nextInt(guaranteedprefixes.size()));
		}
		else if(old.size() > 0)
		{
			return old.get(r.nextInt(old.size()));
		}
		else if((prefix || names.size() == 0) && prefixes.size() > 0)
		{
			return prefixes.get(r.nextInt(prefixes.size()));
		}
		else if(names.size() > 0)
		{
			return names.get(r.nextInt(names.size()));
		}
		else
		{
			return null;
		}
		
	}
	
	private void parseNames(String s, Unit u, List<String> used, List<String> guaranteedprefixes,  List<String> prefixes,  List<String> names, List<String> old)
	{
		if(used.contains(s))
			old.add(s);
		
		List<String> args = Generic.parseArgs(s);

		if(args.get(0).equals("name"))
		{
			if(args.size() == 2)
			{
				names.add(s);
			}
			else if(args.size() > 2 && u.pose.roles.contains(args.get(1)))
			{
				names.add(s);
			}
			else if(args.size() < 2)
				System.out.println("Faulty naming line: " + s);
		}
		else if(args.get(0).equals("prefix"))
		{
			if(args.size() == 2)
				prefixes.add(s);
			else if(args.size() > 2 && u.pose.roles.contains(args.get(1)))
				prefixes.add(s);
			else if(args.size() < 2)
				System.out.println("Faulty naming line: " + s);
		}
		else if(args.get(0).equals("guaranteedprefix"))
		{
			if(args.size() == 2)
				guaranteedprefixes.add(s);
			else if(args.size() > 2 && u.pose.roles.contains(args.get(1)))
				guaranteedprefixes.add(s);	
			else if(args.size() < 2)
				System.out.println("Faulty naming line: " + s);
		}
	}

	private String getWeaponBasedName(Unit u, List<String> used, boolean unnamed, boolean isCommander)
	{
		List<String> prefixes = new ArrayList<String>();
		List<String> guaranteedprefixes = new ArrayList<String>();
		List<String> names = new ArrayList<String>();
		List<String> old = new ArrayList<String>();
		
		for(Item item : u.slotmap.values())
		{
			
			if(item != null)
			{
				if(!item.slot.equals("weapon") && !item.slot.equals("bonusweapon"))
					continue;
				

				
				for(String s : item.tags)
				{
					List<String> args = Generic.parseArgs(s);
					
					if(used.contains(s))
					{
						old.add(s);
					}
					else if(args.size() == 2 && used.contains(args.get(1)))
					{
						old.add(args.get(1));
					}
					else if(args.size() == 3 && used.contains(args.get(2)))
					{
						old.add(args.get(2));
					}
					
					if(args.get(0).equals("name"))
					{
						if(args.size() == 2 && !isCommander)
						{
							names.add(s);
						}
						else if(args.size() == 3 && args.get(1).equals("commander") && isCommander)
						{
							names.add(args.get(2));
						}
						else if(args.size() == 3 && u.pose.roles.contains(args.get(1)) && !isCommander)
						{
							names.add(s);
						}
					}
					else if(args.get(0).equals("prefix"))
					{
						if(args.size() == 2 && !isCommander)
							prefixes.add(s);
						else if(args.size() == 3 && args.get(1).equals("commander") && isCommander)
						{
							prefixes.add(args.get(2));
						}
						else if(args.size() == 3 && u.pose.roles.contains(args.get(1)) && !isCommander)
							prefixes.add(s);
					}
					else if(args.get(0).equals("guaranteedprefix"))
					{
						if(args.size() == 2 && !isCommander)
							guaranteedprefixes.add(s);
						else if(args.size() == 3 && args.get(1).equals("commander") && isCommander)
						{
							guaranteedprefixes.add(args.get(2));
						}
						else if(args.size() == 3 && u.pose.roles.contains(args.get(1)) && !isCommander)
							guaranteedprefixes.add(s);	
					}
				}
			}
		}
		
		Random r = n.random;
		boolean prefix = r.nextBoolean();
		
		List<String> newold = new ArrayList<String>();
		for(String str : old)
		{
			List<String> args = Generic.parseArgs(str);
			if(args.size() > 2 && u.pose.roles.contains(args.get(1)) && !isCommander)
				newold.add(str);
			else if(args.size() > 2 && args.get(1).equals("commander") && isCommander)
				newold.add(args.get(2));
		}
		
		if(guaranteedprefixes.size() > 0 && !unnamed)
		{
			return guaranteedprefixes.get(r.nextInt(guaranteedprefixes.size()));
		}
		else if(newold.size() > 0 && !unnamed)
		{
			return newold.get(r.nextInt(newold.size()));
		}
		else if((prefix || names.size() == 0) && prefixes.size() > 0 && !unnamed && r.nextBoolean())
		{
			return prefixes.get(r.nextInt(prefixes.size()));
		}
		else if(names.size() > 0 && (r.nextBoolean() || unnamed))
		{
			return names.get(r.nextInt(names.size()));
		}
		else
		{
			return null;
		}
		
	}





}
