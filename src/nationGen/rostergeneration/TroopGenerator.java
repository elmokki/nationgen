package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;







import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.AbilityTemplate;
import nationGen.entities.Entity;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.misc.ChanceIncHandler;
import nationGen.misc.Command;
import nationGen.misc.ItemSet;
import nationGen.nation.Nation;
import nationGen.units.Unit;

public class TroopGenerator {
	public NationGen nationGen;
	public Nation nation;
	public UnitGen unitGen;
	public ItemSet used = new ItemSet();
	public ItemSet exclusions = new ItemSet();
	private double bonusrangedness = 0;
	private double dwchance = 0;
	
	private List<Template> templates = new ArrayList<Template>();
	protected ChanceIncHandler chandler;
	
	class Template
	{
		Item armor;
		Unit template;
		Race race;
		String role;
		Pose pose;
		int maxvar;
		boolean canBeSecondary;				// Flag to denote units that can't be allied auxiliaries
		int dws = 0;
		ItemSet weapons = new ItemSet();
		ItemSet bonusweapons = new ItemSet();
		AbilityTemplate abilitytemplate;
		
		public Template(Item armor, Race race, Unit template, String role, Pose pose, AbilityTemplate at)
		{
			this.armor = armor;
			this.race = race;
			this.template = template;
			this.role = role;
			this.pose = pose;
			this.maxvar = 3;
			this.canBeSecondary = true;
			this.abilitytemplate = at;
			
			if(at == null)
				abilitytemplate = new AbilityTemplate(race.nationGen);

				
			if(Generic.getTagValue(abilitytemplate.tags, "maxvarieties") != null)
				maxvar = Integer.parseInt(Generic.getTagValue(abilitytemplate.tags, "maxvarieties"));
			else if(Generic.getTagValue(armor.tags, "maxvarieties") != null)
				maxvar = Integer.parseInt(Generic.getTagValue(armor.tags, "maxvarieties"));
			else if(template.getSlot("mount") != null && Generic.getTagValue(template.getSlot("mount").tags, "maxvarieties") != null)
				maxvar = Integer.parseInt(Generic.getTagValue(template.getSlot("mount").tags, "maxvarieties"));
			else if(Generic.getTagValue(pose.tags, "maxvarieties") != null)
				maxvar = Integer.parseInt(Generic.getTagValue(pose.tags, "maxvarieties"));
			
			this.maxvar = Math.min(pose.getItems("weapon").possibleItems(), maxvar);
			
			
			if(role.equals("mounted"))
				this.maxvar = Math.min(getCavVarieties(pose), maxvar);

			if(Generic.getTagValue(pose.tags, "primaryraceonly") != null)
				this.canBeSecondary = false;

		}
	}
	

	
	public TroopGenerator(NationGen g, Nation n)
	{
		nationGen = g;
		nation = n;
		unitGen = new UnitGen(g, n.random);
		bonusrangedness = nation.random.nextDouble();
		dwchance = Math.max(0, (nation.random.nextDouble() - 0.2) / 8);
		chandler = new ChanceIncHandler(nation);
	}
	
	private int getCavVarieties(Pose p)
	{
		int stuff = 0;
		
		ItemSet tempweps = new ItemSet();
		ItemSet removethese = new ItemSet();
		tempweps.addAll(p.getItems("weapon"));
		for(Item i : tempweps)
		{
			if(i.id.equals("357") || i.tags.contains("lightlance"))
			{
				removethese.add(i);
				stuff = 1;
			}
		}
		tempweps.removeAll(removethese);
		
		boolean has1h = has1H(tempweps);
		if(has1h)
			stuff++;
		boolean has2h = (has2H(tempweps));
		if(has2h)
			stuff++;
	
		
		return stuff;
	}
	
	
	public Unit copyUnit(Unit u)
	{
		Unit unit = unitGen.generateUnit(u.race, u.pose);
		
		// Copy unit
		for(String slot : u.slotmap.keySet())
		{
			unit.setSlot(slot, u.getSlot(slot));
		}
		unit.tags.addAll(u.tags);
		unit.commands.addAll(u.commands);
		unit.appliedFilters.addAll(u.appliedFilters);
		return unit;
	}
	
