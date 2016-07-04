package nationGen.rostergeneration;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.elmokki.Generic;




















import nationGen.NationGen;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.Command;
import nationGen.nation.Nation;
import nationGen.rostergeneration.montagtemplates.TroopMontagTemplate;
import nationGen.units.Unit;



public class RosterGenerator {
	
	NationGen nationGen;
	Nation nation;
	private Random r;


	
	private List<Unit> infantry = new ArrayList<Unit>();
	private List<Unit> ranged = new ArrayList<Unit>();
	private List<Unit> cavalry = new ArrayList<Unit>();
	private List<Unit> chariot = new ArrayList<Unit>();
	private TroopGenerator tgen;
	
	public RosterGenerator(NationGen g, Nation n)
	{
		nationGen = g;
		nation = n;
		this.r = new Random(n.random.nextInt());
		tgen = new TroopGenerator(nationGen, nation);

	}
	
	private boolean canRollNewUnit(Race race)
	{
		boolean allzero = true;
		for(double d : getChances(race))
		{
			if(d > 0)
				allzero = false;
		}
		return !allzero;
	}
	
	public void execute()
	{

	
		
		Race primary = nation.races.get(0);
		Race secondary = nation.races.get(1);
		
		int maxtroops = 10;
		int mintroops = 6;
		if(Generic.containsTag(primary.tags, "maxtroops"))
			maxtroops = Integer.parseInt(Generic.getTagValue(primary.tags, "maxtroops"));
		if(Generic.containsTag(primary.tags, "mintroops"))
			mintroops = Integer.parseInt(Generic.getTagValue(primary.tags, "mintroops"));
		

		int max = mintroops + r.nextInt(maxtroops - mintroops + 1); // 6-10 by default
		int units = 0;
		int secs = 0;
		int prims = 0;
		

		
		double bonussecchance = 0.5;
		if(Generic.containsTag(primary.tags, "secondaryracetroopmod"))
			bonussecchance += Double.parseDouble(Generic.getTagValue(primary.tags, "secondaryracetroopmod"));
		if(Generic.containsTag(secondary.tags, "primaryracetroopmod"))
			bonussecchance -= Double.parseDouble(Generic.getTagValue(secondary.tags, "primaryracetroopmod"));
		
		int maxprimaries = 100;
		
		double minsecaffinity = 0;
		if(Generic.containsTag(primary.tags, "minsecaffinity"))
			minsecaffinity = Double.parseDouble(Generic.getTagValue(primary.tags, "minsecaffinity"));
		
		double maxsecaffinity = 1000;
		if(Generic.containsTag(primary.tags, "maxsecaffinity"))
			maxsecaffinity = Double.parseDouble(Generic.getTagValue(primary.tags, "maxsecaffinity"));
		


		
		// Affinity
		double secaffinity = r.nextDouble() * bonussecchance;	
		

		double nosecaffinitychance = 0.35;
		if(Generic.containsTag(primary.tags, "nosecaffinitychance"))
			nosecaffinitychance = Double.parseDouble(Generic.getTagValue(primary.tags, "nosecaffinitychance"));
		
		if(r.nextDouble() < nosecaffinitychance)
			secaffinity = 0;
		
	
		secaffinity = Math.max(minsecaffinity, secaffinity);
		secaffinity = Math.min(maxsecaffinity, secaffinity);


		// Amount

		

		// Primary amounts
		if(Generic.containsTag(primary.tags, "minsecondaryracetroops"))
			maxprimaries = max - Integer.parseInt(Generic.getTagValue(primary.tags, "minsecondaryracetroops"));
		if(Generic.containsTag(primary.tags, "minsecondaryracetroopshare"))
			maxprimaries = max - (int)Math.round((max * Double.parseDouble(Generic.getTagValue(primary.tags, "minsecondaryracetroopshare"))));
		

		
		if(Generic.containsTag(primary.tags, "maxprimaryracetroops"))
		{
			maxprimaries = Integer.parseInt(Generic.getTagValue(primary.tags, "maxprimaryracetroops"));
			
		}
		if(Generic.containsTag(primary.tags, "maxprimaryracetroopshare"))
			maxprimaries = (int)Math.round((max * Double.parseDouble(Generic.getTagValue(primary.tags, "maxprimaryracetroopshare"))));
		
		
		// Secondary amount
		double secamount = max * 0.3;
		if(r.nextDouble() < bonussecchance)
		{
			secamount+= max * 0.2;
			if(r.nextDouble() < bonussecchance/2)
				secamount+= max * 0.1;
		}
		
		
		// Amount adjust
		if(max - maxprimaries > 0)
			secamount = Math.max(max - maxprimaries, secamount);
		
		
		if(Generic.containsTag(secondary.tags, "maxthisracetroops_as_secondary"))
		{
			secamount = Math.min(secamount, Integer.parseInt(Generic.getTagValue(secondary.tags, "maxthisracetroops_as_secondary")));
		}
		
		if(secaffinity == 0)
			secamount = 0;

		// Max
		if(secaffinity > 0.5)
		{
			max = Math.min(10, max + 2);
			
		}
		else if(secaffinity > 0.3)
			max = Math.min(10, max + 1);
		

		
		if(secamount == 0)
			max = Math.min(max, maxprimaries);
		else
			max = (int) Math.min(max, maxprimaries + secamount);
	
		
		int maxamounts[] = {1, 8, 4, 2};  
		
		// Random chariot maximum
		maxamounts[3] = r.nextInt(3);
		
		// 1-2 ranged maximum
		maxamounts[0] = 1 + r.nextInt(2);  // 1-2
		
		

		int cycles = 0;
		int incs = 1;
		
		while(units < max)
		{

			Race race = null;
			if(prims < maxprimaries && r.nextDouble() > secaffinity)
			{
				race = primary;
			}
			else if(secs < secamount) 
			{
				race = secondary;
			}
			else if(prims < maxprimaries)
			{
				race = primary;
			}
	

			
			
			if(!canRollNewUnit(race))
			{
				if(race == primary)
				{
					if(canRollNewUnit(secondary))
						race = secondary;
					else
						break;
				}
				
				else if(race == secondary)
				{
					if(canRollNewUnit(primary))
						race = primary;
					else
						break;
				}
			}
			

			
			
			
			cycles++;
			if(cycles > 100 * incs)
			{
				incs++;
				for(int i = 0; i < maxamounts.length; i++)
					maxamounts[i]++;
				
				if(secaffinity > 0)
					secamount++;


			}
		
			
			
			
			int[] amounts = {cavalry.size(), infantry.size(), ranged.size(), chariot.size()};
			

			String roll = null;
			int rolls = 0;
			while(roll == null)
			{
				rolls++;
				
				roll = rollRole(getChances(race), maxamounts, amounts, race);
				if(!tgen.canGetMoreUnits(race, roll))
				{
					roll = null;
				}
				
				if(rolls > 100)
					break;
				
			}
			if(rolls > 100)
				break;

			List<Unit> target = null;
			if(roll.equals("ranged") && maxamounts[0] > ranged.size() && tgen.hasPosesWithoutMaxUnits(race, "ranged"))
				target = ranged;
			else if(roll.equals("infantry") && maxamounts[1] > infantry.size() && tgen.hasPosesWithoutMaxUnits(race, "infantry"))
				target = infantry;
			else if(roll.equals("mounted") && maxamounts[2] > cavalry.size() && tgen.hasPosesWithoutMaxUnits(race, "mounted"))
				target = cavalry;
			else if(roll.equals("chariot") && maxamounts[3] > chariot.size() && tgen.hasPosesWithoutMaxUnits(race, "chariot"))
				target = chariot;
			
			if(race != null && target != null && race.hasRole(roll))
			{
			
				Unit u = tgen.generateUnit(roll, race);

				if(u != null)
				{
					target.add(u);
					
					units++;
					
					if(race == primary)
						prims++;
					else if(race == secondary)
						secs++;

				}

				
			}

			

		}
		
		//System.out.println("Start " + ZonedDateTime.now().toLocalTime().truncatedTo(ChronoUnit.SECONDS));

		for(Unit unit : cavalry)
			if(tgen.unitGen.hasMontagPose(unit))
				tgen.unitGen.handleMontagUnits(unit, new TroopMontagTemplate(nationGen, nation), "montagtroops");
		
		for(Unit unit : chariot)
			if(tgen.unitGen.hasMontagPose(unit))
				tgen.unitGen.handleMontagUnits(unit, new TroopMontagTemplate(nationGen, nation), "montagtroops");
		
		for(Unit unit : ranged)
			if(tgen.unitGen.hasMontagPose(unit))
				tgen.unitGen.handleMontagUnits(unit, new TroopMontagTemplate(nationGen, nation), "montagtroops");
		

		for(Unit unit : infantry)
			if(tgen.unitGen.hasMontagPose(unit))
				tgen.unitGen.handleMontagUnits(unit, new TroopMontagTemplate(nationGen, nation), "montagtroops");
		
		//System.out.println("End " + ZonedDateTime.now().toLocalTime().truncatedTo(ChronoUnit.SECONDS));

		putToNation("ranged", sortToLists(ranged));
		putToNation("infantry", sortToLists(infantry));
		putToNation("mounted", sortToLists(cavalry));
		putToNation("chariot", sortToLists(chariot));
		
	
	
		
		tgen = null;
	
	}
	
	

	
	private double[] getChances(Race race)
	{
		double chances[] = {0.25, 1, 0.25, 0.125};
		String[] slots = {"ranged", "infantry", "mounted", "chariot"};
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
		
		for(int i = 0; i < slots.length; i++)
		{
			String role = slots[i];
			if(!tgen.canGetMoreUnits(race, role))
				chances[i] = 0;
		}
		
		
		for(int i = 0; i < slots.length; i++)
			if(!race.hasRole(slots[i]))
				chances[i] = 0;
				
	
		
		return chances;
	}
	

	private String rollRole(double[] chances, int[] maxamounts, int[] amounts, Race race)
	{
		
		double cavshare = chances[2];
		double infantryshare = chances[1];
		double rangedshare = chances[0];
		double chariotshare = chances[3];
		
		
		if(amounts[2] >= maxamounts[2])
			cavshare = 0;
		if(amounts[1] >= maxamounts[1])
			infantryshare = 0;
		if(amounts[0] >= maxamounts[0])
			rangedshare = 0;
		if(amounts[3] >= maxamounts[3])
			chariotshare = 0;
		
		
		
		// Tweak shares if primary race does not have something and secondary does
		Race primary = nation.races.get(0);
		Race secondary = nation.races.get(1);
		
		if(!primary.hasRole("ranged") && secondary.hasRole("ranged") && race == secondary)
			rangedshare *= 2;
		if(!primary.hasRole("mounted") && secondary.hasRole("mounted") && race == secondary)
			cavshare *= 2;	
		
		
		
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
		
		lists = null;
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
