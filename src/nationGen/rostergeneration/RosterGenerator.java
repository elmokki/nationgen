package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;
















import nationGen.NationGen;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.units.Unit;



public class RosterGenerator {
	
	NationGen nationGen;
	Nation nation;
	private Random r;
	private ChanceIncHandler chandler = null;


	
	private List<Unit> infantry = new ArrayList<Unit>();
	private List<Unit> ranged = new ArrayList<Unit>();
	private List<Unit> cavalry = new ArrayList<Unit>();
	private List<Unit> chariot = new ArrayList<Unit>();
	
	public RosterGenerator(NationGen g, Nation n)
	{
		nationGen = g;
		nation = n;
		this.r = new Random(n.random.nextInt());
		chandler = new ChanceIncHandler(nation);
	}
	

	
	public void execute()
	{

	
		
		Race primary = nation.races.get(0);
		Race secondary = nation.races.get(1);
		
		int max = 6 + r.nextInt(5); // 6-10
		int units = 0;
		
		double bonussecchance = 1;
		if(Generic.containsTag(primary.tags, "secondaryracetroopmod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(primary.tags, "secondaryracetroopmod"));
		if(Generic.containsTag(secondary.tags, "primaryracetroopmod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(secondary.tags, "primaryracetroopmod"));
		
		double secaffinity = r.nextDouble() * bonussecchance;
		int secs = 0;
		
		if(secaffinity > 0.5)
			max = Math.min(10, max + 2);
		else if(secaffinity > 0.3)
			max = Math.min(10, max + 1);
		
		if(secaffinity < 0.5 && r.nextDouble() < 0.75)
			secaffinity = 0;
		
		
		
		
		double secamount = max * 0.5;
	
		
		
		double chances[] = {0.3, 1, 0.5, 0};
		int maxamounts[] = {2, 8, 4, 2};  

		maxamounts[3] = r.nextInt(3);
		maxamounts[0] = (int)Math.round(Math.max(1, max * 0.2));
		

		

			
		int cycles = 0;
		int incs = 1;
		
		TroopGenerator tgen = new TroopGenerator(nationGen, nation);

		while(units < max)
		{
			
			Race race = null;
			if(r.nextDouble() < secaffinity && secs < secamount) 
			{
				race = secondary;
				secs++;
			}
			else
				race = primary;
			
			cycles++;
			if(cycles > 100 * incs)
			{
				incs++;
				for(int i = 0; i < maxamounts.length; i++)
					maxamounts[i]++;
			}
		
			chances = getChances(race);
			String roll = rollRole(chances[2], chances[1], chances[0], chances[3]);


			

			List<Unit> target = null;
			if(roll.equals("ranged") && maxamounts[0] > ranged.size())
				target = ranged;
			else if(roll.equals("infantry") && maxamounts[1] > infantry.size())
				target = infantry;
			else if(roll.equals("mounted") && maxamounts[2] > cavalry.size())
				target = cavalry;
			else if(roll.equals("chariot") && maxamounts[3] > chariot.size())
				target = chariot;
						
			
			
			if(race != null && target != null && race.hasRole(roll))
			{
			
				Unit u = tgen.generateUnit(roll, race, 3);
				if(u != null)
				{
					target.add(u);
					units++;
				}
	
				
			}
			

		}



		
		putToNation("ranged", sortToLists(ranged));
		putToNation("infantry", sortToLists(infantry));
		putToNation("mounted", sortToLists(cavalry));
		putToNation("chariot", sortToLists(chariot));
		

	
	}
	
	

	
	private double[] getChances(Race race)
	{
		double chances[] = {0.25, 1, 0.25, 0.125};
		String[] slots = {"ranged", "infantry", "cavalry", "chariot"};
		for(String tag : race.tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).contains("generationchance"))
			{
				String slot = args.get(1);
				int index = -1;
				for(int i = 0; i < slots.length; i++)
				{
					if(slots[i].equals(slot))
						index = i;
				}
				
				if(index != -1)
				{
					chances[index] = Double.parseDouble(args.get(2));
				}
			}
		}
		
		
		return chances;
	}
	

	private String rollRole(double cavshare, double infantryshare, double rangedshare, double chariotshare)
	{
		
		double random = r.nextDouble() * (cavshare + infantryshare + rangedshare + chariotshare);
		
		if(random < cavshare)
		{
			return "mounted";
		}
		else if(random < cavshare + infantryshare)
		{
			return "infantry";
		}
		else if(random < cavshare + infantryshare + rangedshare)
		{
			return "ranged";
		}
		else if(random < cavshare + infantryshare + rangedshare + chariotshare)
		{
			return "chariot";
		}
		
		return "";
	}
	
	
	private void putToNation(String role, List<List<Unit>> lists)
	{
		int i = 0;
		for(List<Unit> list : lists)
		{
			i++;
			if(nation.unitlists.get(role + "-" + i) == null)
				nation.unitlists.put(role + "-" + i, new ArrayList<Unit>());
			
			nation.unitlists.get(role + "-" + i).addAll(list);
			
	
		}	
	}

	private List<List<Unit>> sortToLists(List<Unit> templates)
	{
		List<List<Unit>> lists = this.sortToListsByBasesprite(templates);
		

		
		List<List<Unit>> newlist = new ArrayList<List<Unit>>();
		for(List<Unit> mlist : lists)
		{
			newlist.addAll(this.sortByArmor(mlist));
		}
		

		return newlist;
	}
	
	
	private List<Unit> sortByGcost(List<Unit> templates)
	{
		List<Unit> newlist = new ArrayList<Unit>();
		
		newlist.add(templates.get(0));
		templates.remove(0);
		
		while(templates.size() > 0)
		{
			int gcost = templates.get(0).getGoldCost();
			for(int i = newlist.size() - 1; i >= 0; i--)
			{
				if(gcost > newlist.get(i).getGoldCost())
				{
					newlist.add(templates.get(0));
					templates.remove(0);
					break;
				}
				else if(i == 0)
				{
					newlist.add(0, templates.get(0));
					templates.remove(0);
				}
			}
			
		}

		
		return newlist;
	}
	
	private List<List<Unit>> sortByArmor(List<Unit> templates)
	{
		List<List<Unit>> finallist = new ArrayList<List<Unit>>();
		
		List<Item> allArmor = new ArrayList<Item>();
		for(Unit u : templates)
			if(!allArmor.contains(u.getSlot("armor")))
				allArmor.add(u.getSlot("armor"));
		
		if(allArmor.size() == 0 || (allArmor.size() == 1 && allArmor.get(0) == null))
		{
			finallist.add(templates);
			return finallist;
		}

		while(templates.size() > 0)
		{
			String lowestID = allArmor.get(0).id;
			int lowestProt = nationGen.armordb.GetInteger("" + lowestID, "body");
			for(Item armor : allArmor)
				if(nationGen.armordb.GetInteger(armor.id, "body") < lowestProt)
				{
					lowestID = armor.id;	
					lowestProt = nationGen.armordb.GetInteger("" + lowestID, "body");
				}
			
			List<Item> foobarArmor = new ArrayList<Item>();
			for(Item armor : allArmor)
				if(armor.id.equals(""+ lowestID))
					foobarArmor.add(armor);
			allArmor.removeAll(foobarArmor);
			
			List<Unit> newlist = new ArrayList<Unit>();
			for(Unit u : templates)
				if(u.getSlot("armor").id.equals(lowestID))
					newlist.add(u);

			templates.removeAll(newlist);
			finallist.add(sortByGcost(newlist));
			//System.out.println("Removing all units with " + nationGen.armordb.GetValue(lowestID, "armorname") + ", #" + newlist.size() + ". " + templates.size() + " remain.");
		}

		return finallist;
	}	
	

	

	
	private List<List<Unit>> sortToListsByBasesprite(List<Unit> templates)
	{
		List<List<Unit>> troops = new ArrayList<List<Unit>>();
		
		// Sort to lists!
		List<Item> allArmor = new ArrayList<Item>();
		for(Unit u : templates)
			if(!allArmor.contains(u.getSlot("basesprite")))
				allArmor.add(u.getSlot("basesprite"));
		
		

		while(templates.size() > 0)
		{
			int lowestHP = this.getHP(templates.get(0));
			int prio = 5; 
					
			for(Unit u : templates)
				if(this.getHP(u) < lowestHP)
				{
					lowestHP = this.getHP(u);
				}
				else if(this.getHP(u) == lowestHP)
				{
					if(getPrio(u) > prio)
					{
						int newprio = getPrio(u);
						prio = newprio;
						lowestHP = this.getHP(u);
					}
				}
			
			List<Unit> newlist = new ArrayList<Unit>();
			for(Unit u : templates)
				if(getHP(u) <= lowestHP+2 && getPrio(u) == prio)
					newlist.add(u);

			templates.removeAll(newlist);
			troops.add(newlist);
			//System.out.println("Removing all units with " + nationGen.armordb.GetValue(lowestID, "armorname") + ", #" + newlist.size() + ". " + templates.size() + " remain.");
		}

		
		
		return troops;
	}
	
	
	
	/**
	 * Returns unit hp, only basesprite and race count.
	 * @param u
	 * @return
	 */
	private int getHP(Unit u)
	{
		int hp = 0;
		for(Command c : u.race.unitcommands)
			if(c.command.equals("#hp"))
				hp += Integer.parseInt(c.args.get(0));
		
		for(Command c : u.getSlot("basesprite").commands)
			if(c.command.equals("#hp"))
			{
				String arg = c.args.get(0);
				if(c.args.get(0).startsWith("+"))
					arg = arg.substring(1);
				
				hp += Integer.parseInt(arg);
			}
		
		if(hp > 0)
			return hp;
		else
			return 10;
	}

	
	private int getPrio(Unit u)
	{
		int prio = 5;
		for(String str : u.getSlot("basesprite").tags)
		{
			if(str.startsWith("basespritepriority"))
			{
				prio = Integer.parseInt(str.split(" ")[1]);
				
		

			}
		}
		return prio;
	}
	


	
	
}