	/**
	 * Generates units
	 * @param role
	 * @param race
	 * @param amount
	 * @param minprot
	 * @param maxprot
	 * @param isPrimaryRace
	 * @return
	 */
	public Unit generateUnit(String role, Race race, int amount, int minprot, int maxprot, int maxvarieties, boolean isPrimaryRace)
	{
		Unit unit = null;
		
		double skipchance = 0.2;
		Random random = nation.random;
		
		int cycles = 0;
		do
		{

			cycles++;
			if(cycles > 50)
			{
				break;
			}
			
	
			List<Template> temptemplates = new ArrayList<Template>();
			for(Template t : templates)
				if(t.race == race && (isPrimaryRace || t.canBeSecondary))
					temptemplates.add(t);
			
			if(temptemplates.size() == 0)
				temptemplates = templates;
			
			for(Template t : temptemplates)
			{			
				
				
				int occurances = 0;
				for(Template t2 : templates)
					if(t.armor.id.equals(t2.armor.id) && Math.abs(t.template.getHP() - t2.template.getHP()) < 4 && t.role.equals(t2.role) && t.template.getSlot("mount") == t2.template.getSlot("mount"))
					{
						occurances += t2.weapons.size();
					}
				
				if(occurances >= maxvarieties || random.nextDouble() <= skipchance || t.race != race || !t.pose.roles.contains(role) || occurances >= t.maxvar || t.role != role || (!isPrimaryRace && !t.canBeSecondary))
				{
					continue;
				}

				
				

				
				// Copy unit
				unit = this.copyUnit(t.template);
	
				/*
							
				unit = unitGen.generateUnit(race, t.template.pose);

				for(String slot : t.template.slotmap.keySet())
					unit.setSlot(slot, t.template.getSlot(slot));
				 */

				if(!role.equals("mounted"))
				{

					ItemSet possibleWeapons = t.template.pose.getItems("weapon").filterTag("elite", false).filterTag("sacred", false);
					if(possibleWeapons.possibleItems() - t.weapons.size() <= 0)
					{
						unit = null;
						continue;
					}
					else
					{
						ItemSet tempweps = new ItemSet();
						for(Template t2 : templates)
							if(t.armor.id.equals(t2.armor.id) && Math.abs(t.template.getHP() - t2.template.getHP()) < 3  && t.role.equals(t2.role))
							{
								tempweps.addAll(t.weapons);
							}
						
				
						
						
						
						Item weapon = this.getNewItem("weapon", role, race, t.template.pose, null, tempweps, false, t.template.getTotalProt(true), unit);
						unit.setSlot("weapon", weapon);
						t.weapons.add(weapon);
						
		
						if(role.equals("infantry") && nationGen.weapondb.GetInteger(unit.getSlot("weapon").id, "2h") == 0 && nationGen.weapondb.GetInteger(unit.getSlot("weapon").id, "lgt") < 3)
						{
							double local_dwchance = dwchance;
							if(Generic.getTagValue(unit.race.tags, "dwchancebonus") != null)
								local_dwchance = dwchance + Double.parseDouble(Generic.getTagValue(unit.race.tags, "dwchancebonus"));
							if(Generic.getTagValue(unit.pose.tags, "dwchancebonus") != null)
								local_dwchance = dwchance + Double.parseDouble(Generic.getTagValue(unit.pose.tags, "dwchancebonus"));
							if(Generic.getTagValue(unit.tags, "dwchancebonus") != null)
								local_dwchance = dwchance + Double.parseDouble(Generic.getTagValue(unit.tags, "dwchancebonus"));
							
							if(t.dws < 2 && nation.random.nextDouble() < local_dwchance)
								
							{
								t.dws++;
								this.equipOffhand(unit, role, race, t);
							}
						}
					
					}
				}
				else
				{

					boolean hasllance = false;
					
					ItemSet tempweps = new ItemSet();
					tempweps.addAll(t.weapons);
					ItemSet lances = new ItemSet();
					for(Item i : tempweps)
					{
						if(i.id.equals("357") || i.tags.contains("lightlance"))
						{
							lances.add(i);
							hasllance = true;
						}
					}
					tempweps.removeAll(lances);
					
					boolean has1h = has1H(tempweps);
					boolean has2h = (!has1H(tempweps) && tempweps.size() > 0);
					
					//System.out.println(has1h + ", " + has2h + ", " + hasllance);
					boolean done = false;
					int r = -1;
					while(!done)
					{

						r = random.nextInt(3);

						if(r == 0 && !has2h) // 2h
						{
							tempweps.addAll(t.pose.getItems("weapon").filterTag("elite", false).filterTag("sacred", false).filterDom3DB("2h", "0", true, nationGen.weapondb));
							for(Item i : t.pose.getItems("weapon").filterTag("elite", false).filterTag("sacred", false))
							{
								if(i.id.equals("357") || i.tags.contains("lightlance"))
									tempweps.add(i);
							}
							done = true;
						}
						else if(r == 1 && !has1h) // 1h
						{
							tempweps.addAll(t.pose.getItems("weapon").filterTag("elite", false).filterTag("sacred", false).filterDom3DB("2h", "1", true, nationGen.weapondb));
							for(Item i : t.pose.getItems("weapon").filterTag("elite", false).filterTag("sacred", false))
							{
								if(i.id.equals("357") || i.tags.contains("lightlance"))
									tempweps.add(i);
							}
							done = true;
						}
						else if(r == 2 && !hasllance) // lightlance
						{
							for(Item i : t.pose.getItems("weapon").filterTag("elite", false).filterTag("sacred", false))
							{
								if(!i.id.equals("357") && !i.tags.contains("lightlance"))
									tempweps.add(i);
							}
							done = true;
						}
					}
						
					Item weapon = this.getNewItem("weapon", role, race, unit.pose, tempweps, null, false, t.template.getTotalProt(true), unit);
					
		
					
					unit.setSlot("weapon", weapon);
					
	
					t.weapons.add(weapon);
					
					
					// Lance
					
					if(r == 1 && t.pose.getItems("lanceslot") != null)
					{
						int ap = 0;
						for(Command c : t.template.getSlot("mount").commands)
						{
							if(c.command.equals("#ap"))
								ap = Integer.parseInt(c.args.get(0));
						}
						
						int lancelimit = 5 + random.nextInt(25);
						if(nation.random.nextDouble() > 0.75)
							lancelimit = lancelimit * 2;

						boolean getsLance = (ap >= lancelimit);
						//System.out.println(ap + " vs " + lancelimit + " -> " + getsLance);
						
						if(getsLance && t.pose.getItems("lanceslot").size() > 0)
						{
							tempweps.clear();
							for(Item i : t.pose.getItems("lanceslot"))
							{
								if(!i.id.equals("4") && !i.tags.contains("lance"))
									tempweps.add(i);
							}
							
				
							Item lance = this.getNewItem("lanceslot", role, race, t.pose, tempweps, false, unit);
							unit.setSlot("lanceslot", lance);
						}
						
						
						}
						

					
						
					}
				
				
					// Bonusweapon
					
					this.equipBonusWeapon(unit, role, race, t);
					
				}
			
		

			// Add new armor!
			if(unit == null)
			{
				Item armor = null;
				Unit u = null;
				while(u == null || armor == null)
				{
	
					ItemSet armors = new ItemSet();
					ItemSet oldarmors = new ItemSet();
	
					
					for(Template t : templates)
					{
						if(t.role.equals(role))
							armors.add(t.armor);
					
						
						int occurances = 0;
						for(Template t2 : templates)
							if(t.armor.id.equals(t2.armor.id) && Math.abs(t.template.getHP() - t2.template.getHP()) < 3 && t.role.equals(t2.role))
							{
								occurances += t2.weapons.size();
							}
						if(occurances < t.maxvar)
							oldarmors.add(t.armor);
							
					}
					
				
					if(oldarmors.size() > 0 && nation.random.nextDouble() < 0.1)
					{
						armor = Entity.getRandom(nation.random, chandler.handleChanceIncs(oldarmors));
						List<Pose> usedPoses = new ArrayList<Pose>();
						for(Template t : templates)
							if(t.armor.id.equals(armor.id) && t.role.equals(role))
								usedPoses.add(t.pose);
					
						List<Pose> possibles = race.getPoses(role);
						possibles.removeAll(usedPoses);
						

						if(possibles.size() > 0)
						{
							Pose p = Entity.getRandom(nation.random, chandler.handleChanceIncs(possibles));
							armor = unitGen.getBestMatchForSlot(armor, p, "armor");
						}
						else
							armor = null;
					}


					if(armor == null)
					{

						Pose p = Entity.getRandom(nation.random, chandler.handleChanceIncs(race.getPoses(role)));
						armor = this.getNewItem("armor", role, race, p, armors, false);
	
						if(armor == null)
							armor = this.getNewItem("armor", role, race, armors, false);
					}

					if(armor == null)
						System.out.println("NULL ARMOR?!");
					
	
					Pose p = Entity.getRandom(random, chandler.handleChanceIncs(getPosesWith(race, role, armor)));
					
					
					// Make sure #maxunits is taken into account
					int maxunits = 100;
					if(Generic.getTagValue(p.tags, "maxunits") != null)
					{
						
						maxunits = Integer.parseInt(Generic.getTagValue(p.tags, "maxunits"));
					}	
					
					int count = 0;
					for(Template t : this.templates)
						if(t.pose.equals(p))
							count++;
					
					if(count > maxunits)
					{
						armor = null;
						break;			// EA20150529: If we continue here, we're not going to terminate
						//continue;
					}
			
					// Generate unit!
						

					u = unitGen.generateUnit(race, p);
					u.setSlot("armor", armor);
					u = unitGen.equipUnit(u, used, exclusions, null, false);

					if(u == null)
						armor = null;
				}
				
				if(u == null || armor == null) // EA20150529: The only case where this should occur is when #maxunits has been exceeded
					break;
				
				AbilityTemplate at = null;
				List<AbilityTemplate> atlist = new ArrayList<AbilityTemplate>();
				atlist.addAll(u.pose.templates);
				if(atlist.size() == 0 && nationGen.templates.get("default_" + role + "templates") != null)
					atlist.addAll(nationGen.templates.get("default_" + role + "templates"));
				if(atlist.size() > 0)
					at = Entity.getRandom(random, chandler.handleChanceIncs(u, atlist));
				
				
				
				if(at != null && ChanceIncHandler.canAdd(u, at))
					at.apply(u, nation);
				
				
				Template t = new Template(armor, race, u, role, u.pose, at);



				templates.add(t);
				
				// Exclude similar shield/armor
				for(Pose p2 : this.getPossiblePoses(role, race, minprot, maxprot))
				{	

					if(p2.getItems("offhand") != null && u.getSlot("offhand") != null && u.getSlot("offhand").armor)
						for(Item i : p2.getItems("offhand").filterArmor(true))
							if(i.id.equals(u.getSlot("offhand").id) && (!i.sprite.equals(u.getSlot("offhand").sprite) && !i.name.equals(u.getSlot("offhand").name)))
								{
									
									if(!exclusions.contains(i))
									{
										this.exclusions.add(i);
									}
								}
			
				}
				

				
			}

	
		} while(unit == null);
		
		if(unit != null)
		{
			// Add everything to used;
			addToUsed(unit);
			
			this.handleExtraGeneration(unit, role);
			this.cleanUnit(unit);
	
		
			//System.out.println("CREATED A " + role + ": " + unit.getSlot("armor").name + " and " + unit.getSlot("weapon").name + " / " + unit.pose.roles + " | " + unit.pose.name);
			//if(unit.getSlot("hands") != null)
			//	System.out.println("DPEDPER: " + unit.getSlot("hands").name + " " + unit.getSlot("basesprite").name);
		}
		return unit;
	}
	


