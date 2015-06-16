package nationGen.rostergeneration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;











import com.elmokki.Generic;

import nationGen.NationGen;
import nationGen.entities.AbilityTemplate;
import nationGen.entities.Entity;
import nationGen.entities.Filter;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.entities.Theme;
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
	
	private List<Filter> unitTemplates = new ArrayList<Filter>();
	private int maxtemplates = 0;
	private int appliedtemplates = 0;
	
	private double skipchance = 0.2;
	private List<Template> templates = new ArrayList<Template>();
	protected ChanceIncHandler chandler;
	protected Random random;
	
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
		
		
		public Template(Item armor, Race race, Unit template, String role, Pose pose)
		{
			this.armor = armor;
			this.race = race;
			this.template = template;
			this.role = role;
			this.pose = pose;
			this.maxvar = 3;
			this.canBeSecondary = true;
	
			this.maxvar = getMaxVarieties(template);
			
			
			if(role.equals("mounted"))
				this.maxvar = Math.min(getCavVarieties(pose), maxvar);

			if(Generic.getTagValue(pose.tags, "primaryraceonly") != null)
				this.canBeSecondary = false;

		}
	}
	
	
	public int getMaxVarieties(Unit u)
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
		
		if(u.pose.roles.contains("mounted"))
			maxvar = Math.min(getCavVarieties(u.pose), maxvar);

		return maxvar;

	}
	
	
	public TroopGenerator(NationGen g, Nation n)
	{
		nationGen = g;
		nation = n;
		unitGen = new UnitGen(g, n);
		random = new Random(n.random.nextInt());

		
		bonusrangedness = random.nextDouble() * 0.3;
		chandler = new ChanceIncHandler(nation);
		maxtemplates = 1 + random.nextInt(3); // 1-3
	}
	
	private double getDualWieldChance(Unit u)
	{
		
		double local_dwchance = 0.05;

		List<String> tags = new ArrayList<String>();
		tags.addAll(u.race.tags);
		tags.addAll(u.pose.tags);
		for(Theme t : nation.themes)
			tags.addAll(t.tags);
		for(Filter f : u.appliedFilters)
			tags.addAll(f.tags);
				
		
		List<String> values = Generic.getTagValues(tags, "dwchance");
		if(values.size() > 0)
		{
			List<Double> intvalues = new ArrayList<Double>();
			for(String s : values)
				intvalues.add(Double.parseDouble(s));
			
			
			double largest = intvalues.get(0);
			for(double d : intvalues)
				if(d > largest)
					largest = d;
			
			local_dwchance = largest;
		}
		
		values = Generic.getTagValues(tags, "dwchancebonus");
		if(values.size() > 0)
		{
			for(String s : values)
				local_dwchance +=  Double.parseDouble(s);	
		}
		
		return local_dwchance;
	
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
	public Unit generateUnit(String role, Race race, int maxvarieties)
	{
		Unit unit = null;
		
		boolean isPrimaryRace = (race == nation.races.get(0));
		
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
				
				
				// Calculate the same armor happening in same role in different poses
				int occurances = 0;
				for(Template t2 : templates)
					if(t.armor.id.equals(t2.armor.id) && Math.abs(t.template.getHP() - t2.template.getHP()) < 4 && t.role.equals(t2.role) && t.template.getSlot("mount") == t2.template.getSlot("mount"))
					{
						occurances += t2.weapons.size();
					}
				
				//// Skip over if:
				// - Too many occurances of armor
				// - Skipchance
				// - Wrong race
				// - Wrong role
				// - Too many units in template
				// - Correct race but #primaryraceonly
				if(shouldSkipTemplate(t, isPrimaryRace, maxvarieties, race, role))
				{
					continue;
				}

					
				// Copy unit
				unit = t.template.getCopy();
	
		
				boolean success = false;
				if(!role.equals("mounted"))
				{
					success = armInfantry(unit, t);
					
				}
				else
					success = armCavalry(unit, t);
			
				
				if(!success)
				{
					unit = null;
							
				}
				
				// Bonusweapon
					
				this.equipBonusWeapon(unit, role, race, t);

			}

			
			if(unit == null)
			{

				Template t = getNewTemplate(race, role);
				if(t != null)
					templates.add(t);
				else
					return null;

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
	
	private boolean armInfantry(Unit unit, Template t)
	{

	
		ItemSet possibleWeapons = t.template.pose.getItems("weapon").filterTag("elite", false).filterTag("sacred", false);
		if(possibleWeapons.possibleItems() - t.weapons.size() <= 0)
		{
			unit = null;
			return false;
		}
		else
		{
			ItemSet tempweps = new ItemSet();
			for(Template t2 : templates)
				if(t.armor.id.equals(t2.armor.id) && Math.abs(t.template.getHP() - t2.template.getHP()) < 3  && t.role.equals(t2.role))
				{
					tempweps.addAll(t.weapons);
				}
			
	
			
			
			
			Item weapon = this.getNewItem("weapon", t.role, unit, null, tempweps);
			unit.setSlot("weapon", weapon);
			t.weapons.add(weapon);
			

			if(t.role.equals("infantry") && nationGen.weapondb.GetInteger(unit.getSlot("weapon").id, "2h") == 0 && nationGen.weapondb.GetInteger(unit.getSlot("weapon").id, "lgt") < 3)
			{
				double local_dwchance = this.getDualWieldChance(unit);
				
				if(random.nextDouble() < local_dwchance)
					
				{
					this.equipOffhand(unit, t.role, unit.race, t);
				}
			}
		
		}
		
		return true;
	}
	
	/**
	 * Ugly method for checking whether to skip, but this makes the main code clearer.
	 * @param t
	 * @param isPrimaryRace
	 * @param maxvarieties
	 * @param race
	 * @param role
	 * @return
	 */
	private boolean shouldSkipTemplate(Template t, boolean isPrimaryRace, int maxvarieties, Race race, String role)
	{
		int occurances = 0;
		for(Template t2 : templates)
			if(t.armor.id.equals(t2.armor.id) && Math.abs(t.template.getHP() - t2.template.getHP()) < 4 && t.role.equals(t2.role) && t.template.getSlot("mount") == t2.template.getSlot("mount"))
			{
				occurances += t2.weapons.size();
			}
		
		double rand = random.nextDouble();
		if(occurances >= maxvarieties)
		{
			return true;
		}
		else if(rand <= skipchance)
		{
			return true;

		}
		else if(t.race != race)
		{
			return true;

		}
		else if(!t.pose.roles.contains(role))
		{
			return true;

		}
		else if(t.weapons.size() >= t.maxvar)
		{			
			return true;

		}
		else if(!isPrimaryRace && !t.canBeSecondary)
		{
			return true;

		}
		
		return false;
	}
	
	private boolean armCavalry(Unit unit, Template t)
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
				
			Item weapon = this.getNewItem("weapon", "mounted", unit, tempweps, null);
			

			
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
				if(random.nextDouble() > 0.75)
					lancelimit = lancelimit * 2;

				boolean getsLance = (ap >= lancelimit);
				
				if(getsLance && t.pose.getItems("lanceslot").size() > 0)
				{
					tempweps.clear();
					for(Item i : t.pose.getItems("lanceslot"))
					{
						if(!i.id.equals("4") && !i.tags.contains("lance"))
							tempweps.add(i);
					}
					
		
					Item lance = this.getNewItem("lanceslot", "mounted", unit, tempweps, null);
					unit.setSlot("lanceslot", lance);
				}
				
				
				}
				

			
				return true;
			
	}

	private Template getNewTemplate(Race race, String role)
	{
	
		Unit dummy = new Unit(nationGen, null, null);
		removeEliteSacred(dummy, role);
		
		Item armor = null;
		Unit u = null;
		while(u == null || armor == null)
		{
			
			ItemSet armors = new ItemSet();
			ItemSet oldarmors = new ItemSet();

			
			// Armors are the set of armors that already are in
			// Oldarmors are the armors that are in but may be used
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
			
			// 30% chance to get an old armor if possible
			if(oldarmors.size() > 0 && random.nextDouble() < 0.3)
			{
				armor = Entity.getRandom(random, chandler.handleChanceIncs(dummy, oldarmors));
				List<Item> possibleArmor = new ArrayList<Item>();
				
				
				for(Pose p : race.getPoses(role))
				{
					Item it = unitGen.getBestMatchForSlot(armor, p, "armor");
					if(it != null)
						possibleArmor.add(it);
				
				}
				
				armor = Entity.getRandom(random, possibleArmor);
			}


			if(armor == null)
			{
	
					
				List<Pose> ps = new ArrayList<Pose>();
				for(Pose p : race.getPoses(role))
				{
					List<Item> temp = p.getItems("armor");
					temp.removeAll(armors);
					
					int count = chandler.countPossibleFilters(temp, dummy);
					if(count > 0)
					{
						ps.add(p);
					}
				}
				
				Pose p = Entity.getRandom(random, chandler.handleChanceIncs(dummy, ps));
				
				if(p != null)
				{
					ItemSet newarmors = new ItemSet();
					newarmors.addAll(p.getItems("armor"));
					newarmors.removeAll(armors);
					
					armor = this.getNewItem("armor", role, new Unit(nationGen, race, p), newarmors, null);
				}

			}

			
			

			Pose p = null;
			if(armor != null)
			{
				p = chandler.getRandom(getPosesWithoutMaxUnits(getPosesWith(race, role, armor)));
			}
			else
			{
				p = chandler.getRandom(getPosesWithoutMaxUnits(race.getPoses(role)));
				
				armor = Entity.getRandom(random, chandler.handleChanceIncs(p.getItems("armor")));
			}
			
			if(p == null)
				return null;
			

	
			// Generate unit!
				

			u = unitGen.generateUnit(race, p);

			addInitialFilters(u, role);
			
			unitGen.armorUnit(u, used, exclusions, null, false);
			armor = u.getSlot("armor");
			
			
		}
		
		//if(u == null || armor == null) // EA20150529: The only case where this should occur is when #maxunits has been exceeded
		//	return null;
		


		Template t = new Template(u.getSlot("armor"), race, u, role, u.pose);

		//System.out.println("Adding template with " + t.armor);

		//templates.add(t);
		
		// Exclude similar shield/armor
		for(Pose p2 : this.getPossiblePoses(role, race))
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
		

		return t;
						
					
	}
	
	
	private List<Pose> getPosesWithoutMaxUnits(List<Pose> orig)
	{
		List<Pose> poses = new ArrayList<Pose>();
		for(Pose p : orig)
			if(!poseHasMaxUnits(p))
			{
				poses.add(p);
			}
	
		return poses;
		
	}
	private boolean poseHasMaxUnits(Pose p)
	{
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
			return true;
		}
		
		return false;
	}
	
	protected void addTemplateFilter(Unit u, String query, String defaultv)
	{
		
		
		List<Filter> possibleFilters = new ArrayList<Filter>();
		possibleFilters.addAll(this.unitTemplates);
		possibleFilters.removeAll(u.appliedFilters);
		
	
		
		// Add unit template to available templates
		if(unitTemplates.size() < maxtemplates && possibleFilters.size() == 0)
		{
			List<Filter> tFilters = ChanceIncHandler.retrieveFilters(query, defaultv, nationGen.miscdef, u.pose, u.race);
			possibleFilters.retainAll(tFilters);
			tFilters.removeAll(unitTemplates);			
			tFilters = ChanceIncHandler.getValidUnitFilters(tFilters, u);
			
			Filter t = chandler.getRandom(tFilters, u);
			
			
			
			if(t != null)
			{
				unitTemplates.add(t);
				possibleFilters.add(t);
			}
		
		}
		
		Filter f = chandler.getRandom(possibleFilters, u);
		if(f != null)
		{
			appliedtemplates++;
			u.appliedFilters.add(f);
		}
		
		

	
	}
	
	protected void addInitialFilters(Unit u, String role)
	{

		if(random.nextDouble() < 0.05 || appliedtemplates < maxtemplates)
		{
			addTemplateFilter(u, "trooptemplates", "default_templates");
			if(random.nextDouble() < 0.1)
			{
				addTemplateFilter(u, "trooptemplates", "default_templates");
			}
		}
		
		removeEliteSacred(u, role);

	}
	
	protected void removeEliteSacred(Unit u, String role)
	{
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
			double roll = random.nextDouble();
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
			Item wep = Entity.getRandom(random, chandler.handleChanceIncs(u, stuff));
			u.setSlot("offhand", wep);
		}
		
	}
	
	private void equipBonusWeapon(Unit u, String role, Race race, Template t)
	{
		if(role.equals("ranged"))
			return;
		
		
		ItemSet bonuses = used.filterSlot("bonusweapon").filterForPose(t.pose);
		if(bonuses.possibleItems() < 1 || random.nextDouble() < 0.5)
		{
			bonuses.add(this.getNewItem("bonusweapon", role, u, bonuses));
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
		
		


		double local_bwchance = 0.05;
	
		List<String> tags = new ArrayList<String>();
		tags.addAll(u.race.tags);
		tags.addAll(u.pose.tags);
		for(Theme th : nation.themes)
			tags.addAll(th.tags);
		for(Filter f : u.appliedFilters)
			tags.addAll(f.tags);
				
		
		List<String> values = Generic.getTagValues(tags, "bonusweaponchance");
		if(values.size() > 0)
		{
			List<Double> intvalues = new ArrayList<Double>();
			for(String s : values)
				intvalues.add(Double.parseDouble(s));
			
			
			double largest = intvalues.get(0);
			for(double d : intvalues)
				if(d > largest)
					largest = d;
			
			local_bwchance = largest;
		}
		
		values = Generic.getTagValues(tags, "bonusweaponchancebonus");
		if(values.size() > 0)
		{
			for(String s : values)
				local_bwchance +=  Double.parseDouble(s);	
		}
	
	
		
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
	private List<Pose> getPossiblePoses(String role, Race race)
	{
		// Search for poses with suitable armor
		List<Pose> possiblePoses = new ArrayList<Pose>();
		for(Pose p : race.poses)
		{
			if(!p.roles.contains(role))
				continue;
			
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
		for(Filter f : u.appliedFilters)
			if(f != null)
				tags.addAll(f.tags);
		
		for(String tag : tags)
		{
			List<String> args = Generic.parseArgs(tag);
			if(args.get(0).equals("generateitem"))
			{
				String slot = args.get(2);
				double chance = Double.parseDouble(args.get(1));
				if(random.nextDouble() < chance)
				{
					Item item = getNewItem(slot, role, u, null);
					u.setSlot(slot, item);
					if(used != null && !used.contains(item))
						used.add(item);
				}
			}
		}
	}
	
	
	



	
	public Item getNewItem(String slot, String role, Race race, Pose p, ItemSet used)
	{
		Unit u = new Unit(nationGen, race, p);
		return this.getNewItem(slot, role, u,  p.getItems(slot), used);
	}
	
	public Item getNewItem(String slot, String role, Unit u, ItemSet used)
	{
		return getNewItem(slot, role, u, u.pose.getItems(slot), used);
	}

	public Item getNewItem(String slot, String role, Unit u, ItemSet all, ItemSet used)
	{

		Pose p = null;
		Race race = null;
		
		if(u != null)
		{
			p = u.pose;
			race = u.race;
		}
		
		if(p != null && p.getItems(slot) == null)
			return null;
		
		ItemSet old;
		if(p == null)
			old = this.used.filterSlot(slot).filterForRole(role, race);
		else
			old = this.used.filterSlot(slot).filterForPose(p);
		
		old.retainAll(race.getItems(slot, role));
		
	
		if(all == null && p != null)
			all = p.getItems(slot);

		
		if(p != null)
			all = p.getItems(slot);

		
		old = old.filterImpossibleAdditions(used);
		all = all.filterImpossibleAdditions(used);
		
		if(p != null)
		{
			old = old.filterForPose(p);
			all = all.filterForPose(p);
		}
		

		if(slot.equals("weapon") && role.equals("infantry"))
		{
			if(!this.has1H(used) && random.nextDouble() < 0.5)
			{
				if(has1H(all))
					all = all.filterDom3DB("2h", "0", true, nationGen.weapondb);
				if(has1H(old))
					old = old.filterDom3DB("2h", "0", true, nationGen.weapondb);
			}
		}
		

		
		ItemSet select = old;
		if(chandler.countPossibleFilters(select, u) == 0)
		{
			select = all;
		}
		

		
		if(chandler.countPossibleFilters(select, u) == 0)
		{
			if(p == null)
				select = race.getItems(slot, role);
			else
				select = p.getItems(slot);
		}
		



		

		if(random.nextDouble() < 0.5 && chandler.countPossibleFilters(all, u) > 0)
			select = all;
		
		//System.out.println(all.possibleItems() + " / " + old.possibleItems() + " / " + select.possibleItems());

		
		if(u != null)
			return Entity.getRandom(random, chandler.handleChanceIncs(u, select));
		else
			return Entity.getRandom(random, chandler.handleChanceIncs(new Unit(nationGen, race, p, nation), select));

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
			
		return Entity.getRandom(random, chandler.handleChanceIncs(items));
	}
	
	
	

	
	



	
}
