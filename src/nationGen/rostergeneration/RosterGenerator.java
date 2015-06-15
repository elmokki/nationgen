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
	private UnitGen unitGen = null;
	private ChanceIncHandler chandler = null;

	private List<Filter> unitTemplates = new ArrayList<Filter>();
	private int maxtemplates = 0;
	private ItemSet used = new ItemSet();
	private ItemSet exclusions = new ItemSet();
	
	
	private List<Unit> infantry = new ArrayList<Unit>();
	private List<Unit> ranged = new ArrayList<Unit>();
	private List<Unit> cavalry = new ArrayList<Unit>();
	private List<Unit> chariot = new ArrayList<Unit>();
	
	public RosterGenerator(NationGen g, Nation n)
	{
		nationGen = g;
		nation = n;
		this.r = new Random(n.random.nextInt());
		unitGen = new UnitGen(nationGen, nation);
		chandler = new ChanceIncHandler(nation);
	}
	
	private List<Unit> getTargetList(String roll)
	{
		if(roll.equals("ranged"))
			return ranged;
		else if(roll.equals("infantry"))
			return infantry;
		else if(roll.equals("mounted"))
			return cavalry;
		else if(roll.equals("chariot"))
			return chariot;
		
		return null;
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
		

		maxtemplates = r.nextInt(3) + 1; // 1 to 3;
		

			
		int cycles = 0;
		int incs = 1;
		
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
			
				
			
				Unit u = getNewUnit(race, roll);
				if(u != null)
				{
					
					
					// Add variants!
					if(r.nextDouble() > 0.3)
					{
						int amount = 2;
						if(getMaxVarieties(u) > 1)
						{
							amount += r.nextInt(this.getMaxVarieties(u) - 1);
						}
						else
							amount = 1;
						
						if(u.pose.roles.contains("mounted"))
							amount = 2;
						
						amount = Math.min(amount, max - units + 1);
						

						List<Unit> news = null;
						if(u.pose.roles.contains("mounted"))
							news = unitGen.varyCavalryWeapon(u, amount, "weapon", used, null, null);
						else
							news = unitGen.varyUnit(u, amount, "weapon", used, null, null);

						target.addAll(news);
						units += news.size();
					}
					else
					{
						target.add(u);
						units++;

					}
				}
				
				
			}
			

		}


		// Final stuff
		for(Unit u : infantry)
		{
			unitGen.equipOffhand(u, used, exclusions, null, false);
			unitGen.equipBonusWeapon(u, used, exclusions, null, false);
			unitGen.cleanUnit(u);
		}
		for(Unit u : cavalry)
		{
			unitGen.equipBonusWeapon(u, used, exclusions, null, false);
			unitGen.cleanUnit(u);
		}
		for(Unit u : chariot)
		{
			unitGen.cleanUnit(u);
		}
		for(Unit u : ranged)
		{
			unitGen.cleanUnit(u);
		}
		
		putToNation("ranged", sortToLists(ranged));
		putToNation("infantry", sortToLists(infantry));
		putToNation("mounted", sortToLists(cavalry));
		putToNation("chariot", sortToLists(chariot));
		

	
	}
	
	
	private Unit getNewUnit(Race race, String role)
	{
		Pose p = this.getSuitablePose(race, role);
		if(p == null)
			return null;
		
		Unit u = unitGen.generateUnit(race, p);
		
		
		// Add unit template to available templates
		if(unitTemplates.size() < maxtemplates)
		{
			List<Filter> possibleFilters = ChanceIncHandler.retrieveFilters("trooptemplates", "default_templates", nationGen.miscdef, u.pose, u.race);
			possibleFilters.removeAll(unitTemplates);
			possibleFilters = ChanceIncHandler.getValidUnitFilters(possibleFilters, u);
			
			Filter f = chandler.getRandom(possibleFilters, u);
			if(f != null)
			{
				u.appliedFilters.add(f);
				unitTemplates.add(f);
			}
		}
		
		
		// Remove elite and sacred items
		Filter tf = new Filter(nationGen);
		tf.name = Generic.capitalize(role) + " unit";
		
		boolean elite = false;
		boolean sacred = false;
		for(Filter f : u.appliedFilters)
		{
			if(f.tags.contains("alloweliteitems"))
				elite = true;
			if(f.tags.contains("allowsacreditems"))
				sacred = true;
		}
		
		if(!elite || !sacred)
		{

			if(!elite)
			{
				tf.themeincs.add("thisitemtag elite *0");
				tf.themeincs.add("thisitemtheme elite *0");
			}
			if(!sacred)
			{
				tf.themeincs.add("thisitemtag sacred *0");
				tf.themeincs.add("thisitemtheme sacred *0");

			}
			
		}	

		u.appliedFilters.add(tf);
		
	
		// Equip unit
		

		ItemSet fooexc = new ItemSet();
		List<Unit> others = getTargetList(role);
		for(Unit un : others)
			if(un.pose == u.pose)
				fooexc.add(u.getSlot("armor"));
		
		fooexc.addAll(exclusions);
		
		unitGen.armorUnit(u, used, fooexc, null, false);
		unitGen.armUnit(u, used, exclusions, null, false);

		addToUsed(u);
		


		
		return u;
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
	
	public void addToUsed(Unit unit)
	{
		
		for(Item i : unit.slotmap.values())
			if(!used.contains(i) && unit.getSlot("armor") != i)
				used.add(i);

		
		for(String r : unit.pose.roles)
			for(Pose p2 : unit.race.getPoses(r))
			{	
	
				if(p2.getItems("offhand") != null && unit.getSlot("offhand") != null && unit.getSlot("offhand").armor)
					for(Item i : p2.getItems("offhand").filterArmor(true))
						if(i.id.equals(unit.getSlot("offhand").id) && (!i.sprite.equals(unit.getSlot("offhand").sprite) && !i.name.equals(unit.getSlot("offhand").name)))
							{
								
								if(!exclusions.contains(i))
								{
									this.exclusions.add(i);
								}
							}
		
			}
		
	}
	
	
	private Pose getSuitablePose(Race race, String role)
	{
		Pose p = null;
		List<Pose> poses = race.getPoses(role);
		List<Pose> remove = new ArrayList<Pose>();
		
		for(Pose pose : poses)
		{
			if(Generic.containsTag(pose.tags, "primaryraceonly") && race != nation.races.get(0))
				remove.add(pose);
			
			List<Item> armors = new ArrayList<Item>();
			armors.addAll(pose.getItems("armor"));
			
			
			if(exclusions.filterSlot("armor").filterForPose(pose).containsAll(armors))
			{
				System.out.println(poses.size() + " vs " + exclusions.filterSlot("armor").filterForPose(pose) + " " + role + " " + race);
				remove.add(pose);
			}
			
			
		}
		poses.removeAll(remove);
		p = chandler.getRandom(poses);

		if(p == null)
			System.out.println("NO POSE FOR RACE " + race + " " + exclusions.filterSlot("armor").size() + " main race " + nation.races.get(0) + " role " + role);
		
		return p;
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
	
	
	private int getMaxVarieties(Unit u)
	{
		
		int maxvar = 3;
		if(u.pose.roles.contains("mounted"))
			maxvar = 2;
		if(u.pose.roles.contains("chariot"))
			maxvar = 1;
		
		for(Filter f : u.appliedFilters)
			if(Generic.getTagValue(f.tags, "maxvarieties") != null)
				maxvar = Integer.parseInt(Generic.getTagValue(f.tags, "maxvarieties"));
		
		if(Generic.getTagValue(u.getSlot("armor").tags, "maxvarieties") != null)
			maxvar = Math.min(maxvar, Integer.parseInt(Generic.getTagValue(u.getSlot("armor").tags, "maxvarieties")));
		
		if(u.getSlot("mount") != null && Generic.getTagValue(u.getSlot("mount").tags, "maxvarieties") != null)
			maxvar = Math.min(maxvar, Integer.parseInt(Generic.getTagValue(u.getSlot("mount").tags, "maxvarieties")));
		
		
		if(Generic.getTagValue(u.pose.tags, "maxvarieties") != null)
			maxvar = Math.min(maxvar, Integer.parseInt(Generic.getTagValue(u.pose.tags, "maxvarieties")));
		
		
		maxvar = Math.min(u.pose.getItems("weapon").possibleItems(), maxvar);
		
	
		return maxvar;

	}
	

	
	
}