	public void addToUsed(Unit unit)
	{
		
		for(Item i : unit.slotmap.values())
			if(!used.contains(i))
				used.add(i);
		
	}

	private void equipOffhand(Unit u, String role, Race race, Template t)
	{
		ItemSet stuff = new ItemSet();
		
		if(u.pose.getItems("offhand") == null)
			return;
		
		int rolls = 0;
		while(stuff.possibleItems() == 0 && rolls < 20)
		{

			rolls++;
			double roll = nation.random.nextDouble();
			if(roll < 0.66)
			{
				stuff = u.pose.getItems("offhand").filterArmor(false);
			}
			else
			{
				Item offhand = u.pose.getItems("offhand").filterArmor(false).getItemWithID(u.getSlot("weapon").id, "offhand");
				if(offhand != null)
					stuff.add(offhand);
			}

		}
		
		if(stuff.possibleItems() > 0)
		{
			Item wep = Entity.getRandom(nation.random, chandler.handleChanceIncs(u, stuff));
			u.setSlot("offhand", wep);
		}
		
	}
	
	private void equipBonusWeapon(Unit u, String role, Race race, Template t)
	{
		if(role.equals("ranged"))
			return;
		
		
		Random random = nation.random;
		ItemSet bonuses = used.filterSlot("bonusweapon").filterForPose(t.pose);
		if(bonuses.possibleItems() < 1 || random.nextDouble() < 0.5)
		{
			bonuses.add(this.getNewItem("bonusweapon", role, race, t.pose, bonuses, false, u));
		}
		
		bonuses = bonuses.filterForPose(t.pose);
		
		Item bonusweapon = Entity.getRandom(random, chandler.handleChanceIncs(u, bonuses));
		if(bonusweapon == null)
			return;
		
		// Tieruniqueness.
		if(bonusweapon.tags.contains("tierunique"))
		{
			for(Template t2 : templates)
			{
				if(t2.bonusweapons.getItemWithID(bonusweapon.id, "bonusweapon") != null && t2.armor.id.equals(u.getSlot("armor").id))				
				{
					return;
				}
			}
		}
		
		// Prot level
		int maxprot = 100;
		int minprot = 0;
		int totalprot = u.getTotalProt(false);
		

		if(Generic.containsTag(u.race.tags, "zeroarmor"))
			totalprot -= Integer.parseInt(Generic.getTagValue(u.race.tags, "zeroarmor"));
		if(Generic.containsTag(bonusweapon.tags, "maxprot"))
			maxprot = Integer.parseInt(Generic.getTagValue(bonusweapon.tags, "maxprot"));
		if(Generic.containsTag(bonusweapon.tags, "minprot"))
			minprot = Integer.parseInt(Generic.getTagValue(bonusweapon.tags, "minprot"));
		totalprot = Math.max(0, totalprot);
		
		if(totalprot < minprot || totalprot > maxprot)
			return;
		
		

		double local_bwchance = 0;
		if(Generic.getTagValue(u.race.tags, "bonusweaponchancebonus") != null)
			local_bwchance = local_bwchance + Double.parseDouble(Generic.getTagValue(u.race.tags, "bonusweaponchancebonus"));
		if(Generic.getTagValue(u.pose.tags, "bonusweaponchancebonus") != null)
			local_bwchance = local_bwchance + Double.parseDouble(Generic.getTagValue(u.pose.tags, "bonusweaponchancebonus"));
		if(Generic.getTagValue(u.tags, "bonusweaponchancebonus") != null)
			local_bwchance = local_bwchance + Double.parseDouble(Generic.getTagValue(u.tags, "bonusweaponchancebonus"));
		
		double chance = bonusrangedness + local_bwchance;


		
		double rescost = u.getResCost(false);
		
		rescost += 4 * nationGen.weapondb.GetInteger(u.getSlot("weapon").id, "res");
		if(nationGen.weapondb.GetValue(u.getSlot("weapon").id, "2h").equals("1"))
			rescost += 2 * nationGen.weapondb.GetInteger(u.getSlot("weapon").id, "res");


		
		if(u.getSlot("offhand") != null && u.getSlot("offhand").armor)
			rescost += 4 * nationGen.armordb.GetInteger(u.getSlot("offhand").id, "res");
		else if(u.getSlot("offhand") != null)
			rescost += 4 * nationGen.weapondb.GetInteger(u.getSlot("offhand").id, "res");
		
		if(nationGen.weapondb.GetInteger(u.getSlot("weapon").id, "dmg") <= 4)
			rescost *= 0.75;
		
		if(role.equals("mounted")) // +15% res cost for calculations for cavalry
			rescost = rescost * 1.15;
		if(u.getSlot("lanceslot") != null) // +15% if there already is a lance
			rescost = rescost * 1.15;

		
		if((rescost - 6) / 32 < chance)
		{
			u.setSlot("bonusweapon", bonusweapon);
			used.add(bonusweapon);
			t.bonusweapons.add(bonusweapon);
		}
		
	}

	protected void cleanUnit(Unit u)
	{
		// TODO: Handle more than one hand \:D/
		Item weapon = u.getSlot("weapon");
		boolean twohand = nationGen.weapondb.GetValue(weapon.id, "2h").equals("1");
		if(twohand)
			u.setSlot("offhand", null);
	}
	
	/**
	 * Finds possible poses for certain role/race with certain prot range
	 * @param role
	 * @param race
	 * @param minprot
	 * @param maxprot
	 * @return
	 */
	private List<Pose> getPossiblePoses(String role, Race race, int minprot, int maxprot)
	{
		// Search for poses with suitable armor
		List<Pose> possiblePoses = new ArrayList<Pose>();
		for(Pose p : race.poses)
		{
			if(!p.roles.contains(role))
				continue;
			
			boolean found = false;

			for(Item i : p.getItems("armor"))
			{
				int prot = nationGen.armordb.GetInteger(i.id, "body");
				if(prot <= maxprot && prot >= minprot)
					found = true;
			}
			
			if(found)
				possiblePoses.add(p);
		}
		
		return possiblePoses;
	}
	
	
	protected void handleExtraGeneration(Unit u, String role)
	{
		List<String> tags = new ArrayList<String>();
		tags.addAll(u.pose.tags);
		tags.addAll(u.race.tags);
		for(Item i : u.slotmap.values())
			if(i != null)
				tags.addAll(i.tags);
		
		for(String tag : tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("generateitem"))
			{
				String slot = args.get(2);
				double chance = Double.parseDouble(args.get(1));
				if(nation.random.nextDouble() < chance)
				{
					Item item = getNewItem(slot, role, u.race, u.pose, null, false, u);
					u.setSlot(slot, item);
					if(used != null && !used.contains(item))
						used.add(item);
				}
			}
		}
	}
	
	
	

	
	public Item getNewItem(String slot, String role, Race race, Pose p, ItemSet used, boolean elites, Unit u) {
		return this.getNewItem(slot, role, race, p, null, used, elites, -1);
	}
	
	public Item getNewItem(String slot, String role, Race race, ItemSet used, boolean elites, int prot, Unit u) {
		return this.getNewItem(slot, role, race, null, null, used, elites, prot);
	}
	
	public Item getNewItem(String slot, String role, Race race, ItemSet used, boolean elites, Unit u)
	{
		return this.getNewItem(slot, role, race, null, null, used, elites, -1, u);
	}
	
	public Item getNewItem(String slot, String role, Race race, ItemSet used, boolean elites)
	{
		return this.getNewItem(slot, role, race, null, null, used, elites, -1);
	}
	
	
	public Item getNewItem(String slot, String role, Race race, Pose p, ItemSet used, boolean elites)
	{
		return this.getNewItem(slot, role, race, p, p.getItems(slot), used, elites, -1);
	}
	
	
	public Item getNewItem(String slot, String role, Race race, Pose p, ItemSet all, ItemSet used, boolean elites, int prot)
	{
		return getNewItem(slot, role, race, p, all, used, elites, prot, null);
	}
	
	public Item getNewItem(String slot, String role, Race race, Pose p, ItemSet all, ItemSet used, boolean elites, int prot, Unit u)
	{


		if(p != null && p.getItems(slot) == null)
			return null;
		
		ItemSet old;
		if(p == null)
			old = this.used.filterSlot(slot).filterForRole(role, race);
		else
			old = this.used.filterSlot(slot).filterForPose(p);
		
		old.retainAll(race.getItems(slot, role).filterTag("elite", false).filterTag("sacred", false));
		
	
		if(all == null && p != null)
			all = p.getItems(slot).filterTag("elite", false).filterTag("sacred", false);

		
		if(p != null)
			all = p.getItems(slot).filterTag("elite", false).filterTag("sacred", false);

		
		old = old.filterImpossibleAdditions(used);
		all = all.filterImpossibleAdditions(used);
		
		if(p != null)
		{
			old = old.filterForPose(p);
			all = all.filterForPose(p);
		}
		

		if(slot.equals("weapon") && role.equals("infantry"))
		{
			if(!this.has1H(used) && nation.random.nextDouble() < 0.5)
			{
				if(has1H(all))
					all = all.filterDom3DB("2h", "0", true, nationGen.weapondb);
				if(has1H(old))
					old = old.filterDom3DB("2h", "0", true, nationGen.weapondb);
			}
		}
		

		
		ItemSet select = old;
		if(select.possibleItems() == 0)
		{
			select = all;
		}
		

		
		if(select.possibleItems() == 0)
		{
			if(p == null)
				select = race.getItems(slot, role).filterTag("elite", elites).filterTag("sacred", false);
			else
				select = p.getItems(slot).filterTag("elite", elites).filterTag("sacred", false);
		}
		

		if(prot >= 0)
		{
			if(select.filterMinMaxProt(prot).possibleItems() > 0)
				select = select.filterMinMaxProt(prot);
		}


		if(nation.random.nextDouble() < 0.5 && all.possibleItems() > 0)
			select = all;
		
		//System.out.println(all.possibleItems() + " / " + old.possibleItems() + " / " + select.possibleItems());

		
		if(u != null)
			return Entity.getRandom(nation.random, chandler.handleChanceIncs(u, select));
		else
			return Entity.getRandom(nation.random, chandler.handleChanceIncs(new Unit(nationGen, race, p), select));

	}
	
	public boolean foundInSet(Item item, ItemSet set)
	{
		for(Item i : set)
			if(i.id.equals(item.id))
				return true;
		
		return false;
	}
	
	public boolean has1H(ItemSet used)
	{
		used = used.filterSlot("weapon").filterDom3DB("rng", "0", true, nationGen.weapondb).filterDom3DB("2h", "0", true, nationGen.weapondb);
		return(used.size() > 0);
	}
	
	public boolean has2H(ItemSet used)
	{
		used = used.filterSlot("weapon").filterDom3DB("rng", "0", true, nationGen.weapondb).filterDom3DB("2h", "1", true, nationGen.weapondb);
		return(used.size() > 0);
	}
	
	public List<Pose> getPosesWith(Race race, String role, Item item)
	{
		List<Pose> poses = new ArrayList<Pose>();
		for(Pose pose : race.poses)
			if(pose.getItems(item.slot) != null && pose.roles.contains(role))
				for(Item item2 : pose.getItems(item.slot))
				{
					boolean ok = false;
					if(item2 == item)
						ok = true;
					
					/*
					if(item.id.equals("-1") && item2.id.equals("-1") && item.sprite.equals(item2.sprite))
						ok = true;
					else if(item.id.equals(item2.id) && item.name.equals(item2.name))
						ok = true;
					*/
					
					if(ok && !poses.contains(pose))
						poses.add(pose);
						
				}
			
		return poses;
	}
	

	public Item getMatchingItem(Pose p, String role, Item item)
	{
	

		List<Item> items = new ArrayList<Item>();
		
		for(Item item2 : p.getItems(item.slot))
		{
			boolean ok = false;
			if(item.id.equals("-1") && item2.id.equals("-1") && item.sprite.equals(item2.sprite))
				ok = true;
			else if(item.id.equals(item2.id) && item.name.equals(item2.name))
				ok = true;
			
			if(ok && !items.contains(item2))
				items.add(item2);
				
		}
		
		if(items.size() == 0)
			for(Item item2 : p.getItems(item.slot))
			{
				boolean ok = false;
				if(item.id.equals("-1") && item2.id.equals("-1") && item.sprite.equals(item2.sprite))
					ok = true;
				else if(item.id.equals(item2.id))
					ok = true;
				
				if(ok && !items.contains(item2))
					items.add(item2);
					
			}
			
		return Entity.getRandom(nation.random, chandler.handleChanceIncs(items));
	}
	
	
	

	
	



	
}
